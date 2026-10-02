<?php

namespace Tests\Feature;

use App\Models\Bus;
use App\Models\User;
use App\Services\GpsMobileService;
use App\Support\DriverLocation;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

/**
 * Contrato único de driver_location_{PLACA} y su uso en local/testing.
 */
class DriverLocationTest extends TestCase
{
    use RefreshDatabase;

    private function bus(string $plate = 'MB101', int $ext = 900001): Bus
    {
        return Bus::create(['name' => 'Demo', 'plate' => $plate, 'external_vehicle_id' => $ext, 'capacity' => 40, 'is_active' => true]);
    }

    public function test_put_and_get_follow_the_contract(): void
    {
        DriverLocation::put('mb-101', 7.12, -73.11, 90, 40);

        $loc = DriverLocation::get('MB101');

        $this->assertSame(7.12, $loc['latitude']);
        $this->assertSame(-73.11, $loc['longitude']);
        $this->assertSame(90, $loc['heading']);
        $this->assertSame(40, $loc['speed_kmh']);
        $this->assertNotEmpty($loc['updated_at']);
        $this->assertSame('driver_location_MB101', DriverLocation::key('mb-101'));
    }

    public function test_get_tolerates_legacy_lat_lng_entries_and_missing_ones(): void
    {
        Cache::put('driver_location_OLD1', ['lat' => 1.5, 'lng' => 2.5, 'heading' => 10], 60);

        $this->assertSame(1.5, DriverLocation::get('OLD1')['latitude']);
        $this->assertNull(DriverLocation::get('NOPE'));

        DriverLocation::forget('OLD1');
        $this->assertNull(DriverLocation::get('OLD1'));
    }

    public function test_driver_endpoint_writes_the_unified_contract(): void
    {
        $this->bus('RUTA1', 97141);
        $driver = User::factory()->create(['role' => 'driver']);

        $this->actingAs($driver, 'sanctum')
            ->postJson('/api/v1/buses/RUTA1/location', ['lat' => 7.1, 'lng' => -73.1, 'heading' => 180])
            ->assertOk();

        $loc = DriverLocation::get('RUTA1');
        $this->assertSame(7.1, $loc['latitude']);
        $this->assertSame(180, $loc['heading']);
        $this->assertNotNull($loc['updated_at']);

        $this->actingAs($driver, 'sanctum')->deleteJson('/api/v1/buses/RUTA1/location')->assertOk();
        $this->assertNull(DriverLocation::get('RUTA1'));
    }

    public function test_service_returns_live_buses_even_if_external_gps_is_down(): void
    {
        Http::fake(['*' => Http::response([], 500)]);
        $this->bus();
        DriverLocation::put('MB101', 7.13, -73.12, 45);

        $vehicles = app(GpsMobileService::class)->getAllBuses();

        $this->assertCount(1, $vehicles);
        $this->assertSame('MB101', $vehicles[0]['Placa']);
        $this->assertSame(7.13, $vehicles[0]['Latitud']);
        $this->assertSame(45, $vehicles[0]['Sentido']);
    }

    public function test_service_keeps_original_result_without_live_locations(): void
    {
        Http::fake(['*' => Http::response([], 500)]);
        $this->bus();

        $this->assertNull(app(GpsMobileService::class)->getAllBuses());
    }

    public function test_service_overrides_external_position_and_keeps_other_vehicles(): void
    {
        config(['gpsmobile.base_url' => 'http://gps.test']);
        Http::fake(['*/api/Home/*' => Http::response(['sucess' => true, 'response' => ['veh' => [
            ['Placa' => 'MB101', 'Latitud' => 1.0, 'Longitud' => 1.0, 'Sentido' => 0],
            ['Placa' => 'OTRO', 'Latitud' => 2.0, 'Longitud' => 2.0, 'Sentido' => 5],
        ]]])]);
        $this->bus();
        DriverLocation::put('MB101', 7.13, -73.12, 45);

        $byPlate = collect(app(GpsMobileService::class)->getAllBuses())->keyBy('Placa');

        $this->assertSame(7.13, $byPlate['MB101']['Latitud']);
        $this->assertEquals(2.0, $byPlate['OTRO']['Latitud']);
        $this->assertCount(2, $byPlate);
    }

    public function test_public_buses_and_show_endpoints_use_live_positions(): void
    {
        Http::fake(['*' => Http::response([], 500)]);
        $bus = $this->bus();
        DriverLocation::put('MB101', 7.13, -73.12, 270, 33);
        $user = User::factory()->create(['role' => 'pasajero']);

        $this->actingAs($user, 'sanctum')->getJson('/api/v1/buses')
            ->assertOk()
            ->assertJsonPath('data.0.plate', 'MB101')
            ->assertJsonPath('data.0.latitude', 7.13)
            ->assertJsonPath('data.0.heading', 270);

        $this->actingAs($user, 'sanctum')->getJson('/api/v1/buses/MB101')
            ->assertOk()
            ->assertJsonPath('data.id', $bus->id)
            ->assertJsonPath('data.longitude', -73.12)
            ->assertJsonPath('data.speed_kmh', 33)
            ->assertJsonPath('data.heading', 270);
    }

    public function test_production_ignores_live_locations(): void
    {
        Http::fake(['*' => Http::response([], 500)]);
        $this->bus();
        DriverLocation::put('MB101', 7.13, -73.12, 45);

        $this->app['env'] = 'production';

        $this->assertNull(app(GpsMobileService::class)->getAllBuses());
        $this->assertNull(app(GpsMobileService::class)->getBusDetail(900001));
    }
}
