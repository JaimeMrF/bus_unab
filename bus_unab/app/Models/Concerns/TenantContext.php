<?php

namespace App\Models\Concerns;

/**
 * Portador estático del tenant activo (M3 · S3.1.2).
 *
 * POR DEFECTO está desactivado (null): paneles, seeders, tests y API publicada
 * se comportan EXACTAMENTE como antes del pivote. El aislamiento real se
 * activa por bloque de ejecución con run()/set() — lo consumirá el middleware
 * EnsureTenantScope en T3.3 y el panel /empresa.
 */
class TenantContext
{
    protected static ?int $currentTenantId = null;

    public static function set(?int $id): void
    {
        static::$currentTenantId = $id;
    }

    public static function id(): ?int
    {
        return static::$currentTenantId;
    }

    /** Ejecuta $fn con el tenant activo y RESTAURA el contexto previo (anidable). */
    public static function run(?int $id, callable $fn): mixed
    {
        $previous = static::$currentTenantId;
        static::$currentTenantId = $id;

        try {
            return $fn();
        } finally {
            static::$currentTenantId = $previous;
        }
    }
}
