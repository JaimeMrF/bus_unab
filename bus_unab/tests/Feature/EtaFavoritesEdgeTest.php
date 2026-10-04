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
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

/**
 * QA T20 — casos límite y de abuso de /eta, ?eta=1, favoritos y ETag/304.
 * Complementa EtaAndFavoritesTest (camino feliz de backend).
 */
class EtaFavoritesEdgeTest extends TestCase
{
    use RefreshDatabase;

    private int $seq = 0;

    /** @return array{0:Bus,1:array<int,Stop>} Ruta recta al norte con 3 paradas (0.01° ≈ 1105 m). */
    private function route(?int $tenantId = null, string $plate = 'ETA1'): array
    {
        $this->seq++;
        $bus = Bus::create([
            'name' => "B$plate", 'plate' => $plate, 'external_vehicle_id' => 9000 + $this->seq,
            'capacity' => 40, 'is_active' => true, 'transportadora_id' => $tenantId,
        ]);
        $stops = [];
        foreach ([0.00, 0.01, 0.02] as $i => $d) {
            $stop = Stop::create([
                'name' => "$plate-P$i", 'latitude' => 7.10 + $d, 'longitude' => -73.1,
                'radius_meters' => 50, 'is_active' => true, 'transportadora_id' => $tenantId,
            ]);
            RouteStop::create(['bus_id' => $bus->id, 'stop_id' => $stop->id, 'order' => $i + 1]);
            BusRouteWaypoint::create(['bus_id' => $bus->id, 'order' => $i + 1, 'latitude' => 7.10 + $d, 'longitude' => -73.1]);
            $stops[] = $stop;
        }

        return [$bus, $stops];
    }

    /** Hallazgos abiertos: omitidos por defecto; ejecutar con RUN_QA_FINDINGS=1. */
    private function skipUnlessFindings(): void
    {
        if (! env('RUN_QA_FINDINGS')) {
            $this->markTestSkipped('Hallazgo abierto: RUN_QA_FINDINGS=1 para ejecutarlo.');
        }
    }

    private function user(?int $tenantId = null): User
    {
        return User::factory()->create(['role' => 'pasajero', 'transportadora_id' => $tenantId]);
    }

    private function tenant(string $slug): Transportadora
    {
        return Transportadora::create(['nombre' => strtoupper($slug), 'slug' => $slug]);
    }

    // ── /eta ─────────────────────────────────────────────────────────

    public function test_bus_without_position_is_503_and_never_500(): void
    {
        [, $stops] = $this->route();
        Http::fake(['*' => Http::response([], 500)]); // GPS externo caído y sin posición del conductor

        $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[1]->id}")
            ->assertStatus(503)
            ->assertJsonPath('success', false);
    }

    public function test_stop_id_must_be_a_positive_integer(): void
    {
        [, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 30);
        $u = $this->user();

        foreach (['abc', '1.5', '', '[]', '1e3', '99999999999999999999'] as $bad) {
            $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/eta?stop_id='.urlencode($bad))
                ->assertStatus(422);
        }
        foreach ([0, -1] as $notFound) {
            $this->actingAs($u, 'sanctum')->getJson("/api/v1/buses/ETA1/eta?stop_id=$notFound")->assertNotFound();
        }
        $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/eta?stop_id[]='.$stops[0]->id)->assertStatus(422);
    }

    public function test_inactive_stop_or_bus_is_404(): void
    {
        [$bus, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 30);
        $u = $this->user();

        $stops[1]->update(['is_active' => false]);
        $this->actingAs($u, 'sanctum')->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[1]->id}")->assertNotFound();

        $bus->update(['is_active' => false]);
        $this->actingAs($u, 'sanctum')->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[2]->id}")->assertNotFound();
    }

    public function test_plate_is_case_insensitive_and_sanitised(): void
    {
        [, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 30);

        $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/eta-1/eta?stop_id={$stops[2]->id}")
            ->assertOk();
    }

    public function test_zero_speed_uses_fallback_with_lower_confidence_and_finite_eta(): void
    {
        [, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 0);

        $eta = $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[2]->id}")->assertOk()->json('data');

        $this->assertSame('medium', $eta['confidence']);
        $this->assertGreaterThan(0, $eta['speed_mps']);
        $this->assertIsInt($eta['eta_seconds']);
        $this->assertLessThan(3600, $eta['eta_seconds']);
    }

    public function test_just_below_moving_threshold_is_treated_as_stopped(): void
    {
        [, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 3.5); // 0.97 m/s < 1 m/s

        $this->assertSame('medium', $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[2]->id}")->json('data.confidence'));
    }

    public function test_bus_already_at_the_stop_gives_zero_distance_not_negative_or_nan(): void
    {
        [, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.11, -73.1, 0, 30);

        $eta = $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[1]->id}")->assertOk()->json('data');

        $this->assertLessThanOrEqual(20, $eta['distance_m']);
        $this->assertGreaterThanOrEqual(0, $eta['eta_seconds']);
    }

    public function test_stale_driver_position_lowers_confidence(): void
    {
        [, $stops] = $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 36);
        $this->travel(2)->minutes();

        $c = $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$stops[2]->id}");

        if ($c->status() === 200) {
            $this->assertNotSame('high', $c->json('data.confidence'), 'posición de hace 2 min no puede ser confianza alta');
        } else {
            $this->assertSame(503, $c->status());
        }
    }

    public function test_bus_without_route_degrades_to_low_confidence_straight_line(): void
    {
        $bus = Bus::create(['name' => 'NR', 'plate' => 'NOROUTE', 'external_vehicle_id' => 9900, 'capacity' => 30, 'is_active' => true]);
        $stop = Stop::create(['name' => 'S', 'latitude' => 7.12, 'longitude' => -73.1, 'radius_meters' => 50, 'is_active' => true]);
        DriverLocation::put('NOROUTE', 7.10, -73.1, 0, 36);

        $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/NOROUTE/eta?stop_id={$stop->id}")
            ->assertOk()->assertJsonPath('data.confidence', 'low');
    }

    /** [HALLAZGO bajo] Se devuelve un ETA para una parada que NO pertenece a la ruta de ese bus. */
    public function test_eta_for_a_stop_outside_the_bus_route_is_rejected(): void
    {
        $this->skipUnlessFindings();
        $this->route();
        DriverLocation::put('ETA1', 7.10, -73.1, 0, 30);
        $foreign = Stop::create(['name' => 'Otra', 'latitude' => 7.50, 'longitude' => -73.5, 'radius_meters' => 50, 'is_active' => true]);

        $this->actingAs($this->user(), 'sanctum')
            ->getJson("/api/v1/buses/ETA1/eta?stop_id={$foreign->id}")
            ->assertStatus(422);
    }

    public function test_eta_cache_does_not_leak_across_buses_or_stops(): void
    {
        [, $stopsA] = $this->route(null, 'AAA1');
        [, $stopsB] = $this->route(null, 'BBB1');
        DriverLocation::put('AAA1', 7.10, -73.1, 0, 36);
        DriverLocation::put('BBB1', 7.19, -73.1, 0, 36);
        $u = $this->user();

        $a = $this->actingAs($u, 'sanctum')->getJson("/api/v1/buses/AAA1/eta?stop_id={$stopsA[2]->id}")->json('data.distance_m');
        $b = $this->actingAs($u, 'sanctum')->getJson("/api/v1/buses/BBB1/eta?stop_id={$stopsB[2]->id}")->json('data.distance_m');

        $this->assertNotSame($a, $b);
    }

    public function test_stops_eta_flag_variants_never_break_and_null_when_no_position(): void
    {
        $this->route();
        Http::fake(['*' => Http::response([], 500)]);
        $u = $this->user();

        foreach (['', '?eta=0', '?eta=abc', '?eta[]=1', '?eta=1'] as $q) {
            $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/stops'.$q)->assertOk();
        }
        $rows = $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/ETA1/stops?eta=1')->json('data');
        $this->assertCount(3, $rows);
        foreach ($rows as $row) {
            $this->assertNull($row['eta_seconds'] ?? null, 'sin posición el ETA por parada es null, no 503');
        }
    }

    public function test_stops_with_eta_flag_skips_eta_for_unknown_bus_with_404(): void
    {
        $this->actingAs($this->user(), 'sanctum')->getJson('/api/v1/buses/NOPE/stops?eta=1')->assertNotFound();
    }

    // ── Favoritos ────────────────────────────────────────────────────

    public function test_favorite_stop_id_type_confusion_is_rejected(): void
    {
        [, $stops] = $this->route();
        $u = $this->user();

        foreach (['array' => [1, 2], 'objeto' => ['a' => 1], 'float' => 1.5, 'texto' => 'abc', 'null' => null] as $name => $bad) {
            $status = $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $bad])->status();
            $this->assertSame(422, $status, "stop_id tipo '$name' devolvió $status");
        }
        foreach ([0, -5] as $nf) {
            $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $nf])->assertNotFound();
        }
        $this->assertSame(0, FavoriteStop::count());

        // Un id numérico como string es válido (clientes que serializan mal).
        $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => (string) $stops[0]->id])->assertStatus(201);
    }

    /** [HALLAZGO bajo] JSON `true` pasa la regla `integer` de Laravel y se guarda como stop_id = 1. */
    public function test_favorite_stop_id_rejects_json_boolean(): void
    {
        $this->skipUnlessFindings();
        $this->route();
        Stop::query()->whereKey(1)->exists() ?: Stop::create(['name' => 'Uno', 'latitude' => 7, 'longitude' => -73, 'radius_meters' => 50, 'is_active' => true]);

        $this->actingAs($this->user(), 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => true])->assertStatus(422);
        $this->assertSame(0, FavoriteStop::count());
    }

    public function test_concurrent_duplicate_adds_leave_a_single_row(): void
    {
        [, $stops] = $this->route();
        $u = $this->user();

        for ($i = 0; $i < 5; $i++) {
            $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $stops[0]->id]);
        }

        $this->assertSame(1, FavoriteStop::where('user_id', $u->id)->count());
    }

    public function test_deleting_never_touches_other_users_or_other_tenants_favorites(): void
    {
        $a = $this->tenant('a');
        $b = $this->tenant('b');
        [, $stopsA] = $this->route($a->id, 'AAA1');
        [, $stopsB] = $this->route($b->id, 'BBB1');
        $userA = $this->user($a->id);
        $userB = $this->user($b->id);
        FavoriteStop::create(['user_id' => $userB->id, 'stop_id' => $stopsB[0]->id]);

        $this->actingAs($userA, 'sanctum')->deleteJson("/api/v1/favorites/stops/{$stopsB[0]->id}")->assertNoContent();

        $this->assertSame(1, FavoriteStop::where('user_id', $userB->id)->count(), 'userA no puede borrar favoritos de userB');
        $this->assertCount(1, $this->actingAs($userB, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'));
        $this->assertSame([], $this->actingAs($userA, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'));
    }

    public function test_favorites_list_hides_stops_deactivated_or_deleted_later(): void
    {
        [, $stops] = $this->route();
        $u = $this->user();
        foreach ([0, 1, 2] as $i) {
            FavoriteStop::create(['user_id' => $u->id, 'stop_id' => $stops[$i]->id]);
        }
        $stops[0]->update(['is_active' => false]);
        $stops[1]->delete();

        $ids = array_column($this->actingAs($u, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'), 'id');

        $this->assertSame([$stops[2]->id], $ids);
    }

    public function test_user_without_tenant_cannot_see_other_users_favorites(): void
    {
        [, $stops] = $this->route();
        $a = $this->user();
        $b = $this->user();
        FavoriteStop::create(['user_id' => $a->id, 'stop_id' => $stops[0]->id]);

        $this->assertSame([], $this->actingAs($b, 'sanctum')->getJson('/api/v1/favorites/stops')->json('data'));
    }

    public function test_favorites_do_not_leak_user_ids_or_internal_fields(): void
    {
        [, $stops] = $this->route();
        $u = $this->user();
        FavoriteStop::create(['user_id' => $u->id, 'stop_id' => $stops[0]->id]);

        $body = $this->actingAs($u, 'sanctum')->getJson('/api/v1/favorites/stops')->getContent();

        $this->assertStringNotContainsString('user_id', $body);
        $this->assertStringNotContainsString('transportadora', $body);
    }

    /** [HALLAZGO bajo] No hay tope de favoritos por usuario: se pueden acumular tantas filas como paradas existan. */
    public function test_favorites_have_a_per_user_cap(): void
    {
        $this->skipUnlessFindings();
        $u = $this->user();
        for ($i = 0; $i < 300; $i++) {
            $stop = Stop::create(['name' => "S$i", 'latitude' => 7 + $i / 1000, 'longitude' => -73, 'radius_meters' => 50, 'is_active' => true]);
            FavoriteStop::create(['user_id' => $u->id, 'stop_id' => $stop->id]);
        }
        $extra = Stop::create(['name' => 'Extra', 'latitude' => 8, 'longitude' => -73, 'radius_meters' => 50, 'is_active' => true]);

        $status = $this->actingAs($u, 'sanctum')->postJson('/api/v1/favorites/stops', ['stop_id' => $extra->id])->status();

        $this->assertContains($status, [409, 422], 'sin tope: el favorito 301 se aceptó con '.$status);
    }

    // ── ETag / 304 ───────────────────────────────────────────────────

    public function test_etag_changes_when_data_changes_and_old_etag_gets_fresh_body(): void
    {
        [, $stops] = $this->route();
        $u = $this->user();

        $first = $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops');
        $etag = $first->headers->get('ETag');
        $stops[0]->update(['name' => 'Renombrada']);

        $second = $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops', ['If-None-Match' => $etag])->assertOk();

        $this->assertNotSame($etag, $second->headers->get('ETag'));
        $this->assertStringContainsString('Renombrada', $second->getContent());
    }

    public function test_etag_of_one_tenant_never_yields_304_for_another(): void
    {
        $a = $this->tenant('a');
        $b = $this->tenant('b');
        $this->route($a->id, 'AAA1');
        [, $stopsB] = $this->route($b->id, 'BBB1');
        $userA = $this->user($a->id);
        $userB = $this->user($b->id);

        $etagA = $this->actingAs($userA, 'sanctum')->getJson('/api/v1/stops')->headers->get('ETag');
        $resB = $this->actingAs($userB, 'sanctum')->getJson('/api/v1/stops', ['If-None-Match' => $etagA])->assertOk();

        $this->assertNotSame($etagA, $resB->headers->get('ETag'));
        $this->assertStringContainsString($stopsB[0]->name, $resB->getContent());
        $this->assertStringNotContainsString('AAA1-P0', $resB->getContent());
    }

    public function test_if_none_match_variants(): void
    {
        $this->route();
        $u = $this->user();
        $etag = $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops')->headers->get('ETag');

        $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops', ['If-None-Match' => 'W/'.$etag])->assertStatus(304);
        $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops', ['If-None-Match' => '"otro", '.$etag])->assertStatus(304);
        $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops', ['If-None-Match' => '"basura"'])->assertOk();
        $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops', ['If-None-Match' => ''])->assertOk();
        $this->actingAs($u, 'sanctum')->getJson('/api/v1/stops', ['If-None-Match' => str_repeat('a', 9000)])->assertOk();
    }

    public function test_304_has_empty_body_and_keeps_cache_headers(): void
    {
        $this->route();
        $u = $this->user();
        $etag = $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/catalog')->headers->get('ETag');

        $r = $this->actingAs($u, 'sanctum')->getJson('/api/v1/buses/catalog', ['If-None-Match' => $etag])->assertStatus(304);

        $this->assertSame('', $r->getContent());
        $this->assertStringContainsString('private', (string) $r->headers->get('Cache-Control'));
        $this->assertStringNotContainsString('public', (string) $r->headers->get('Cache-Control'));
    }

    public function test_etag_endpoints_still_require_authentication(): void
    {
        $this->getJson('/api/v1/stops', ['If-None-Match' => '"x"'])->assertUnauthorized();
        $this->getJson('/api/v1/buses/catalog')->assertUnauthorized();
    }

    protected function tearDown(): void
    {
        Cache::flush();
        parent::tearDown();
    }
}
