<?php

namespace Tests\Feature\Admin;

use App\Models\Bus;
use App\Models\User;
use App\Services\GpsMobileService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Tests\TestCase;

class BusMapDataTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        // Sin red en pruebas: el GPS no devuelve posición.
        $this->mock(GpsMobileService::class, function ($m) {
            $m->shouldReceive('getBusDetail')->andReturn(null);
            $m->shouldReceive('getAllBuses')->andReturn([]);
        });
    }

    public function test_super_admin_gets_map_data(): void
    {
        // El seeder demo crea super_admin; antes solo pasaba el rol "admin" y el mapa quedaba vacío (403).
        $super = User::factory()->create(['role' => 'super_admin']);

        $this->actingAs($super)->getJson('/admin/map-data')->assertOk();
    }

    public function test_passenger_cannot_read_map_data(): void
    {
        $this->actingAs(User::factory()->create())->getJson('/admin/map-data')->assertForbidden();
    }

    public function test_tenant_admin_sees_only_own_fleet(): void
    {
        $mine = \App\Models\Transportadora::create(['nombre' => 'Mia', 'slug' => 'mia']);
        $other = \App\Models\Transportadora::create(['nombre' => 'Ajena', 'slug' => 'ajena']);
        Bus::withoutGlobalScopes()->create(['name' => 'Mia', 'plate' => 'MIA1', 'external_vehicle_id' => 11, 'capacity' => 40, 'is_active' => true, 'transportadora_id' => $mine->id]);
        Bus::withoutGlobalScopes()->create(['name' => 'Ajena', 'plate' => 'AJE1', 'external_vehicle_id' => 12, 'capacity' => 40, 'is_active' => true, 'transportadora_id' => $other->id]);

        $admin = User::factory()->create(['role' => 'tenant_admin', 'transportadora_id' => $mine->id]);

        $plates = collect($this->actingAs($admin)->getJson('/admin/map-data')->assertOk()->json('buses'))->pluck('plate');

        $this->assertSame(['MIA1'], $plates->all());
    }

    public function test_driver_full_flag_marks_bus_full_on_admin_map(): void
    {
        $bus = Bus::create([
            'name' => 'Ruta 1', 'plate' => 'RUTA1', 'external_vehicle_id' => 1,
            'capacity' => 40, 'is_active' => true,
        ]);
        Cache::put(Bus::manualFullKey($bus->plate), true, 60);

        $super = User::factory()->create(['role' => 'super_admin']);

        $buses = $this->actingAs($super)->getJson('/admin/map-data')->assertOk()->json('buses');
        $row = collect($buses)->firstWhere('plate', 'RUTA1');

        $this->assertSame('full', $row['occupancy']['level']);
    }
}
