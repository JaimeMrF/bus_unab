<?php

namespace Tests\Feature;

use App\Models\Transportadora;
use App\Services\BrandingService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\RateLimiter;
use Tests\TestCase;

/**
 * White-label — GET /api/v1/branding/{slug} (público, sin auth).
 */
class BrandingTest extends TestCase
{
    use RefreshDatabase;

    private function tenant(array $attrs = []): Transportadora
    {
        return Transportadora::create($attrs + [
            'nombre' => 'Acme Transit',
            'slug' => 'acme',
            'activo' => true,
        ]);
    }

    private function customBranding(): array
    {
        $colors = BrandingService::defaultColors();
        $colors['light']['primary'] = '#7c3aed';

        return [
            'app_name' => 'Acme Go',
            'tagline' => 'Muévete fácil',
            'support_email' => 'ayuda@acme.test',
            'font_family' => 'inter',
            'corner_radius' => 'lg',
            'features' => ['qr_payments' => false, 'wallet' => true, 'driver_mode' => false],
            'colors' => $colors,
        ];
    }

    public function test_returns_contract_structure_without_auth(): void
    {
        $this->tenant();

        $this->getJson('/api/v1/branding/acme')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['success', 'message', 'data' => [
                'slug', 'app_name', 'tagline', 'support_email',
                'logo_url', 'logo_dark_url', 'icon_url', 'mascot_url',
                'colors' => ['light' => BrandingService::COLOR_KEYS, 'dark' => BrandingService::COLOR_KEYS],
                'font_family', 'corner_radius',
                'features' => BrandingService::FEATURES,
                'version',
            ]])
            ->assertJsonPath('data.slug', 'acme')
            ->assertJsonPath('data.app_name', 'Acme Transit')
            ->assertJsonPath('data.font_family', 'system')
            ->assertJsonPath('data.corner_radius', 'md')
            ->assertJsonPath('data.version', 1);
    }

    public function test_defaults_are_neutral_valid_hex(): void
    {
        $this->tenant();

        $colors = $this->getJson('/api/v1/branding/acme')->json('data.colors');

        foreach (['light', 'dark'] as $mode) {
            $this->assertCount(11, $colors[$mode]);
            foreach ($colors[$mode] as $key => $hex) {
                $this->assertMatchesRegularExpression('/^#[0-9A-F]{6}$/', $hex, "$mode.$key");
            }
        }
    }

    public function test_applies_tenant_overrides_and_normalizes_hex(): void
    {
        $this->tenant(['branding' => $this->customBranding()]);

        $this->getJson('/api/v1/branding/acme')
            ->assertOk()
            ->assertJsonPath('data.app_name', 'Acme Go')
            ->assertJsonPath('data.tagline', 'Muévete fácil')
            ->assertJsonPath('data.support_email', 'ayuda@acme.test')
            ->assertJsonPath('data.font_family', 'inter')
            ->assertJsonPath('data.corner_radius', 'lg')
            ->assertJsonPath('data.colors.light.primary', '#7C3AED')
            ->assertJsonPath('data.features.qr_payments', false)
            ->assertJsonPath('data.features.driver_mode', false)
            ->assertJsonPath('data.features.wallet', true);
    }

    public function test_invalid_stored_values_fall_back_to_safe_defaults(): void
    {
        $this->tenant(['branding' => [
            'font_family' => 'comic-sans',
            'corner_radius' => 'xl',
            'colors' => ['light' => ['primary' => 'red', 'accent' => 'javascript:alert(1)']],
        ]]);

        $data = $this->getJson('/api/v1/branding/acme')->assertOk()->json('data');

        $defaults = BrandingService::defaultColors();
        $this->assertSame('system', $data['font_family']);
        $this->assertSame('md', $data['corner_radius']);
        $this->assertSame($defaults['light']['primary'], $data['colors']['light']['primary']);
        $this->assertSame($defaults['light']['accent'], $data['colors']['light']['accent']);
    }

    public function test_image_urls_are_absolute_https_never_local_paths(): void
    {
        config(['filesystems.disks.public.url' => 'https://cdn.example.test/storage']);
        $this->tenant(['logo_path' => 'branding/l.png', 'icon_path' => 'branding/i.png']);

        $data = $this->getJson('/api/v1/branding/acme')->json('data');

        $this->assertStringStartsWith('https://', $data['logo_url']);
        $this->assertStringStartsWith('https://', $data['icon_url']);
        $this->assertNull($data['logo_dark_url']);
        $this->assertNull($data['mascot_url']);
    }

    public function test_sends_etag_and_cache_control(): void
    {
        $this->tenant();

        $res = $this->getJson('/api/v1/branding/acme')->assertOk();

        $this->assertNotEmpty($res->headers->get('ETag'));
        $cc = (string) $res->headers->get('Cache-Control');
        $this->assertStringContainsString('public', $cc);
        $this->assertStringContainsString('max-age=300', $cc);
    }

    public function test_if_none_match_returns_304(): void
    {
        $this->tenant();
        $etag = $this->getJson('/api/v1/branding/acme')->headers->get('ETag');

        $this->getJson('/api/v1/branding/acme', ['If-None-Match' => $etag])
            ->assertStatus(304);
    }

    public function test_etag_and_version_change_after_branding_update(): void
    {
        $t = $this->tenant();
        $etag = $this->getJson('/api/v1/branding/acme')->headers->get('ETag');

        $t->update(['branding' => $this->customBranding()]);

        $res = $this->getJson('/api/v1/branding/acme', ['If-None-Match' => $etag])->assertOk();
        $this->assertNotSame($etag, $res->headers->get('ETag'));
        $res->assertJsonPath('data.version', 2);
    }

    public function test_version_not_bumped_by_non_visual_change(): void
    {
        $t = $this->tenant();
        $t->update(['contacto_nombre' => 'Otro']);

        $this->assertSame(1, $t->fresh()->branding_version);
    }

    public function test_unknown_slug_returns_generic_404(): void
    {
        $this->getJson('/api/v1/branding/no-existe')
            ->assertNotFound()
            ->assertJsonPath('success', false);
    }

    public function test_inactive_tenant_is_indistinguishable_from_unknown(): void
    {
        $this->tenant(['slug' => 'dormida', 'activo' => false]);

        $inactive = $this->getJson('/api/v1/branding/dormida');
        $unknown = $this->getJson('/api/v1/branding/fantasma');

        $inactive->assertNotFound();
        $this->assertSame($unknown->getContent(), $inactive->getContent());
    }

    public function test_soft_deleted_tenant_returns_404(): void
    {
        $this->tenant()->delete();

        $this->getJson('/api/v1/branding/acme')->assertNotFound();
    }

    public function test_malformed_slug_is_rejected(): void
    {
        $this->getJson('/api/v1/branding/'.str_repeat('a', 61))->assertNotFound();
        $this->getJson('/api/v1/branding/bad%20slug')->assertNotFound();
    }

    public function test_does_not_leak_other_tenants_or_internal_fields(): void
    {
        $this->tenant(['branding' => $this->customBranding(), 'nit' => '900123456-7', 'contacto_telefono' => '3000000000']);
        $this->tenant(['nombre' => 'Rival SAS', 'slug' => 'rival', 'branding' => ['app_name' => 'RivalApp']]);

        $body = $this->getJson('/api/v1/branding/acme')->assertOk()->getContent();

        $this->assertStringNotContainsString('RivalApp', $body);
        $this->assertStringNotContainsString('Rival SAS', $body);
        $this->assertStringNotContainsString('900123456-7', $body);
        $this->assertStringNotContainsString('3000000000', $body);
        $this->assertStringNotContainsString('_path', $body);
        $this->assertStringNotContainsString('"id"', $body);
    }

    public function test_each_tenant_gets_its_own_profile(): void
    {
        $this->tenant(['branding' => $this->customBranding()]);
        $this->tenant(['nombre' => 'Rival SAS', 'slug' => 'rival', 'branding' => ['app_name' => 'RivalApp']]);

        $this->getJson('/api/v1/branding/rival')
            ->assertOk()
            ->assertJsonPath('data.app_name', 'RivalApp')
            ->assertJsonPath('data.slug', 'rival');
    }

    public function test_throttle_60_per_minute(): void
    {
        $this->tenant();
        RateLimiter::clear('branding');

        for ($i = 0; $i < 60; $i++) {
            $this->getJson('/api/v1/branding/acme')->assertSuccessful();
        }

        $this->getJson('/api/v1/branding/acme')->assertStatus(429);
    }
}
