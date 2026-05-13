<?php

namespace Database\Seeders;

use App\Models\Bus;
use App\Models\Stop;
use App\Models\RouteStop;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class Route2Seeder extends Seeder
{
    public function run(): void
    {
        // 1. Definir las paradas con coordenadas reales
        $stopsData = [
            ['name' => 'Av. El jardín bloque L',        'lat' => 7.134271, 'lng' => -73.119855, 'address' => 'Av. El Jardín, UNAB Bloque L'],
            ['name' => 'Frente a la Clínica Bucaramanga', 'lat' => 7.131065, 'lng' => -73.117420, 'address' => 'Cra. 33 #52-25, Bucaramanga'],
            ['name' => 'Torres de Monterrey',           'lat' => 7.127889, 'lng' => -73.115987, 'address' => 'Cra. 33 #56, Bucaramanga'],
            ['name' => 'Diamante II',                   'lat' => 7.103521, 'lng' => -73.116542, 'address' => 'Diamante II, Bucaramanga'],
            ['name' => 'Puente Provenza',               'lat' => 7.095832, 'lng' => -73.112845, 'address' => 'Autopista a Floridablanca, Provenza'],
            ['name' => 'Facultad de Ciencias de la Salud', 'lat' => 7.121456, 'lng' => -73.114789, 'address' => 'Calle 45, UNAB Salud'],
            ['name' => 'CC. Parque Caracolí',           'lat' => 7.067890, 'lng' => -73.102345, 'address' => 'Carrera 27, Floridablanca'],
            ['name' => 'CC. Cacique',                   'lat' => 7.106543, 'lng' => -73.104321, 'address' => 'Transversal 93, Bucaramanga'],
            ['name' => 'Garabatos',                     'lat' => 7.112345, 'lng' => -73.108765, 'address' => 'Carrera 33, Cabecera'],
            ['name' => 'Arturo Calle',                  'lat' => 7.114567, 'lng' => -73.110987, 'address' => 'Carrera 33 #48, Bucaramanga'],
            ['name' => 'Motocenter Suzuki',             'lat' => 7.116789, 'lng' => -73.112123, 'address' => 'Carrera 27, Bucaramanga'],
            ['name' => 'CC. Cañaveral',                 'lat' => 7.065432, 'lng' => -73.100123, 'address' => 'Calle 30, Floridablanca'],
            ['name' => 'Puente Peatonal UNAB',          'lat' => 7.119321, 'lng' => -73.115432, 'address' => 'Calle 45, Bucaramanga'],
            ['name' => 'Más por menos',                 'lat' => 7.117654, 'lng' => -73.113210, 'address' => 'Carrera 33, Bucaramanga'],
        ];

        DB::transaction(function () use ($stopsData) {
            // 2. Buscar el bus de la Ruta 2
            $bus = Bus::where('plate', 'RUTA2')->first();
            
            if (!$bus) {
                // Si no existe, lo creamos para que el seeder no falle
                $bus = Bus::create([
                    'name' => 'Ruta 2',
                    'plate' => 'RUTA2',
                    'capacity' => 40,
                    'external_vehicle_id' => 190024,
                    'is_active' => true
                ]);
            }

            // Limpiar paradas previas de esta ruta si existen
            RouteStop::where('bus_id', $bus->id)->delete();

            foreach ($stopsData as $index => $data) {
                // 3. Crear o actualizar la parada física
                $stop = Stop::updateOrCreate(
                    ['name' => $data['name']],
                    [
                        'latitude' => $data['lat'],
                        'longitude' => $data['lng'],
                        'address' => $data['address'],
                        'radius_meters' => 100,
                        'is_active' => true
                    ]
                );

                // 4. Asignar la parada al bus en el orden correspondiente
                RouteStop::create([
                    'bus_id' => $bus->id,
                    'stop_id' => $stop->id,
                    'order' => ($index + 1) * 10,
                    'estimated_minutes' => 5 // Valor por defecto
                ]);
            }
        });
    }
}
