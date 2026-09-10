<?php

namespace Database\Seeders;

use App\Models\Transportadora;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // S3.1.3 — el tenant raíz se crea PRIMERO: todo lo demás le pertenece.
        $this->call([
            TransportadoraSeeder::class,
            BusSeeder::class,
            StopSeeder::class,
            AdminSeeder::class,
            DemoUserSeeder::class,
            Route2Seeder::class,
        ]);

        // Backfill del tenant histórico: la data semilla preexistente (igual
        // que la producción actual) se asigna a "Metropolitana UNAB".
        // El contexto global de tenant está DESACTIVADO durante el seeding,
        // así que un UPDATE directo por query builder es lo más simple y
        // no choca con el GlobalScope (ver Concerns/BelongsToTenant.php).
        $historica = Transportadora::where('slug', TransportadoraSeeder::SLUG_HISTORICA)->first();

        if ($historica === null) {
            return; // el seeder de tenants es idempotente; nada que backfilliar
        }

        foreach (['buses', 'stops', 'points_of_interest'] as $table) {
            DB::table($table)
                ->whereNull('transportadora_id')
                ->update(['transportadora_id' => $historica->id]);
        }

        // Usuarios OPERATIVOS (admin/driver/student de la demo UNAB) al tenant
        // histórico, para que el panel /empresa vea el equipo completo.
        // Los pasajeros sin rol operativo quedan NULL = pasajeros de ciudad
        // (DISEÑO_DB.md §3).
        DB::table('users')
            ->whereNull('transportadora_id')
            ->whereIn('role', ['admin', 'driver', 'student'])
            ->update(['transportadora_id' => $historica->id]);
    }
}
