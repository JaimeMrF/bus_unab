<?php

namespace Tests\Feature;

use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\Stop;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class QrHardeningTest extends TestCase
{
    use RefreshDatabase;

    private function legacyRequest(): array
    {
        $passenger = User::factory()->create(['role' => 'pasajero']);
        $bus = Bus::create(['name' => 'B', 'plate' => 'B1', 'external_vehicle_id' => 5, 'capacity' => 40, 'is_active' => true]);
        $stop = Stop::create(['name' => 'S', 'latitude' => 7.1, 'longitude' => -73.1, 'radius_meters' => 50, 'is_active' => true]);
        $req = BusRequest::create(['user_id' => $passenger->id, 'bus_id' => $bus->id, 'stop_id' => $stop->id, 'status' => 'pending']);

        return [$req, ['request_id' => $req->id, 'user_id' => $passenger->id, 'bus_id' => $bus->id, 'stop_id' => $stop->id]];
    }

    public function test_legacy_validate_rejects_future_timestamps_but_tolerates_small_skew(): void
    {
        [$req, $p] = $this->legacyRequest();
        $driver = User::factory()->create(['role' => 'driver']);
        $now = (int) (microtime(true) * 1000);

        $this->actingAs($driver, 'sanctum')->postJson('/api/v1/qr/validate', $p + ['ts' => $now + 10 * 365 * 86_400_000])->assertStatus(422);
        $this->actingAs($driver, 'sanctum')->postJson('/api/v1/qr/validate', $p + ['ts' => $now + 60_000])->assertStatus(422);
        $this->assertSame('pending', $req->fresh()->status);

        $this->actingAs($driver, 'sanctum')->postJson('/api/v1/qr/validate', $p + ['ts' => $now + 2_000])->assertOk();
        $this->assertSame('boarded', $req->fresh()->status);
    }
}
