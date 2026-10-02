<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * Limitadores nombrados: cada endpoint (o grupo auth) tiene su propio contador.
 */
class RateLimitingTest extends TestCase
{
    use RefreshDatabase;

    public function test_auth_endpoints_share_a_budget_of_ten_per_minute(): void
    {
        for ($i = 0; $i < 10; $i++) {
            $this->postJson('/api/v1/auth/login', ['email' => 'x@test.co', 'password' => 'bad'])->assertStatus(401);
        }

        $this->postJson('/api/v1/auth/login', ['email' => 'x@test.co', 'password' => 'bad'])->assertStatus(429);
        $this->postJson('/api/v1/auth/register', [])->assertStatus(429);
    }

    public function test_authenticated_endpoints_do_not_share_counters(): void
    {
        $user = User::factory()->create(['role' => 'pasajero']);

        // wallet/recharge-mock tiene tope 10/min; agotarlo no afecta a GET wallet ni a stops.
        for ($i = 0; $i < 11; $i++) {
            $last = $this->actingAs($user, 'sanctum')->postJson('/api/v1/wallet/recharge-mock', []);
        }
        $last->assertStatus(429);

        $this->actingAs($user, 'sanctum')->getJson('/api/v1/wallet')->assertOk();
        $this->actingAs($user, 'sanctum')->getJson('/api/v1/stops')->assertOk();
    }

    public function test_counters_are_per_user(): void
    {
        $a = User::factory()->create(['role' => 'pasajero']);
        $b = User::factory()->create(['role' => 'pasajero']);

        for ($i = 0; $i < 11; $i++) {
            $this->actingAs($a, 'sanctum')->postJson('/api/v1/wallet/recharge-mock', []);
        }

        $this->actingAs($b, 'sanctum')->postJson('/api/v1/wallet/recharge-mock', [])->assertStatus(422);
    }
}
