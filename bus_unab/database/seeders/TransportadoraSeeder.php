<?php

namespace Database\Seeders;

use App\Models\Transportadora;
use Illuminate\Database\Seeder;

/**
 * M3 · S3.1.3 — Tenants de demostración.
 *
 * Crea las dos transportadoras que usa el seed completo:
 *  - "BUCARATRANSIT Demo" (slug bt-demo): tenant nuevo, sin datos
 *    heredados; servirá para demostrar el aislamiento entre tenants.
 *  - "Metropolitana UNAB (histórica)" (slug unab-historica): tenant al que
 *    se le backfilia TODA la data semilla preexistente (buses, stops, POI,
 *    usuarios admin/driver/student) — ver DatabaseSeeder.
 *
 * Idempotente: usa updateOrCreate por slug para que un segundo
 * `db:seed` no duplique ni rompa el UNIQUE.
 */
class TransportadoraSeeder extends Seeder
{
    public const SLUG_DEMO = 'bt-demo';
    public const SLUG_HISTORICA = 'unab-historica';

    public function run(): void
    {
        Transportadora::updateOrCreate(
            ['slug' => self::SLUG_DEMO],
            [
                'nombre'         => 'BUCARATRANSIT Demo',
                'contacto_email' => 'demo@bucaratransit.co',
                'plan'           => 'basico',
                'activo'         => true,
            ],
        );

        Transportadora::updateOrCreate(
            ['slug' => self::SLUG_HISTORICA],
            [
                'nombre'         => 'Metropolitana UNAB (histórica)',
                'contacto_email' => 'operaciones@unab-historica.co',
                'plan'           => 'basico',
                'activo'         => true,
            ],
        );
    }
}
