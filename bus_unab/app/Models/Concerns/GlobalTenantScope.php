<?php

namespace App\Models\Concerns;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Scope;

/**
 * Aplica `where transportadora_id = <tenant actual>` SOLO cuando hay
 * contexto de tenant activo (ver TenantContext). Sin contexto, es un no-op.
 *
 * M3 · S3.1.2 — CUELLOS_BOTELLA §1: defensa en profundidad; las policies y
 * el middleware EnsureTenantScope (T3.3) irán ENCIMA de este scope.
 */
class GlobalTenantScope implements Scope
{
    public function apply(Builder $builder, Model $model): void
    {
        $tenantId = TenantContext::id();

        // Desactivado por defecto: no romper filas con NULL (datos compartidos
        // de ciudad) ni queries fuera de petición (console, seeders, tests).
        if ($tenantId === null) {
            return;
        }

        $builder->where($model->qualifyColumn('transportadora_id'), $tenantId);
    }
}
