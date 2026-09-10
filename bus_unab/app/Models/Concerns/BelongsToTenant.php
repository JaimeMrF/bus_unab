<?php

namespace App\Models\Concerns;

use App\Models\Transportadora;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * BelongsToTenant — aislamiento por transportadora (M3 · S3.1.2).
 *
 * DISEÑO: DISEÑO_DB.md §3 y CUELLOS_BOTELLA.md §1.
 *
 *  - GlobalScope (GlobalTenantScope) filtra por `transportadora_id` SOLO
 *    cuando hay un tenant en contexto. POR DEFECTO NO HAY CONTEXTO, así que
 *    nada cambia para paneles, seeders, tests ni la API ya publicada.
 *  - `BelongsToTenant::withTenant($id, fn)` ejecuta un bloque con contexto.
 *  - `scopeWithoutTenant()` = llave de escape para Super Admin/reportes.
 *  - En `creating`, si hay contexto, auto-pobla transportadora_id.
 */
trait BelongsToTenant
{
    public static function bootBelongsToTenant(): void
    {
        static::addGlobalScope(new GlobalTenantScope());

        static::creating(function ($model) {
            if (TenantContext::id() !== null && empty($model->transportadora_id)) {
                $model->transportadora_id = TenantContext::id();
            }
        });
    }

    /** Empresa dueña de la fila. NULL = dato compartido de ciudad/plataforma. */
    public function transportadora(): BelongsTo
    {
        return $this->belongsTo(Transportadora::class);
    }

    /**
     * Corre $callback filtrando y auto-asignando al tenant $transportadoraId
     * (null = sin contexto). Anidable y seguro ante excepciones.
     */
    public static function withTenant(?int $transportadoraId, callable $callback): mixed
    {
        return TenantContext::run($transportadoraId, $callback);
    }

    /** Escape: consulta sin el global scope (Super Admin, seeders, reports). */
    public function scopeWithoutTenant(Builder $query): Builder
    {
        return $query->withoutGlobalScope(GlobalTenantScope::class);
    }
}
