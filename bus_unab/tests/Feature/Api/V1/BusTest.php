<?php

namespace Tests\Feature\Api\V1;

use App\Models\Bus;
use App\Models\User;
use App\Services\GpsMobileService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class BusTest extends TestCase
{
    use RefreshDatabase;

    private User $user;

    protected function setUp(): void
    {
        parent::setUp();
        $this->user = User::factory()->create();
    }

    private function createBus(string $plate = 'RUTA1', int $externalId = 97141): Bus
    {
        return Bus::create([
            'name'                => "Ruta {$plate}",
            'plate'               => $plate,
            'external_vehicle_id' => $externalId,
            'capacity'            => 40,
            'is_active'           => true,
        ]);
    }

    private function fakeGpsData(string $plate = 'RUTA1'): array
    {
        return [
            'Placa'    => $plate,
            'Latitud'  => '7.1218',
            'Longitud' => '-73.1158',
            'Sentido'  => '90',
        ];
    }

    public function test_buses_index_returns_active_buses(): void
    {
        $this->createBus('RUTA1');
        $this->createBus('RUTA2', 190024);

        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getAllBuses')->andReturn([
            $this->fakeGpsData('RUTA1'),
            $this->fakeGpsData('RUTA2'),
        ]);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses')
            ->assertStatus(200)
            ->assertJson(['success' => true])
            ->assertJsonCount(2, 'data');
    }

    public function test_buses_index_filters_out_unknown_plates(): void
    {
        $this->createBus('RUTA1');

        $mock = $this->mock(GpsMobileService::class);
        // GPS devuelve un vehículo que no está en nuestra BD
        $mock->shouldReceive('getAllBuses')->andReturn([
            $this->fakeGpsData('RUTA1'),
            $this->fakeGpsData('RUTADESCONOCIDA'),
        ]);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses')
            ->assertStatus(200)
            ->assertJsonCount(1, 'data');
    }

    public function test_buses_index_returns_503_when_gps_fails(): void
    {
        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getAllBuses')->andReturn(null);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses')
            ->assertStatus(503);
    }

    public function test_buses_index_requires_authentication(): void
    {
        $this->getJson('/api/v1/buses')->assertStatus(401);
    }

    public function test_buses_index_validates_lat_lng(): void
    {
        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getAllBuses')->andReturn([]);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses?lat=invalid&lng=invalid')
            ->assertStatus(422);
    }

    public function test_bus_show_returns_detail(): void
    {
        $this->createBus('RUTA1');

        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getBusDetail')->andReturn([
            'Lt'   => '7.1218',
            'Lg'   => '-73.1158',
            'Vel'  => '30',
            'Std'  => '180',
            'Info' => 'Bucaramanga, El Jardín. Calle 48 con 27 CSQ:5 DSleep:0',
            'Cond' => 'Juan Pérez',
            'NEv'  => 'Encendido',
            'FdS'  => '2026-05-02 10:00:00',
        ]);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1')
            ->assertStatus(200)
            ->assertJson(['success' => true])
            ->assertJsonStructure(['data' => [
                'id', 'name', 'plate', 'latitude', 'longitude',
                'speed_kmh', 'heading', 'address', 'driver', 'last_updated_at',
            ]]);
    }

    public function test_bus_show_cleans_gps_noise_from_address(): void
    {
        $this->createBus('RUTA1');

        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getBusDetail')->andReturn([
            'Lt'   => '7.1218',
            'Lg'   => '-73.1158',
            'Vel'  => '0',
            'Std'  => '0',
            'Info' => 'Bucaramanga, El Jardín. Calle 48 CSQ:5 DSleep:0 IN1:1 IN2:0',
            'Cond' => '',
            'NEv'  => null,
            'FdS'  => null,
        ]);

        $response = $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1');

        $response->assertStatus(200);
        $address = $response->json('data.address');
        $this->assertStringNotContainsString('CSQ', $address);
        $this->assertStringNotContainsString('DSleep', $address);
    }

    public function test_bus_show_returns_null_driver_when_empty(): void
    {
        $this->createBus('RUTA1');

        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getBusDetail')->andReturn([
            'Lt' => '7.1', 'Lg' => '-73.1', 'Vel' => '0', 'Std' => '0',
            'Info' => 'Dir', 'Cond' => '', 'NEv' => null, 'FdS' => null,
        ]);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1')
            ->assertStatus(200)
            ->assertJson(['data' => ['driver' => null]]);
    }

    public function test_bus_show_returns_404_for_unknown_plate(): void
    {
        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTAXX')
            ->assertStatus(404)
            ->assertJson(['success' => false]);
    }

    public function test_bus_show_returns_404_for_inactive_bus(): void
    {
        Bus::create([
            'name' => 'Ruta Inactiva', 'plate' => 'RUTAIN',
            'external_vehicle_id' => 999, 'capacity' => 40, 'is_active' => false,
        ]);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTAIN')
            ->assertStatus(404);
    }

    public function test_bus_show_returns_503_when_gps_fails(): void
    {
        $this->createBus('RUTA1');

        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getBusDetail')->andReturn(null);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1')
            ->assertStatus(503);
    }

    public function test_bus_show_sanitizes_special_chars_in_plate(): void
    {
        // Inyección de caracteres especiales en la plate
        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1<script>')
            ->assertStatus(404);
    }
}
