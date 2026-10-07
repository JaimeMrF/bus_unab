<?php

namespace Tests\Feature\Api\V1;

use App\Models\Transportadora;
use App\Models\User;
use App\Services\AuthService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\Client\Factory as HttpFactory;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class GoogleAndSessionsTest extends TestCase
{
    use RefreshDatabase;

    private function fakeGoogle(array $overrides = []): void
    {
        config(['services.google.client_id' => 'mine.apps.googleusercontent.com']);
        Http::swap(new HttpFactory); // cada llamada reemplaza el stub anterior
        Http::fake(['oauth2.googleapis.com/*' => Http::response(array_merge([
            'aud' => 'mine.apps.googleusercontent.com',
            'iss' => 'https://accounts.google.com',
            'sub' => 'g-100',
            'email' => 'nuevo@gmail.com',
            'email_verified' => 'true',
            'name' => 'Nuevo',
            'exp' => time() + 600,
        ], $overrides), 200)]);
    }

    private function google(array $extra = [])
    {
        return $this->postJson('/api/v1/auth/google', ['id_token' => 'x'] + $extra);
    }

    // ── (1) email_verified ───────────────────────────────────────────

    public function test_unverified_email_cannot_link_an_existing_admin_account(): void
    {
        $admin = User::factory()->create(['email' => 'boss@empresa.co', 'role' => 'tenant_admin', 'google_id' => null]);
        $this->fakeGoogle(['email' => 'boss@empresa.co', 'email_verified' => 'false']);

        $this->google()->assertStatus(401)->assertJsonPath('message', 'Token de Google inválido o expirado');

        $this->assertNull($admin->fresh()->google_id);
        $this->assertSame(0, $admin->tokens()->count());
    }

    public function test_missing_email_verified_is_treated_as_unverified_for_link_and_create(): void
    {
        User::factory()->create(['email' => 'boss@empresa.co', 'role' => 'driver']);
        $this->fakeGoogle(['email' => 'boss@empresa.co', 'email_verified' => null]);
        $this->google()->assertStatus(401);

        $this->fakeGoogle(['email' => 'otro@gmail.com', 'sub' => 'g-2', 'email_verified' => false]);
        $this->google()->assertStatus(401);
        $this->assertDatabaseMissing('users', ['email' => 'otro@gmail.com']);
    }

    public function test_verified_email_accepts_bool_or_string_true(): void
    {
        $existing = User::factory()->create(['email' => 'ana@gmail.com', 'role' => 'pasajero']);

        $this->fakeGoogle(['email' => 'ana@gmail.com', 'email_verified' => true]);
        $this->google()->assertOk();
        $this->assertSame('g-100', $existing->fresh()->google_id);

        $this->fakeGoogle(['sub' => 'g-3', 'email' => 'new@gmail.com', 'email_verified' => 'true']);
        $this->google()->assertOk();
        $this->assertDatabaseHas('users', ['email' => 'new@gmail.com', 'role' => 'pasajero']);
    }

    public function test_already_linked_account_still_logs_in_by_sub(): void
    {
        User::factory()->create(['email' => 'ana@gmail.com', 'google_id' => 'g-100', 'role' => 'pasajero']);
        $this->fakeGoogle(['email_verified' => 'false']);

        $this->google()->assertOk();
    }

    // ── (2) un token por dispositivo ─────────────────────────────────

    public function test_second_device_does_not_log_out_the_first(): void
    {
        $user = User::factory()->create(['email' => 'u@test.co', 'password' => bcrypt('Secreta123'), 'role' => 'pasajero']);
        $creds = ['email' => 'u@test.co', 'password' => 'Secreta123'];

        $phone = $this->postJson('/api/v1/auth/login', $creds + ['device_name' => 'pixel-8'])->json('data.access_token');
        $tablet = $this->postJson('/api/v1/auth/login', $creds + ['device_name' => 'tablet'])->json('data.access_token');

        $this->assertSame(2, $user->tokens()->count());
        $this->withToken($phone)->getJson('/api/v1/auth/me')->assertOk();
        $this->app['auth']->forgetGuards();
        $this->withToken($tablet)->getJson('/api/v1/auth/me')->assertOk();
    }

    public function test_same_device_name_replaces_only_its_own_token(): void
    {
        $user = User::factory()->create(['email' => 'u@test.co', 'password' => bcrypt('Secreta123'), 'role' => 'pasajero']);
        $creds = ['email' => 'u@test.co', 'password' => 'Secreta123'];

        $this->postJson('/api/v1/auth/login', $creds + ['device_name' => 'pixel-8']);
        $this->postJson('/api/v1/auth/login', $creds + ['device_name' => 'tablet']);
        $this->postJson('/api/v1/auth/login', $creds + ['device_name' => 'pixel-8']);

        $this->assertSame(['pixel-8', 'tablet'], $user->tokens()->orderBy('name')->pluck('name')->all());
    }

    public function test_default_device_name_and_validation(): void
    {
        $user = User::factory()->create(['email' => 'u@test.co', 'password' => bcrypt('Secreta123'), 'role' => 'pasajero']);
        $creds = ['email' => 'u@test.co', 'password' => 'Secreta123'];

        $this->postJson('/api/v1/auth/login', $creds)->assertOk();
        $this->assertSame(['mobile_app'], $user->tokens()->pluck('name')->all());

        $this->postJson('/api/v1/auth/login', $creds + ['device_name' => str_repeat('x', 61)])
            ->assertStatus(422)->assertJsonValidationErrors('device_name');
    }

    public function test_at_most_five_active_tokens_oldest_are_dropped(): void
    {
        $user = User::factory()->create(['role' => 'pasajero']);
        $svc = app(AuthService::class);

        foreach (range(1, 7) as $i) {
            $svc->generateToken($user, "dev-$i");
        }

        $this->assertSame(5, $user->tokens()->count());
        $this->assertSame(['dev-3', 'dev-4', 'dev-5', 'dev-6', 'dev-7'], $user->tokens()->orderBy('id')->pluck('name')->all());
    }

    public function test_register_and_google_also_use_per_device_tokens(): void
    {
        $this->postJson('/api/v1/auth/register', [
            'name' => 'Ana', 'email' => 'ana@gmail.com', 'password' => 'Secreta123', 'password_confirmation' => 'Secreta123',
            'device_name' => 'pixel-8',
        ])->assertOk();
        $this->assertSame(['pixel-8'], User::where('email', 'ana@gmail.com')->first()->tokens()->pluck('name')->all());

        $this->fakeGoogle(['email' => 'ana@gmail.com', 'sub' => 'g-9']);
        $this->google(['device_name' => 'tablet'])->assertOk();
        $this->assertSame(['pixel-8', 'tablet'], User::where('email', 'ana@gmail.com')->first()->tokens()->orderBy('name')->pluck('name')->all());
    }

    // ── (3) organization en /auth/google ─────────────────────────────

    public function test_google_creates_passenger_in_the_given_active_organization(): void
    {
        $t = Transportadora::create(['nombre' => 'Acme', 'slug' => 'acme']);
        $this->fakeGoogle();

        $this->google(['organization' => 'acme'])
            ->assertOk()
            ->assertJsonPath('data.user.organization_slug', 'acme');

        $this->assertSame($t->id, User::where('email', 'nuevo@gmail.com')->value('transportadora_id'));
        $this->assertSame('pasajero', User::where('email', 'nuevo@gmail.com')->value('role'));
    }

    public function test_google_with_unknown_or_inactive_organization_is_rejected_when_creating(): void
    {
        Transportadora::create(['nombre' => 'Off', 'slug' => 'off', 'activo' => false]);
        $this->fakeGoogle();

        $a = $this->google(['organization' => 'nope'])->assertStatus(422);
        $b = $this->google(['organization' => 'off'])->assertStatus(422);

        $this->assertSame($a->getContent(), $b->getContent());
        $this->assertDatabaseMissing('users', ['email' => 'nuevo@gmail.com']);
    }

    public function test_google_never_changes_the_organization_of_existing_users(): void
    {
        $a = Transportadora::create(['nombre' => 'A', 'slug' => 'a']);
        Transportadora::create(['nombre' => 'B', 'slug' => 'b']);
        $linked = User::factory()->create(['email' => 'linked@gmail.com', 'google_id' => 'g-100', 'role' => 'pasajero', 'transportadora_id' => $a->id]);
        $byEmail = User::factory()->create(['email' => 'byemail@gmail.com', 'role' => 'pasajero', 'transportadora_id' => null]);

        $this->fakeGoogle(['email' => 'linked@gmail.com']);
        $this->google(['organization' => 'b'])->assertOk()->assertJsonPath('data.user.organization_slug', 'a');
        $this->assertSame($a->id, $linked->fresh()->transportadora_id);

        // Con una org inexistente un usuario existente igualmente entra (se ignora).
        $this->google(['organization' => 'nope'])->assertOk();

        $this->fakeGoogle(['sub' => 'g-200', 'email' => 'byemail@gmail.com']);
        $this->google(['organization' => 'b'])->assertOk()->assertJsonPath('data.user.organization_slug', null);
        $this->assertNull($byEmail->fresh()->transportadora_id);
    }
}
