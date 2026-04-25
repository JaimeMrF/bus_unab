<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class BusSeeder extends Seeder
{
    public function run(): void
    {
        DB::table('buses')->insert([
            [
                'name'                => 'Ruta 1',
                'plate'               => 'RUTA1',
                'capacity'            => 40,
                'external_vehicle_id' => 97141,
                'is_active'           => true,
                'created_at'          => now(),
                'updated_at'          => now(),
            ],
            [
                'name'                => 'Ruta 2',
                'plate'               => 'RUTA2',
                'capacity'            => 40,
                'external_vehicle_id' => 190024,
                'is_active'           => true,
                'created_at'          => now(),
                'updated_at'          => now(),
            ],
            [
                'name'                => 'Ruta 3',
                'plate'               => 'RUTA3',
                'capacity'            => 40,
                'external_vehicle_id' => 190049,
                'is_active'           => true,
                'created_at'          => now(),
                'updated_at'          => now(),
            ],
        ]);
    }
}
