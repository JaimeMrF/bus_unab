<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\Stop;
use App\Services\GpsMobileService;
use App\Support\DriverLocation;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class BusMapController extends Controller
{
    public function __construct(private readonly GpsMobileService $gpsService) {}

    /**
     * Devuelve el snapshot de todos los buses + paradas para el mapa del admin.
     *
     * GET /admin/map-data
     * Protegido: super admin ve toda la flota; el admin de empresa (/empresa) solo la suya.
     */
    public function data(Request $request): JsonResponse
    {
        // Doble validación de seguridad — el middleware web + sesión Filament
        $user = auth()->user();

        if (! $user || ! ($user->isSuperAdmin() || $user->isTenantAdmin())) {
            abort(403);
        }

        // Esta ruta no pasa por EnsureTenantScope: el filtro por empresa va explícito.
        $tenantId = $user->isSuperAdmin() ? null : $user->transportadora_id;
        $scoped = fn ($query) => $tenantId === null ? $query : $query->withoutGlobalScopes()->where('transportadora_id', $tenantId);

        $buses = $scoped(Bus::query())->where('is_active', true)->get();

        // Una sola llamada al listado del GPS: el detalle se pide solo para placas que el proveedor
        // reporta. Pedirlo bus por bus a ciegas sumaba un timeout por cada bus sin GPS real y el
        // mapa terminaba en "GPS no disponible".
        $reported = collect($this->gpsService->getAllBuses() ?? [])->pluck('Placa')->flip();

        $busData = $buses->map(function (Bus $bus) use ($reported) {
            $detail = isset($reported[$bus->plate]) && ! DriverLocation::get($bus->plate)
                ? $this->gpsService->getBusDetail($bus->external_vehicle_id)
                : null;
            $pending = BusRequest::where('bus_id', $bus->id)
                ->where('status', 'pending')
                ->count();

            $pct = $bus->capacity > 0
                ? round(min(($pending / $bus->capacity) * 100, 100), 1)
                : 0.0;

            if ($bus->isManuallyFull()) {
                $pct = 100.0;
            }

            $level = match (true) {
                $pct >= 90 => 'full',
                $pct >= 60 => 'high',
                $pct >= 30 => 'medium',
                default => 'low',
            };

            $gps = null;
            if ($detail) {
                $rawAddress = $detail['Info'] ?? '';
                $address = trim(preg_replace('/\s+CSQ:\d+.*$/i', '', $rawAddress));

                $gps = [
                    'latitude' => (float) ($detail['Lt'] ?? 0),
                    'longitude' => (float) ($detail['Lg'] ?? 0),
                    'speed_kmh' => (int) ($detail['Vel'] ?? 0),
                    'heading' => (int) ($detail['Std'] ?? 0),
                    'address' => $address ?: null,
                    'driver' => ($detail['Cond'] ?? null) ?: null,
                    'last_event' => $detail['NEv'] ?? null,
                    'last_updated_at' => $detail['FdS'] ?? null,
                    'online' => true,
                ];
            }

            // Sobreescribir con la ubicación del teléfono del conductor si está activa
            $driverLoc = DriverLocation::get($bus->plate);
            if ($driverLoc) {
                if ($gps) {
                    $gps['latitude'] = $driverLoc['latitude'];
                    $gps['longitude'] = $driverLoc['longitude'];
                    $gps['heading'] = $driverLoc['heading'];
                } else {
                    $gps = [
                        'latitude' => $driverLoc['latitude'],
                        'longitude' => $driverLoc['longitude'],
                        'speed_kmh' => $driverLoc['speed_kmh'] ?? 0,
                        'heading' => $driverLoc['heading'],
                        'address' => null,
                        'driver' => null,
                        'last_event' => null,
                        'last_updated_at' => null,
                        'online' => true,
                    ];
                }
            }

            return [
                'id' => $bus->id,
                'name' => $bus->name,
                'plate' => $bus->plate,
                'capacity' => $bus->capacity,
                'occupancy' => [
                    'count' => $pending,
                    'percentage' => $pct,
                    'level' => $level,
                ],
                'gps' => $gps ?? ['online' => false],
            ];
        });

        $stops = $scoped(Stop::query())->where('is_active', true)
            ->get(['id', 'name', 'address', 'latitude', 'longitude', 'radius_meters']);

        return response()->json([
            'buses' => $busData,
            'stops' => $stops,
            'fetched_at' => now()->toIso8601String(),
        ]);
    }
}
