<?php

namespace Database\Seeders;

use App\Models\Bus;
use Illuminate\Database\Seeder;

class BusSeeder extends Seeder
{
    public function run(): void
    {
        $buses = [
            ['name' => 'Ruta 1', 'plate' => 'RUTA1', 'external_vehicle_id' => 97141],
            ['name' => 'Ruta 2', 'plate' => 'RUTA2', 'external_vehicle_id' => 190024],
            ['name' => 'Ruta 3', 'plate' => 'RUTA3', 'external_vehicle_id' => 190049],
        ];

        foreach ($buses as $bus) {
            Bus::updateOrCreate(
                ['external_vehicle_id' => $bus['external_vehicle_id']],
                ['name' => $bus['name'], 'plate' => $bus['plate'], 'capacity' => 40, 'is_active' => true]
            );
        }
    }
}
