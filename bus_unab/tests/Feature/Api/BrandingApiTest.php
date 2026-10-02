<?php

namespace Tests\Feature\Api;

use App\Models\Transportadora;
use App\Services\BrandingService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class BrandingApiTest extends TestCase
{
    use RefreshDatabase;

    private function tenant(array $overrides = []): Transportadora
    {
        return Transportadora::create(array_merge([
            'nombre' => 'Metro Demo',
            'slug' => 'metro-demo',
            'contacto_email' => 'hola@metro.test',
        ], $overrides));
    }

    public function test_returns_contract_with_neutral_defaults(): void
    {
        $this->tenant();

        $res = $this->getJson('/api/v1/branding/metro-demo')->assertOk();

        $res->assertJsonPath('success', true)
            ->assertJsonPath('data.slug', 'metro-demo')
            ->assertJsonPath('data.app_name', 'Metro Demo')
            ->assertJsonPath('data.support_email', 'hola@metro.test')
            ->assertJsonPath('data.tagline', null)
            ->assertJsonPath('data.logo_url', null)
            ->assertJsonPath('data.font_family', 'system')
            ->assertJsonPath('data.corner_radius', 'md')
            ->assertJsonPath('data.features.wallet', true)
            ->assertJsonPath('data.version', 1);

        foreach (['light', 'dark'] as $mode) {
            $this->assertEqualsCanonicalizing(
                BrandingService::COLOR_KEYS,
                array_keys($res->json("data.colors.$mode")),
            );
        }
    }

    public function test_applies_tenant_overrides_and_ignores_invalid_values(): void
    {
        $this->tenant([
            'logo_path' => 'branding/1/logo.png',
            'branding' => [
                'app_name' => 'Mi Bus',
                'tagline' => 'Viaja mejor',
                'font_family' => 'comic-sans',
                'corner_radius' => 'lg',
                'features' => ['wallet' => false],
                'colors' => ['light' => ['primary' => '#112233', 'accent' => 'not-a-color']],
            ],
        ]);

        $data = $this->getJson('/api/v1/branding/metro-demo')->assertOk()->json('data');

        $this->assertSame('Mi Bus', $data['app_name']);
        $this->assertSame('Viaja mejor', $data['tagline']);
        $this->assertSame('system', $data['font_family']);
        $this->assertSame('lg', $data['corner_radius']);
        $this->assertFalse($data['features']['wallet']);
        $this->assertTrue($data['features']['qr_payments']);
        $this->assertSame('#112233', $data['colors']['light']['primary']);
        $this->assertSame('#0EA5E9', $data['colors']['light']['accent']);
        $this->assertStringStartsWith('http', $data['logo_url']);
        $this->assertStringEndsWith('branding/1/logo.png', $data['logo_url']);
    }

    public function test_unknown_and_inactive_tenants_get_identical_404(): void
    {
        $this->tenant(['slug' => 'apagada', 'activo' => false]);

        $missing = $this->getJson('/api/v1/branding/no-existe')->assertNotFound();
        $inactive = $this->getJson('/api/v1/branding/apagada')->assertNotFound();

        $this->assertSame($missing->getContent(), $inactive->getContent());
    }

    public function test_is_public_and_sends_cache_headers(): void
    {
        $this->tenant();

        $res = $this->getJson('/api/v1/branding/metro-demo')->assertOk();

        $this->assertStringContainsString('public', $res->headers->get('Cache-Control'));
        $this->assertStringContainsString('max-age=300', $res->headers->get('Cache-Control'));
        $this->assertNotEmpty($res->headers->get('ETag'));
    }

    public function test_conditional_request_returns_304(): void
    {
        $this->tenant();

        $etag = $this->getJson('/api/v1/branding/metro-demo')->headers->get('ETag');

        $this->getJson('/api/v1/branding/metro-demo', ['If-None-Match' => $etag])
            ->assertStatus(304);
    }

    public function test_version_increments_on_branding_change_and_changes_etag(): void
    {
        $t = $this->tenant();
        $etag = $this->getJson('/api/v1/branding/metro-demo')->headers->get('ETag');

        $t->update(['branding' => ['app_name' => 'Nuevo']]);

        $res = $this->getJson('/api/v1/branding/metro-demo', ['If-None-Match' => $etag])->assertOk();
        $this->assertSame(2, $res->json('data.version'));
        $this->assertNotSame($etag, $res->headers->get('ETag'));

        $t->update(['plan' => 'pro']); // cambio no visual
        $this->assertSame(2, $t->fresh()->branding_version);
    }

    public function test_tenant_payloads_are_isolated(): void
    {
        $this->tenant(['slug' => 'a', 'nombre' => 'A', 'branding' => ['app_name' => 'Marca A']]);
        $this->tenant(['slug' => 'b', 'nombre' => 'B', 'branding' => ['app_name' => 'Marca B']]);

        $this->getJson('/api/v1/branding/a')->assertJsonPath('data.app_name', 'Marca A');
        $this->getJson('/api/v1/branding/b')->assertJsonPath('data.app_name', 'Marca B');
    }

    public function test_is_rate_limited(): void
    {
        $this->tenant();

        for ($i = 0; $i < 60; $i++) {
            $this->getJson('/api/v1/branding/metro-demo')->assertOk();
        }

        $this->getJson('/api/v1/branding/metro-demo')->assertStatus(429);
    }
}
