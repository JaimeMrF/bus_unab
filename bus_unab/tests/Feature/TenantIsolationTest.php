<?php

namespace Tests\Feature;

use App\Models\Bus;
use App\Models\Concerns\BelongsToTenant;
use App\Models\Concerns\TenantContext;
use App\Models\Stop;
use App\Models\Transportadora;
use App\Models\User;
use App\Services\GpsMobileService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * M3 · S3.1.2 — El global scope de tenant debe ser TRANSPARENTE por defecto
 * (no rompe API publicada/paneles/tests legados) y AISLAR solo dentro de
 * withTenant(). BelongsToTenant se prueba a través de Bus/Stop/User, que
 * son los modelos que lo usan.
 */
class TenantIsolationTest extends TestCase
{
    use RefreshDatabase;

    private Transportadora $tenantA;

    private Transportadora $tenantB;

    protected function setUp(): void
    {
        parent::setUp();

        $this->tenantA = Transportadora::create([
            'nombre' => 'Tenant A',
            'slug'   => 'tenant-a',
        ]);
        $this->tenantB = Transportadora::create([
            'nombre' => 'Tenant B',
            'slug'   => 'tenant-b',
        ]);
    }

    private function makeBus(string $plate, ?int $tenantId): Bus
    {
        static $seq = 0;

        return Bus::create([
            'name'                => "Ruta {$plate}",
            'plate'               => $plate,
            'external_vehicle_id' => 95000 + ++$seq,
            'capacity'            => 40,
            'is_active'           => true,
            'transportadora_id'   => $tenantId,
        ]);
    }

    public function test_scope_is_disabled_by_default_all_rows_visible(): void
    {
        $this->makeBus('A1', $this->tenantA->id);
        $this->makeBus('B1', $this->tenantB->id);
        $this->makeBus('N1', null); // dato compartido de ciudad

        // Sin contexto: exactamente el comportamiento pre-pivote.
        $this->assertCount(3, Bus::all());
    }

    public function test_withTenant_filters_rows_of_other_tenants(): void
    {
        $this->makeBus('A1', $this->tenantA->id);
        $this->makeBus('B1', $this->tenantB->id);
        $this->makeBus('N1', null);

        $platesA = Bus::withTenant($this->tenantA->id, fn () => Bus::pluck('plate')->all());

        // Solo las del tenant A: las ajenas Y las compartidas (NULL) quedan fuera
        // del panel /empresa (decision DISEÑO_DB §3: el scope es estricto).
        $this->assertSame(['A1'], $platesA);
    }

    public function test_creating_hook_auto_assigns_active_tenant(): void
    {
        $bus = Bus::withTenant($this->tenantB->id, function () {
            return Bus::create([
                'name'                => 'Auto-asignado',
                'plate'               => 'AUTO1',
                'external_vehicle_id' => 97140,
                'capacity'            => 40,
                'is_active'           => true,
                // transportadora_id NO se pasa: debe rellenarse del contexto.
            ]);
        });

        $this->assertSame($this->tenantB->id, $bus->fresh()->transportadora_id);
    }

    public function test_creating_hook_does_not_override_explicit_tenant(): void
    {
        $bus = Bus::withTenant($this->tenantB->id, function () {
            return Bus::create([
                'name'                => 'Explícito',
                'plate'               => 'EXP1',
                'external_vehicle_id' => 97141,
                'capacity'            => 40,
                'is_active'           => true,
                'transportadora_id'   => $this->tenantA->id,
            ]);
        });

        $this->assertSame($this->tenantA->id, $bus->fresh()->transportadora_id);
    }

    public function test_scope_without_tenant_is_the_escape_hatch(): void
    {
        $this->makeBus('A1', $this->tenantA->id);
        $this->makeBus('B1', $this->tenantB->id);

        Bus::withTenant($this->tenantA->id, function () {
            $this->assertCount(1, Bus::all());
            // Super Admin / reportes: ignora el contexto activo.
            $this->assertCount(2, Bus::withoutTenant()->get());
        });
    }

    public function test_context_is_restored_after_callback_and_on_exception(): void
    {
        $this->makeBus('A1', $this->tenantA->id);
        $this->makeBus('B1', $this->tenantB->id);

        // Anidado: A dentro de nada, B dentro de A, vuelve a A, luego libre.
        Bus::withTenant($this->tenantA->id, function () {
            $this->assertCount(1, Bus::all());

            Bus::withTenant($this->tenantB->id, function () {
                $this->assertCount(1, Bus::all());
                $this->assertSame('B1', Bus::first()->plate);
            });

            $this->assertSame('A1', Bus::first()->plate);
        });

        $this->assertCount(2, Bus::all());

        // Excepción dentro del bloque TAMPOCO deja contexto contaminado.
        try {
            Bus::withTenant($this->tenantA->id, function () {
                $this->assertCount(1, Bus::all());
                throw new \RuntimeException('boom');
            });
        } catch (\RuntimeException) {
            // esperado
        }

        $this->assertCount(2, Bus::all(), 'el contexto debe restaurarse aun con excepción');
    }

    public function test_relation_transportadora_resolves_in_both_directions(): void
    {
        $bus  = $this->makeBus('A1', $this->tenantA->id);
        $stop = Stop::create([
            'name' => 'Parada A', 'latitude' => 7.1, 'longitude' => -73.1,
            'transportadora_id' => $this->tenantA->id,
        ]);
        $user = User::factory()->create(['transportadora_id' => $this->tenantA->id]);

        $this->assertSame($this->tenantA->id, $bus->transportadora->id);
        $this->assertSame($this->tenantA->id, $stop->transportadora->id);
        $this->assertSame($this->tenantA->id, $user->transportadora->id);

        $this->assertTrue($this->tenantA->buses->contains($bus));
        $this->assertTrue($this->tenantA->stops->contains($stop));
        $this->assertTrue($this->tenantA->users->contains($user));
    }

    public function test_trait_is_applied_on_all_core_models(): void
    {
        foreach ([Bus::class, Stop::class, User::class, \App\Models\PointOfInterest::class] as $model) {
            $this->assertContains(
                BelongsToTenant::class,
                class_uses_recursive($model),
                "{$model} debe usar BelongsToTenant",
            );
        }
    }

    // ==================================================================
    // M3 · S3.3.3 — Nivel HTTP: middleware `tenant.scope` en la API v1.
    // Patrón de auth copiado de tests/Feature/Api/V1/BusTest.php:
    // actingAs($user, 'sanctum'). Los usuarios con transportadora NULL
    // (pasajeros) DEBEN seguir viendo todo: compatibilidad garantizada.
    // ==================================================================

    private function makeStop(string $name, ?int $tenantId): Stop
    {
        return Stop::create([
            'name'              => $name,
            'latitude'          => 7.1,
            'longitude'         => -73.1,
            'is_active'         => true,
            'transportadora_id' => $tenantId,
        ]);
    }

    private function gpsFix(string $plate): array
    {
        return [
            'Placa'    => $plate,
            'Latitud'  => '7.1218',
            'Longitud' => '-73.1158',
            'Sentido'  => '90',
        ];
    }

    public function test_api_tenant_member_sees_only_own_buses_in_index(): void
    {
        $this->makeBus('AAA1', $this->tenantA->id);
        $this->makeBus('BBB1', $this->tenantB->id);

        $driverA = User::factory()->driver()->create(['transportadora_id' => $this->tenantA->id]);

        // El GPS reporta ambas flotas: el scope del tenant debe dejar solo A.
        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getAllBuses')
            ->andReturn([$this->gpsFix('AAA1'), $this->gpsFix('BBB1')]);

        $this->actingAs($driverA, 'sanctum')
            ->getJson('/api/v1/buses')
            ->assertStatus(200)
            ->assertJsonCount(1, 'data')
            ->assertJsonFragment(['plate' => 'AAA1']);
    }

    public function test_api_cross_tenant_bus_detail_returns_404(): void
    {
        $this->makeBus('AAA1', $this->tenantA->id);
        $this->makeBus('BBB1', $this->tenantB->id);

        $driverA = User::factory()->driver()->create(['transportadora_id' => $this->tenantA->id]);

        // BBB1 existe y está activa, pero es de otra transportadora:
        // 404 silencioso (no se confirma la existencia del recurso ajeno).
        $this->actingAs($driverA, 'sanctum')
            ->getJson('/api/v1/buses/BBB1')
            ->assertStatus(404)
            ->assertJson(['success' => false]);
    }

    public function test_api_user_without_tenant_still_sees_all_buses(): void
    {
        $this->makeBus('AAA1', $this->tenantA->id);
        $this->makeBus('BBB1', $this->tenantB->id);

        $passenger = User::factory()->create(); // transportadora_id NULL (rol student)

        $mock = $this->mock(GpsMobileService::class);
        $mock->shouldReceive('getAllBuses')
            ->andReturn([$this->gpsFix('AAA1'), $this->gpsFix('BBB1')]);

        // Backward compat: el pasajero de ciudad sigue viendo toda la flota.
        $this->actingAs($passenger, 'sanctum')
            ->getJson('/api/v1/buses')
            ->assertStatus(200)
            ->assertJsonCount(2, 'data');
    }

    public function test_api_tenant_member_sees_only_own_stops(): void
    {
        $this->makeStop('Parada A', $this->tenantA->id);
        $this->makeStop('Parada B', $this->tenantB->id);
        $this->makeStop('Parada ciudad', null); // compartida: scope estricto la excluye

        $adminA = User::factory()->admin()->create(['transportadora_id' => $this->tenantA->id]);

        $this->actingAs($adminA, 'sanctum')
            ->getJson('/api/v1/stops')
            ->assertStatus(200)
            ->assertJsonCount(1, 'data')
            ->assertJsonFragment(['name' => 'Parada A']);
    }

    public function test_api_tenant_context_does_not_leak_after_request(): void
    {
        $this->makeStop('Parada A', $this->tenantA->id);
        $this->makeStop('Parada B', $this->tenantB->id);

        $adminA = User::factory()->admin()->create(['transportadora_id' => $this->tenantA->id]);

        $this->actingAs($adminA, 'sanctum')
            ->getJson('/api/v1/stops')
            ->assertStatus(200)
            ->assertJsonCount(1, 'data');

        // El finally de TenantContext::run() debe haber restaurado "sin
        // contexto": nada de filtrado fantasma en consultas posteriores.
        $this->assertNull(TenantContext::id());
        $this->assertCount(2, Stop::all());
    }
}
