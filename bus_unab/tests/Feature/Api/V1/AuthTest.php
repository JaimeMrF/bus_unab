<?php

namespace Tests\Feature\Api\V1;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class AuthTest extends TestCase
{
    use RefreshDatabase;

    public function test_login_with_valid_credentials(): void
    {
        User::factory()->create([
            'email' => 'test@unab.edu.co',
            'password' => 'password',
        ]);

        $response = $this->postJson('/api/v1/auth/login', [
            'email'    => 'test@unab.edu.co',
            'password' => 'password',
        ]);

        $response->assertStatus(200)
            ->assertJsonStructure([
                'success', 'message',
                'data' => ['access_token', 'token_type', 'user'],
            ])
            ->assertJson(['success' => true]);
    }

    public function test_login_with_invalid_password(): void
    {
        User::factory()->create(['email' => 'test@unab.edu.co']);

        $this->postJson('/api/v1/auth/login', [
            'email'    => 'test@unab.edu.co',
            'password' => 'wrong',
        ])->assertStatus(401)->assertJson(['success' => false]);
    }

    public function test_login_with_nonexistent_email(): void
    {
        $this->postJson('/api/v1/auth/login', [
            'email'    => 'nobody@unab.edu.co',
            'password' => 'password',
        ])->assertStatus(401);
    }

    public function test_login_validates_required_fields(): void
    {
        $this->postJson('/api/v1/auth/login', [])
            ->assertStatus(422);
    }

    public function test_me_returns_authenticated_user(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/auth/me')
            ->assertStatus(200)
            ->assertJson([
                'success' => true,
                'data'    => [
                    'id'    => $user->id,
                    'email' => $user->email,
                    'role'  => $user->role,
                ],
            ]);
    }

    public function test_me_requires_authentication(): void
    {
        $this->getJson('/api/v1/auth/me')->assertStatus(401);
    }

    public function test_logout_succeeds(): void
    {
        $user  = User::factory()->create();
        $token = $user->createToken('mobile_app')->plainTextToken;

        $this->withHeader('Authorization', "Bearer {$token}")
            ->postJson('/api/v1/auth/logout')
            ->assertStatus(200)
            ->assertJson(['success' => true]);
    }

    public function test_google_login_fails_with_invalid_token(): void
    {
        config(['services.google.client_id' => 'test-client.apps.googleusercontent.com']);

        Http::fake([
            'oauth2.googleapis.com/*' => Http::response(['error' => 'invalid_token'], 400),
        ]);

        $this->postJson('/api/v1/auth/google', ['id_token' => 'bad-token'])
            ->assertStatus(401)
            ->assertJson(['success' => false]);
    }

    public function test_google_login_creates_new_user(): void
    {
        config(['services.google.client_id' => 'test-client.apps.googleusercontent.com']);

        Http::fake([
            'oauth2.googleapis.com/*' => Http::response([
                'sub'     => 'google-sub-123',
                'email'   => 'alumno@unab.edu.co',
                'name'    => 'Alumno Test',
                'picture' => 'https://example.com/photo.jpg',
                'aud'     => 'test-client.apps.googleusercontent.com',
                'exp'     => time() + 3600,
            ], 200),
        ]);

        $response = $this->postJson('/api/v1/auth/google', ['id_token' => 'valid-token']);

        $response->assertStatus(200)
            ->assertJsonStructure(['data' => ['access_token', 'user']]);

        $this->assertDatabaseHas('users', [
            'email'     => 'alumno@unab.edu.co',
            'google_id' => 'google-sub-123',
            'role'      => 'student',
        ]);
    }

    public function test_google_login_links_existing_user(): void
    {
        config(['services.google.client_id' => 'test-client.apps.googleusercontent.com']);

        $existing = User::factory()->create([
            'email'     => 'admin@unab.edu.co',
            'role'      => 'admin',
            'google_id' => null,
        ]);

        Http::fake([
            'oauth2.googleapis.com/*' => Http::response([
                'sub'     => 'google-admin-999',
                'email'   => 'admin@unab.edu.co',
                'name'    => 'Admin UNAB',
                'picture' => null,
                'aud'     => 'test-client.apps.googleusercontent.com',
                'exp'     => time() + 3600,
            ], 200),
        ]);

        $this->postJson('/api/v1/auth/google', ['id_token' => 'valid-token'])
            ->assertStatus(200);

        $this->assertDatabaseHas('users', [
            'id'        => $existing->id,
            'google_id' => 'google-admin-999',
            'role'      => 'admin',
        ]);

        // No debe crear un usuario duplicado
        $this->assertDatabaseCount('users', 1);
    }

    public function test_google_login_rejects_wrong_audience(): void
    {
        config(['services.google.client_id' => 'test-client.apps.googleusercontent.com']);

        Http::fake([
            'oauth2.googleapis.com/*' => Http::response([
                'sub' => 'google-123',
                'aud' => 'other-app.apps.googleusercontent.com',
                'exp' => time() + 3600,
            ], 200),
        ]);

        $this->postJson('/api/v1/auth/google', ['id_token' => 'token'])
            ->assertStatus(401);
    }
}
