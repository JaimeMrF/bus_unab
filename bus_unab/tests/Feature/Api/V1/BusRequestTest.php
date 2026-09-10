<?php

namespace Tests\Feature\Api\V1;

use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\RouteStop;
use App\Models\Stop;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class BusRequestTest extends TestCase
{
    use RefreshDatabase;

    private User $student;
    private Bus  $bus;
    private Stop $stop;

    protected function setUp(): void
    {
        parent::setUp();

        $this->student = User::factory()->create(['role' => 'pasajero']);

        $this->bus = Bus::create([
            'name'                => 'Ruta 1',
            'plate'               => 'RUTA1',
            'external_vehicle_id' => 97141,
            'capacity'            => 40,
            'is_active'           => true,
        ]);

        $this->stop = Stop::create([
            'name'          => 'UNAB Campus',
            'address'       => 'Calle 48',
            'latitude'      => 7.1218,
            'longitude'     => -73.1158,
            'radius_meters' => 100,
            'is_active'     => true,
        ]);

        RouteStop::create([
            'bus_id'            => $this->bus->id,
            'stop_id'           => $this->stop->id,
            'order'             => 1,
            'estimated_minutes' => 5,
        ]);
    }

    // ── Store ────────────────────────────────────────────────────────────────────

    public function test_student_can_create_bus_request(): void
    {
        $response = $this->actingAs($this->student, 'sanctum')
            ->postJson('/api/v1/requests', [
                'bus_id'  => $this->bus->id,
                'stop_id' => $this->stop->id,
            ]);

        $response->assertStatus(201)
            ->assertJson(['success' => true])
            ->assertJsonStructure(['data' => [
                'request', 'current_occupancy', 'capacity', 'percentage', 'level', 'is_full',
            ]]);

        $this->assertDatabaseHas('bus_requests', [
            'user_id' => $this->student->id,
            'bus_id'  => $this->bus->id,
            'stop_id' => $this->stop->id,
            'status'  => 'pending',
        ]);
    }

    public function test_request_requires_authentication(): void
    {
        $this->postJson('/api/v1/requests', [
            'bus_id'  => $this->bus->id,
            'stop_id' => $this->stop->id,
        ])->assertStatus(401);
    }

    public function test_request_validates_required_fields(): void
    {
        $this->actingAs($this->student, 'sanctum')
            ->postJson('/api/v1/requests', [])
            ->assertStatus(422);
    }

    public function test_request_rejects_stop_not_on_route(): void
    {
        $otherStop = Stop::create([
            'name'          => 'Otra Parada',
            'address'       => 'Calle 10',
            'latitude'      => 7.2000,
            'longitude'     => -73.2000,
            'radius_meters' => 100,
            'is_active'     => true,
        ]);

        $this->actingAs($this->student, 'sanctum')
            ->postJson('/api/v1/requests', [
                'bus_id'  => $this->bus->id,
                'stop_id' => $otherStop->id,
            ])->assertStatus(400);
    }

    public function test_new_request_cancels_previous_pending_request(): void
    {
        // Primera solicitud
        $first = BusRequest::create([
            'user_id' => $this->student->id,
            'bus_id'  => $this->bus->id,
            'stop_id' => $this->stop->id,
            'status'  => 'pending',
        ]);

        // Nueva solicitud del mismo usuario para el mismo bus
        $this->actingAs($this->student, 'sanctum')
            ->postJson('/api/v1/requests', [
                'bus_id'  => $this->bus->id,
                'stop_id' => $this->stop->id,
            ])->assertStatus(201);

        $this->assertDatabaseHas('bus_requests', [
            'id'     => $first->id,
            'status' => 'cancelled',
        ]);
    }

    public function test_is_full_is_true_when_bus_reaches_capacity(): void
    {
        // Llenar el bus hasta capacidad - 1
        $others = User::factory()->count(39)->create();
        foreach ($others as $u) {
            BusRequest::create([
                'user_id' => $u->id,
                'bus_id'  => $this->bus->id,
                'stop_id' => $this->stop->id,
                'status'  => 'pending',
            ]);
        }

        $response = $this->actingAs($this->student, 'sanctum')
            ->postJson('/api/v1/requests', [
                'bus_id'  => $this->bus->id,
                'stop_id' => $this->stop->id,
            ]);

        $response->assertStatus(201);
        // Con 40 solicitudes, el bus debe reportarse lleno
        $this->assertTrue($response->json('data.is_full'));
        $this->assertEquals(40, $response->json('data.current_occupancy'));
    }

    public function test_is_full_is_false_when_bus_has_space(): void
    {
        $response = $this->actingAs($this->student, 'sanctum')
            ->postJson('/api/v1/requests', [
                'bus_id'  => $this->bus->id,
                'stop_id' => $this->stop->id,
            ]);

        $response->assertStatus(201);
        $this->assertFalse($response->json('data.is_full'));
    }

    // ── Cancel ───────────────────────────────────────────────────────────────────

    public function test_student_can_cancel_request(): void
    {
        BusRequest::create([
            'user_id' => $this->student->id,
            'bus_id'  => $this->bus->id,
            'stop_id' => $this->stop->id,
            'status'  => 'pending',
        ]);

        $this->actingAs($this->student, 'sanctum')
            ->deleteJson("/api/v1/requests/{$this->bus->id}")
            ->assertStatus(200)
            ->assertJson(['success' => true]);

        $this->assertDatabaseHas('bus_requests', [
            'user_id' => $this->student->id,
            'status'  => 'cancelled',
        ]);
    }

    public function test_cancel_returns_404_when_no_active_request(): void
    {
        $this->actingAs($this->student, 'sanctum')
            ->deleteJson("/api/v1/requests/{$this->bus->id}")
            ->assertStatus(404);
    }

    // ── Occupancy ────────────────────────────────────────────────────────────────

    public function test_occupancy_returns_zero_when_no_requests(): void
    {
        $this->actingAs($this->student, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1/occupancy')
            ->assertStatus(200)
            ->assertJson([
                'success' => true,
                'data'    => ['current_occupancy' => 0, 'capacity' => 40, 'level' => 'low'],
            ]);
    }

    public function test_occupancy_counts_only_pending_requests(): void
    {
        BusRequest::create([
            'user_id' => $this->student->id,
            'bus_id'  => $this->bus->id,
            'stop_id' => $this->stop->id,
            'status'  => 'pending',
        ]);

        BusRequest::create([
            'user_id' => User::factory()->create()->id,
            'bus_id'  => $this->bus->id,
            'stop_id' => $this->stop->id,
            'status'  => 'cancelled',
        ]);

        $this->actingAs($this->student, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1/occupancy')
            ->assertStatus(200)
            ->assertJson(['data' => ['current_occupancy' => 1]]);
    }

    public function test_occupancy_level_is_high_above_60_percent(): void
    {
        // Crear 25/40 = 62.5% de ocupación
        $users = User::factory()->count(25)->create();
        foreach ($users as $u) {
            BusRequest::create([
                'user_id' => $u->id,
                'bus_id'  => $this->bus->id,
                'stop_id' => $this->stop->id,
                'status'  => 'pending',
            ]);
        }

        $this->actingAs($this->student, 'sanctum')
            ->getJson('/api/v1/buses/RUTA1/occupancy')
            ->assertStatus(200)
            ->assertJson(['data' => ['level' => 'high']]);
    }

    // ── Bus Arrived ──────────────────────────────────────────────────────────────

    public function test_student_cannot_mark_bus_arrived(): void
    {
        $this->actingAs($this->student, 'sanctum')
            ->postJson('/api/v1/buses/RUTA1/arrived', ['stop_id' => $this->stop->id])
            ->assertStatus(403);
    }

    public function test_admin_can_mark_bus_arrived_and_notifies_users(): void
    {
        $admin = User::factory()->admin()->create();

        BusRequest::create([
            'user_id' => $this->student->id,
            'bus_id'  => $this->bus->id,
            'stop_id' => $this->stop->id,
            'status'  => 'pending',
        ]);

        $response = $this->actingAs($admin, 'sanctum')
            ->postJson('/api/v1/buses/RUTA1/arrived', ['stop_id' => $this->stop->id]);

        $response->assertStatus(200)
            ->assertJson([
                'success' => true,
                'data'    => ['notified_users' => 1],
            ]);

        $this->assertDatabaseHas('bus_requests', [
            'user_id' => $this->student->id,
            'status'  => 'boarded',
        ]);
    }

    public function test_driver_can_mark_bus_arrived(): void
    {
        $driver = User::factory()->driver()->create();

        $this->actingAs($driver, 'sanctum')
            ->postJson('/api/v1/buses/RUTA1/arrived', ['stop_id' => $this->stop->id])
            ->assertStatus(200)
            ->assertJson(['data' => ['notified_users' => 0]]);
    }

    public function test_bus_arrived_returns_zero_when_no_pending_requests(): void
    {
        $admin = User::factory()->admin()->create();

        $this->actingAs($admin, 'sanctum')
            ->postJson('/api/v1/buses/RUTA1/arrived', ['stop_id' => $this->stop->id])
            ->assertStatus(200)
            ->assertJson(['data' => ['notified_users' => 0]]);
    }
}
