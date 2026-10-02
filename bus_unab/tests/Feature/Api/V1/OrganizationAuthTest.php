<?php

namespace Tests\Feature\Api\V1;

use App\Models\Transportadora;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * White-label — el tenant se deduce del usuario (sin header X-Organization).
 */
class OrganizationAuthTest extends TestCase
{
    use RefreshDatabase;

    private function payload(array $overrides = []): array
    {
        return array_merge([
            'name' => 'Ana Pérez',
            'email' => 'ana@gmail.com',
            'password' => 'Secreta123',
            'password_confirmation' => 'Secreta123',
        ], $overrides);
    }

    public function test_register_with_active_organization_assigns_tenant(): void
    {
        $t = Transportadora::create(['nombre' => 'Acme', 'slug' => 'acme']);

        $res = $this->postJson('/api/v1/auth/register', $this->payload(['organization' => 'acme']))
            ->assertOk()
            ->assertJsonPath('data.user.organization_slug', 'acme');

        $user = User::where('email', 'ana@gmail.com')->firstOrFail();
        $this->assertSame($t->id, $user->transportadora_id);
        $this->assertSame('pasajero', $user->role);
        $this->assertNotEmpty($res->json('data.access_token'));
    }

    public function test_register_without_organization_has_null_slug(): void
    {
        $this->postJson('/api/v1/auth/register', $this->payload())
            ->assertOk()
            ->assertJsonPath('data.user.organization_slug', null);

        $this->assertNull(User::where('email', 'ana@gmail.com')->value('transportadora_id'));
    }

    public function test_register_with_unknown_or_inactive_organization_is_rejected_identically(): void
    {
        Transportadora::create(['nombre' => 'Off', 'slug' => 'off', 'activo' => false]);

        $missing = $this->postJson('/api/v1/auth/register', $this->payload(['organization' => 'nope']))
            ->assertStatus(422);
        $inactive = $this->postJson('/api/v1/auth/register', $this->payload(['organization' => 'off']))
            ->assertStatus(422);

        $this->assertSame($missing->getContent(), $inactive->getContent());
        $this->assertDatabaseMissing('users', ['email' => 'ana@gmail.com']);
    }

    public function test_login_and_me_expose_organization_slug(): void
    {
        $t = Transportadora::create(['nombre' => 'Acme', 'slug' => 'acme']);
        User::factory()->create([
            'email' => 'emp@acme.co',
            'password' => bcrypt('Secreta123'),
            'role' => 'pasajero',
            'transportadora_id' => $t->id,
        ]);

        $login = $this->postJson('/api/v1/auth/login', ['email' => 'emp@acme.co', 'password' => 'Secreta123'])
            ->assertOk()
            ->assertJsonPath('data.user.organization_slug', 'acme');

        $this->withToken($login->json('data.access_token'))
            ->getJson('/api/v1/auth/me')
            ->assertOk()
            ->assertJsonPath('data.organization_slug', 'acme');
    }

    public function test_user_without_tenant_gets_null_slug_on_me(): void
    {
        $user = User::factory()->create(['role' => 'pasajero', 'transportadora_id' => null]);

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/auth/me')
            ->assertOk()
            ->assertJsonPath('data.organization_slug', null);
    }
}
