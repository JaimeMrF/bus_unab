<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class StopSeeder extends Seeder
{
    /**
     * Paradas de ejemplo en el área de UNAB Bucaramanga.
     * Coordenadas aproximadas — ajustar con los puntos reales de cada ruta.
     */
    public function run(): void
    {
        $stops = [
            ['name' => 'UNAB Campus Principal',         'address' => 'Calle 45 #27-27, Bucaramanga',       'latitude' => 7.1218,    'longitude' => -73.1158],
            ['name' => 'Parque Santander',               'address' => 'Calle 35 con Cra 19, Bucaramanga',   'latitude' => 7.1133,    'longitude' => -73.1099],
            ['name' => 'Centro Comercial Cabecera',      'address' => 'Cra 35 con Calle 52, Bucaramanga',   'latitude' => 7.1065,    'longitude' => -73.1059],
            ['name' => 'Floridablanca Centro',           'address' => 'Cra 10 con Calle 7, Floridablanca',  'latitude' => 7.0643,    'longitude' => -73.0881],
            ['name' => 'Terminal de Transportes',        'address' => 'Cra 15 con Calle 30, Bucaramanga',   'latitude' => 7.0919,    'longitude' => -73.1208],
            ['name' => 'Metrolínea Estación Norte',      'address' => 'Av. González Valencia, Bucaramanga', 'latitude' => 7.1290,    'longitude' => -73.1241],
            ['name' => 'Clínica FOSCAL',                 'address' => 'Autopista Bucaramanga-Floridablanca', 'latitude' => 7.0793,   'longitude' => -73.1041],
            ['name' => 'Universidad Industrial de Santander', 'address' => 'Cra 27 con Calle 9, Bucaramanga', 'latitude' => 7.1405, 'longitude' => -73.1197],
        ];

        foreach ($stops as $stop) {
            DB::table('stops')->insert([
                'name'          => $stop['name'],
                'address'       => $stop['address'],
                'latitude'      => $stop['latitude'],
                'longitude'     => $stop['longitude'],
                'radius_meters' => 100,
                'is_active'     => true,
                'created_at'    => now(),
                'updated_at'    => now(),
            ]);
        }
    }
}
