<?php

namespace Tests\Feature\Api\V1;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

/**
 * H1/H3 · Ola de roles del pivote:
 *  - 'student' eliminado del producto (quien se auto-registra es PASAJERO).
 *  - POST /auth/register público solo crea pasajeros.
 *  - Google login SIN candado de dominio @unab (antes 403; ahora 200 + pasajero).
 *  - El login por contraseña funciona para pasajeros de cualquier dominio.
 */
class RegisterPasajeroTest extends TestCase
{
    use RefreshDatabase;

    private function payload(array $overrides = []): array
    {
        return array_merge([
            'name'                  => 'Ana Pérez',
            'email'                 => 'ana@gmail.com',
            'password'              => 'Secreta123',
            'password_confirmation' => 'Secreta123',
        ], $overrides);
    }

    public function test_register_creates_pasajero_and_token_is_usable(): void
    {
        $response = $this->postJson('/api/v1/auth/register', $this->payload());

        $response->assertStatus(200)
            ->assertJsonStructure(['data' => ['access_token', 'user']]);

        $user = User::where('email', 'ana@gmail.com')->firstOrFail();
        $this->assertSame('pasajero', $user->role);
        $this->assertTrue($user->isPasajero());

        // El token devuelto sirve para rutas protegidas.
        $this->withToken($response->json('data.access_token'))
            ->getJson('/api/v1/auth/me')
            ->assertStatus(200);
    }

    public function test_register_rejects_duplicate_email(): void
    {
        User::factory()->create(['email' => 'ana@gmail.com']);

        $this->postJson('/api/v1/auth/register', $this->payload())
            ->assertStatus(422)
            ->assertJsonValidationErrors('email');
    }

    public function test_register_rejects_weak_or_unconfirmed_password(): void
    {
        $this->postJson('/api/v1/auth/register', $this->payload([
            'password'              => 'corta',
            'password_confirmation' => 'corta',
        ]))->assertStatus(422)->assertJsonValidationErrors('password');

        $this->postJson('/api/v1/auth/register', $this->payload([
            'password_confirmation' => 'Distinta123',
        ]))->assertStatus(422)->assertJsonValidationErrors('password');
    }

    public function test_registered_pasajero_can_login_from_any_domain(): void
    {
        $this->postJson('/api/v1/auth/register', $this->payload())->assertStatus(200);

        $this->postJson('/api/v1/auth/login', [
            'email'    => 'ana@gmail.com',
            'password' => 'Secreta123',
        ])->assertStatus(200)
            ->assertJsonPath('data.user.role', 'pasajero');
    }

    public function test_google_login_no_longer_locks_institutional_domain(): void
    {
        config(['services.google.client_id' => 'test-client.apps.googleusercontent.com']);

        Http::fake([
            'oauth2.googleapis.com/*' => Http::response([
                'sub'     => 'google-gmail-001',
                'email'   => 'pepito@gmail.com',
                'name'    => 'Pepito Ciudad',
                'picture' => null,
                'aud'     => 'test-client.apps.googleusercontent.com',
                'exp'     => time() + 3600,
            ], 200),
        ]);

        $this->postJson('/api/v1/auth/google', ['id_token' => 'valid-token'])
            ->assertStatus(200);

        $this->assertDatabaseHas('users', [
            'email'     => 'pepito@gmail.com',
            'google_id' => 'google-gmail-001',
            'role'      => 'pasajero',
        ]);
    }

    public function test_assignable_roles_are_scoped_by_panel(): void
    {
        $empresa = User::assignableRoles('empresa');
        $this->arrayNotHasRole($empresa, 'admin');
        $this->arrayNotHasRole($empresa, 'super_admin');
        $this->arrayNotHasRole($empresa, 'tenant_admin');
        $this->assertArrayHasKey('driver', $empresa);
        $this->assertArrayHasKey('pasajero', $empresa);

        $admin = User::assignableRoles('admin');
        $this->assertArrayHasKey('tenant_admin', $admin);
        $this->assertArrayHasKey('super_admin', $admin);

        $this->assertSame('driver', User::defaultRoleForPanel('empresa'));
        $this->assertSame('pasajero', User::defaultRoleForPanel('admin'));
    }

    private function arrayNotHasRole(array $roles, string $role): void
    {
        $this->assertArrayNotHasKey($role, $roles, "el panel no debe poder asignar '{$role}'");
    }
}
