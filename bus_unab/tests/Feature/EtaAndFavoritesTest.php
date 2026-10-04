<?php

namespace Tests\Feature;

use App\Models\Bus;
use App\Models\BusRouteWaypoint;
use App\Models\FavoriteStop;
use App\Models\RouteStop;
use App\Models\Stop;
use App\Models\Transportadora;
use App\Models\User;
use App\Support\DriverLocation;
use App\Support\RoutePath;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class EtaAndFavoritesTest extends TestCase
{
    use RefreshDatabase;

    /** Ruta recta hacia el norte: 0.01° de latitud ≈ 1105 m. */
    private function route(?int $tenantId = null): array
    {
        $bus = Bus::create(['name' => 'B', 'plate' => 'ETA1', 'external_vehicle_id' => 8001, 'capacity' => 40, 'is_active' => true, 'transportadora_id' => $tenantId]);
        $stops = [];
        foreach ([0.00, 0.01, 0.02] as $i => $d) {
            $stop = Stop::create(['name' => "P$i", 'latitude' => 7.10 + $d, 'longitude' => -73.1, 'radius_meters' => 50, 'is_active' => true, 'transportadora_id' => $tenantId]);
            RouteStop::create(['bus_id' => $bus->id, 'stop_id' => $stop->id, 'order' => $i + 1]);
            BusRouteWaypoint::create(['bus_id' => $bus->id, 'order' => $i + 1, 'latitude' => 7.10 + $d, 'longitude' => -73.1]);
            $stops[] = $stop;
        }

        return [$bus, $stops];
    }

    private function user(?int $tenantId = null): User
    {
        return User::factory()->create(['role' => 'pasajero', 'transportadora_id' => $tenantId]);
    }

    public function test_route_path_measures_along_the_route_and_wraps_the_circuit(): void
    {
        $path = new RoutePath([[7.10, -73.1], [7.11, -73.1], [7.12, -73.1]]);

        $ahead = $path->distanceAlong(7.10, -73.1, 7.12, -73.1);
        $this->assertEqualsWithDelta(2210, $ahead, 15);

        $behind = $path->distanceAlong(7.12, -73.1, 7.11, -73.1); // detrás: rodea el cierre
        $this->assertEqualsWithDelta(2210 - 1105, $behind, 15);
        $this->assertFalse((new RoutePath([[7.1, -73.1]]))->usable());
    }

    public function test_eta_uses_route_distance_and_recent_speed(): void
    {
        [$bus, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 36); // 36 km/h = 10 m/s

        $eta = $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[2]->id}")
            ->assertOk()->json('data');

        $this->assertEqualsWithDelta(2210, $eta['distance_m'], 15);
        $this->assertEquals(10.0, $eta['speed_mps']);
        $this->assertEqualsWithDelta(221, $eta['eta_seconds'], 3);
        $this->assertSame('high', $eta['confidence']);
        $this->assertEqualsCanonicalizing(['eta_seconds', 'distance_m', 'speed_mps', 'confidence'], array_keys($eta));
    }

    public function test_stopped_bus_uses_fallback_speed_with_lower_confidence(): void
    {
        [$bus, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 0);

        $eta = $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[1]->id}")->json('data');

        $this->assertEquals(8.0, $eta['speed_mps']);
        $this->assertSame('medium', $eta['confidence']);
        $this->assertEqualsWithDelta(1105 / 8, $eta['eta_seconds'], 3);
    }

    public function test_eta_falls_back_to_external_gps_and_returns_503_without_position(): void
    {
        config(['gpsmobile.base_url' => 'http://gps.test']);
        [$bus, $stops] = $this->route();
        $url = "/api/v1/buses/ETA1/eta?stop_id={$stops[1]->id}";

        Http::fake(['*' => Http::sequence()
            ->push([], 500)
            ->push(['sucess' => true, 'response' => ['veh' => ['Lt' => 7.10, 'Lg' => -73.1, 'Vel' => 18, 'Std' => 0, 'Info' => '', 'Cond' => null, 'NEv' => null, 'FdS' => null]]])]);
        $this->actingAs($this->user(), 'sanctum')->getJson($url)->assertStatus(503);

        Cache::flush();
        $this->actingAs($this->user(), 'sanctum')->getJson($url)->assertOk()->assertJsonPath('data.speed_mps', 5);
    }

    public function test_eta_validates_input_and_unknown_resources(): void
    {
        [$bus, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 30);
        $u = $this->user();

        $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/eta')->assertStatus(422);
        $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/eta?stop_id=99999')->assertNotFound();
        $this->actingAs($u, 'sanctum')->getJson("/api/v1/buses/NOPE/eta?stop_id={$stops[0]->id}")->assertNotFound();
    }

    public function test_eta_is_cached_for_a_few_seconds(): void
    {
        [$bus, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 36);
        $u = $this->user();
        $url = "/api/v1/buses/ETA1/eta?stop_id={$stops[2]->id}";

        $first = $this->actingAs($u, 'sanctum')->getJson($url)->json('data');
        DriverLocation::put('ETA1', 7.19, -73.1, 0, 36); // se movió, pero la caché aún vale
        $this->assertSame($first, $this->actingAs($u, 'sanctum')->getJson($url)->json('data'));
    }

    public function test_stops_by_bus_includes_eta_only_on_request_without_n_plus_one(): void
    {
        [$bus, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 36);
        $u = $this->user();

        $plain = $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/stops')->json('data');
        $this->assertArrayNotHasKey('eta_seconds', $plain[0]);

        DB::enableQueryLog();
        $with = $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/stops?eta=1')->assertOk()->json('data');
        $queries = count(DB::getQueryLog());

        $this->assertEqualsWithDelta(110, $with[1]['eta_seconds'], 3);
        $this->assertEqualsWithDelta(221, $with[2]['eta_seconds'], 3);
        $this->assertLessThan(15, $queries);
    }

    public function test_stops_and_catalog_support_etag_304(): void
    {
        $this->route();
        $u = $this->user();

        foreach (['/api/v1/stops', '/api/v1/buses/catalog'] as $url) {
            $first = $this->actingAs($u, 'sanctum')->getJson($url)->assertOk();
            $etag = $first->headers->get('ETag');
            $this->assertNotEmpty($etag);
            $this->assertStringContainsString('private', $first->headers->get('Cache-Control'));

            $this->actingAs($u, 'sanctum')->getJson($url, ['If-None-Match' => $etag])->assertStatus(304);
        }
    }

    public function test_favorites_crud_is_idempotent_and_per_user(): void
    {
        [, $stops] = $this->route();
        $a = $this->user();
        $b = $this->user();

        $this->actingAs($a, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $stops[0]->id])->assertStatus(201);
        $this->actingAs($a, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $stops[0]->id])->assertOk();
        $this->actingAs($a, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $stops[1]->id])->assertStatus(201);

        $list = $this->actingAs($a, 'sanctum')->getJson('/api/v1/favorites/stops')->assertOk()->json('data');
        $this->assertSame([$stops[1]->id, $stops[0]->id], array_column($list, 'id'));
        $this->assertEqualsCanonicalizing(['id', 'name', 'latitude', 'longitude'], array_keys($list[0]));
        $this->assertSame([], $this->actingAs($b, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'));

        $this->actingAs($b, 'sanctum')->deleteJson("/api/v1/favorites/stops/{$stops[0]->id}")->assertNoContent();
        $this->assertCount(2, $this->actingAs($a, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'));

        $this->actingAs($a, 'sanctum')->deleteJson("/api/v1/favorites/stops/{$stops[0]->id}")->assertNoContent();
        $this->actingAs($a, 'sanctum')->deleteJson("/api/v1/favorites/stops/{$stops[0]->id}")->assertNoContent();
        $this->assertCount(1, $this->actingAs($a, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'));
    }

    public function test_favorites_validate_and_reject_unknown_or_inactive_stops(): void
    {
        [, $stops] = $this->route();
        $stops[2]->update(['is_active' => false]);
        $u = $this->user();

        $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', [])->assertStatus(422);
        $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => 9999])->assertNotFound();
        $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $stops[2]->id])->assertNotFound();
    }

    public function test_favorites_and_eta_are_tenant_scoped(): void
    {
        $a = Transportadora::create(['nombre' => 'A', 'slug' => 'a']);
        $b = Transportadora::create(['nombre' => 'B', 'slug' => 'b']);
        [$busB, $stopsB] = $this->route($b->id);
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 30);
        $userA = $this->user($a->id);

        $this->actingAs($userA, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $stopsB[0]->id])->assertNotFound();
        $this->actingAs($userA, 'sanctum')->getJson("/api/v1/buses/ETA1/eta?stop_id={$stopsB[0]->id}")->assertNotFound();

        // Un favorito previo de otro tenant tampoco se lista.
        FavoriteStop::create(['user_id' => $userA->id, 'stop_id' => $stopsB[0]->id]);
        $this->assertSame([], $this->actingAs($userA, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'));

        $userB = $this->user($b->id);
        $this->actingAs($userB, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $stopsB[0]->id])->assertStatus(201);
    }

    public function test_endpoints_require_authentication(): void
    {
        $this->getJson('/api/v1/buses/ETA1/eta?stop_id=1')->assertUnauthorized();
        $this->getJson('/api/v1/favorites/stops')->assertUnauthorized();
        $this->postJson('/api/v1/favorites/stops', ['stop_id' => 1])->assertUnauthorized();
        $this->deleteJson('/api/v1/favorites/stops/1')->assertUnauthorized();
    }
}
