<?php

namespace Tests\Feature;

use App\Models\User;
use App\Services\AuthService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Http;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

/**
 * Controles de seguridad mínimos exigidos por el plan white-label.
 * Si alguno falla, es un hallazgo para backend (no se corrige en tests).
 */
class SecurityBaselineTest extends TestCase
{
    use RefreshDatabase;

    public function test_recharge_mock_is_blocked_in_production(): void
    {
        $user = User::factory()->create();
        Sanctum::actingAs($user);
        $this->app['env'] = 'production';

        $this->postJson('/api/v1/wallet/recharge-mock', ['monto_centavos' => 1000])
            ->assertForbidden();
    }

    public function test_recharge_mock_is_blocked_in_staging(): void
    {
        Sanctum::actingAs(User::factory()->create());
        $this->app['env'] = 'staging';

        $this->postJson('/api/v1/wallet/recharge-mock', ['monto_centavos' => 1000])
            ->assertForbidden();
    }

    public function test_sanctum_tokens_expire(): void
    {
        $this->assertNotNull(config('sanctum.expiration'), 'SANCTUM_TOKEN_EXPIRATION no debe ser null');
        $this->assertLessThanOrEqual(60 * 24 * 30, (int) config('sanctum.expiration'), 'Expiración > 30 días');
    }

    public function test_expired_sanctum_token_is_rejected(): void
    {
        $user = User::factory()->create();
        $token = $user->createToken('t');
        $user->tokens()->update(['created_at' => now()->subMinutes((int) config('sanctum.expiration') + 5)]);

        $this->withHeader('Authorization', 'Bearer '.$token->plainTextToken)
            ->getJson('/api/v1/wallet')
            ->assertUnauthorized();
    }

    /**
     * Regresión: un cliente que NO manda `Accept: application/json` (navegador,
     * monitor de uptime) provocaba un 500 — `Route [login] not defined` — porque el
     * middleware de auth evaluaba route('login') al construir la excepción. La API
     * debe contestar 401 siempre, con o sin esa cabecera.
     */
    public function test_api_responds_401_without_the_json_accept_header(): void
    {
        foreach (['/api/v1/stops', '/api/v1/buses', '/api/v1/auth/me'] as $path) {
            // get() manda Accept: text/html; getJson() manda application/json.
            $this->get($path)->assertStatus(401);
            $this->getJson($path)->assertStatus(401);
        }
    }

    public function test_cors_is_not_wildcard_with_credentials_or_open_origins(): void
    {
        $cors = config('cors');

        $this->assertNotNull($cors, 'Falta config/cors.php (CORS no configurado explícitamente)');
        $this->assertNotContains('*', $cors['allowed_origins'] ?? ['*'], 'allowed_origins no debe ser *');
    }

    public function test_cors_does_not_reflect_arbitrary_origin(): void
    {
        $res = $this->withHeaders(['Origin' => 'https://evil.test', 'Access-Control-Request-Method' => 'GET'])
            ->options('/api/v1/branding/acme');

        $allow = $res->headers->get('Access-Control-Allow-Origin');
        $this->assertNotSame('*', $allow);
        $this->assertNotSame('https://evil.test', $allow);
    }

    public function test_google_token_with_wrong_audience_is_rejected(): void
    {
        config(['services.google.client_id' => 'mine.apps.googleusercontent.com']);
        Http::fake(['oauth2.googleapis.com/*' => Http::response([
            'aud' => 'other.apps.googleusercontent.com', 'iss' => 'accounts.google.com',
            'sub' => '1', 'email' => 'a@b.test', 'exp' => time() + 600,
        ])]);

        $this->assertNull(app(AuthService::class)->verifyGoogleToken('x'));
    }

    public function test_google_token_with_wrong_issuer_is_rejected(): void
    {
        config(['services.google.client_id' => 'mine.apps.googleusercontent.com']);
        Http::fake(['oauth2.googleapis.com/*' => Http::response([
            'aud' => 'mine.apps.googleusercontent.com', 'iss' => 'https://evil.test',
            'sub' => '1', 'email' => 'a@b.test', 'exp' => time() + 600,
        ])]);

        $this->assertNull(app(AuthService::class)->verifyGoogleToken('x'), 'No se valida iss');
    }

    public function test_google_token_valid_is_accepted(): void
    {
        config(['services.google.client_id' => 'mine.apps.googleusercontent.com']);
        Http::fake(['oauth2.googleapis.com/*' => Http::response([
            'aud' => 'mine.apps.googleusercontent.com', 'iss' => 'https://accounts.google.com',
            'sub' => '1', 'email' => 'a@b.test', 'exp' => time() + 600,
        ])]);

        $this->assertNotNull(app(AuthService::class)->verifyGoogleToken('x'));
    }
}
