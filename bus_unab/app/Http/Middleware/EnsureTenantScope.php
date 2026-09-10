<?php

namespace App\Http\Middleware;

use App\Models\Concerns\TenantContext;
use Closure;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * M3 · S3.3.3 — Aislamiento duro de tenant en la API Sanctum.
 *
 * Reglas (DISEÑO_DB.md §3, CUELLOS_BOTELLA.md §1):
 *  - Usuario sin tenant (Super Admin, pasajero de ciudad, seeders, consola)
 *    → pasa sin restricciones (el GlobalScope propio tampoco aplica sin contexto).
 *  - Usuario CON tenant: se ACTIVA el contexto (TenantContext::run) durante
 *    toda la petición, de modo que el GlobalScope de BelongsToTenant filtre
 *    índices y consultas (GET /api/v1/buses, GET /api/v1/stops, ...). El
 *    run() es anidable y su finally restaura el contexto previo incluso si
 *    el controlador lanza excepción (404/500) → no hay fuga post-request.
 *  - Usuario CON tenant: cualquier modelo ligado a la ruta (Bus, Stop,
 *    PointOfInterest, User) cuyo transportadora_id sea DISTINTO → 404 silencioso
 *    (no 403: no revelamos que el recurso existe).
 *  - Modelo con transportadora_id NULL = dato compartido de ciudad → pasa.
 *
 * Nota: la lista de modelos es whitelisted a propósito; nada más se inspecciona.
 */
class EnsureTenantScope
{
    /** Modelos de dominio con columna transportadora_id. */
    private const TENANT_MODELS = [
        \App\Models\Bus::class,
        \App\Models\Stop::class,
        \App\Models\User::class,
        \App\Models\PointOfInterest::class,
    ];

    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (! $user || $user->transportadora_id === null) {
            return $next($request);
        }

        // Parámetro de ruta ligado a modelo de OTRO tenant → 404 silencioso.
        foreach ($request->route()?->parameters() ?? [] as $parameter) {
            if (! $parameter instanceof Model || ! in_array($parameter::class, self::TENANT_MODELS, true)) {
                continue;
            }

            $owner = $parameter->getAttribute('transportadora_id');

            if ($owner !== null && (int) $owner !== (int) $user->transportadora_id) {
                abort(404, 'Recurso no encontrado.');
            }
        }

        // Miembro de transportadora: AISLAMIENTO DURO — activa el contexto para
        // toda la petición (índices y consultas del controlador pasan por el
        // GlobalScope). run() restaura el contexto previo en su finally, así
        // que ni un 404/500 del controlador deja contexto "fantasma".
        return TenantContext::run(
            (int) $user->transportadora_id,
            fn (): Response => $next($request)
        );
    }
}
