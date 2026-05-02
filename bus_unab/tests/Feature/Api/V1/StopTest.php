<?php

namespace Tests\Feature\Api\V1;

use App\Models\Bus;
use App\Models\RouteStop;
use App\Models\Stop;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class StopTest extends TestCase
{
    use RefreshDatabase;

    private User $user;

    protected function setUp(): void
    {
        parent::setUp();
        $this->user = User::factory()->create();
    }

    private function createStop(array $overrides = []): Stop
    {
        return Stop::create(array_merge([
            'name'          => 'Parada Test',
            'address'       => 'Calle 48 #39-234',
            'latitude'      => 7.1218,
            'longitude'     => -73.1158,
            'radius_meters' => 100,
            'is_active'     => true,
        ], $overrides));
    }

    public function test_stops_index_returns_active_stops(): void
    {
        $this->createStop(['name' => 'UNAB Campus']);
        $this->createStop(['name' => 'Parque Santander', 'latitude' => 7.1100, 'longitude' => -73.1200]);
        $this->createStop(['name' => 'Inactiva', 'is_active' => false]);

        $response = $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/stops');

        $response->assertStatus(200)
            ->assertJson(['success' => true])
            ->assertJsonCount(2, 'data');
    }

    public function test_stops_index_returns_correct_fields(): void
    {
        $this->createStop(['name' => 'UNAB Campus']);

        $response = $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/stops');

        $response->assertStatus(200)
            ->assertJsonStructure(['data' => [['id', 'name', 'address', 'latitude', 'longitude']]]);
    }

    public function test_stops_index_requires_authentication(): void
    {
        $this->getJson('/api/v1/stops')->assertStatus(401);
    }

    public function test_stops_by_bus_returns_ordered_stops(): void
    {
        $bus = Bus::create([
            'name'                => 'Ruta 1',
            'plate'               => 'RUTA1',
            'external_vehicle_id' => 97141,
            'capacity'            => 40,
            'is_active'           => true,
        ]);

        $stopA = $this->createStop(['name' => 'Parada A', 'latitude' => 7.1]);
        $stopB = $this->createStop(['name' => 'Parada B', 'latitude' => 7.2]);
        $stopC = $this->createStop(['name' => 'Parada C', 'latitude' => 7.3]);

        RouteStop::create(['bus_id' => $bus->id, 'stop_id' => $stopA->id, 'order' => 1, 'estimated_minutes' => 0]);
        RouteStop::create(['bus_id' => $bus->id, 'stop_id' => $stopB->id, 'order' => 2, 'estimated_minutes' => 5]);
        RouteStop::create(['bus_id' => $bus->id, 'stop_id' => $stopC->id, 'order' => 3, 'estimated_minutes' => 10]);

        $response = $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1/stops');

        $response->assertStatus(200)
            ->assertJsonCount(3, 'data');

        $stops = $response->json('data');
        $this->assertEquals(1, $stops[0]['order']);
        $this->assertEquals(2, $stops[1]['order']);
        $this->assertEquals(3, $stops[2]['order']);
    }

    public function test_stops_by_bus_includes_estimated_minutes(): void
    {
        $bus = Bus::create([
            'name' => 'Ruta 1', 'plate' => 'RUTA1',
            'external_vehicle_id' => 97141, 'capacity' => 40, 'is_active' => true,
        ]);

        $stop = $this->createStop();
        RouteStop::create([
            'bus_id' => $bus->id, 'stop_id' => $stop->id,
            'order' => 1, 'estimated_minutes' => 7,
        ]);

        $response = $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1/stops');

        $response->assertStatus(200);
        $this->assertEquals(7, $response->json('data.0.estimated_minutes'));
    }

    public function test_stops_by_bus_returns_404_for_unknown_plate(): void
    {
        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTAXX/stops')
            ->assertStatus(404);
    }

    public function test_stops_by_bus_returns_404_for_inactive_bus(): void
    {
        Bus::create([
            'name' => 'Ruta Inactiva', 'plate' => 'RUTAIN',
            'external_vehicle_id' => 999, 'capacity' => 40, 'is_active' => false,
        ]);

        $this->actingAs($this->user, 'sanctum')
            ->getJson('/api/v1/buses/RUTAIN/stops')
            ->assertStatus(404);
    }
}
