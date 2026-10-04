<?php

namespace Tests\Feature;

use App\Filament\Tenant\Pages\BrandingPage;
use App\Models\Transportadora;
use App\Models\User;
use App\Services\BrandingService;
use Database\Seeders\DemoSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Livewire\Livewire;
use Tests\TestCase;

class BrandingPosesTest extends TestCase
{
    use RefreshDatabase;

    public function test_payload_is_backward_compatible_with_null_poses_and_default_bus_style(): void
    {
        Transportadora::create(['nombre' => 'Acme', 'slug' => 'acme']);

        $data = $this->getJson('/api/v1/branding/acme')->assertOk()->json('data');

        $this->assertSame(BrandingService::MASCOT_POSES, array_keys($data['mascot_poses']));
        $this->assertSame([], array_filter($data['mascot_poses']));
        $this->assertNull($data['mascot_url']);
        $this->assertSame(['body' => null, 'accent' => null, 'icon' => 'classic', 'icon_url' => null], $data['bus_style']);
    }

    public function test_poses_resolve_to_urls_and_greeting_is_the_mascot_fallback(): void
    {
        Transportadora::create([
            'nombre' => 'Acme', 'slug' => 'acme',
            'mascot_poses' => ['greeting' => 'branding/a/g.webp', 'sad' => 'branding/a/s.webp'],
            'bus_style' => ['body' => '#01265a', 'accent' => 'no-hex', 'icon' => 'minibus', 'icon_path' => 'branding/a/bus.png'],
        ]);

        $data = $this->getJson('/api/v1/branding/acme')->json('data');

        $this->assertStringEndsWith('branding/a/g.webp', $data['mascot_poses']['greeting']);
        $this->assertStringEndsWith('branding/a/s.webp', $data['mascot_poses']['sad']);
        $this->assertNull($data['mascot_poses']['ok']);
        $this->assertSame($data['mascot_poses']['greeting'], $data['mascot_url']);
        $this->assertSame('#01265A', $data['bus_style']['body']);
        $this->assertNull($data['bus_style']['accent']);
        $this->assertSame('minibus', $data['bus_style']['icon']);
        $this->assertStringEndsWith('branding/a/bus.png', $data['bus_style']['icon_url']);
    }

    public function test_explicit_single_mascot_wins_over_greeting(): void
    {
        Transportadora::create([
            'nombre' => 'Acme', 'slug' => 'acme', 'mascot_path' => 'branding/a/m.png',
            'mascot_poses' => ['greeting' => 'branding/a/g.webp'],
        ]);

        $this->assertStringEndsWith('m.png', $this->getJson('/api/v1/branding/acme')->json('data.mascot_url'));
    }

    public function test_changing_poses_or_bus_style_bumps_version_and_invalidates_cache(): void
    {
        $t = Transportadora::create(['nombre' => 'Acme', 'slug' => 'acme']);
        $this->getJson('/api/v1/branding/acme')->assertJsonPath('data.version', 1);

        $t->update(['bus_style' => ['icon' => 'modern']]);
        $this->getJson('/api/v1/branding/acme')->assertJsonPath('data.version', 2)->assertJsonPath('data.bus_style.icon', 'modern');

        $t->update(['mascot_poses' => ['ok' => 'branding/a/ok.webp']]);
        $this->assertSame(3, $this->getJson('/api/v1/branding/acme')->json('data.version'));
    }

    public function test_bus_style_validation(): void
    {
        $svc = new BrandingService;
        $branding = [
            'app_name' => 'X', 'font_family' => 'inter', 'corner_radius' => 'md',
            'features' => ['qr_payments' => true, 'wallet' => true, 'driver_mode' => true],
            'colors' => BrandingService::defaultColors(),
        ];

        $this->assertTrue($svc->validator($branding, ['body' => '#112233', 'accent' => null, 'icon' => 'modern'])->passes());

        $errors = $svc->validator($branding, ['body' => 'red', 'accent' => '#FFF', 'icon' => 'tank'])->errors();
        $this->assertTrue($errors->has('bus_style.body'));
        $this->assertTrue($errors->has('bus_style.accent'));
        $this->assertTrue($errors->has('bus_style.icon'));
    }

    public function test_demo_has_bucaratransit_with_nine_real_webp_poses_and_is_idempotent(): void
    {
        Storage::fake('public');
        $this->seed(DemoSeeder::class);
        $this->seed(DemoSeeder::class);

        $data = $this->getJson('/api/v1/branding/bucaratransit')->assertOk()->json('data');

        $this->assertSame('#01265A', $data['colors']['light']['primary']);
        $this->assertSame('#FCBB01', $data['colors']['light']['secondary']);
        $this->assertSame('poppins', $data['font_family']);
        $this->assertSame('lg', $data['corner_radius']);
        $this->assertSame(['qr_payments' => true, 'wallet' => true, 'driver_mode' => true], $data['features']);
        $this->assertCount(9, array_filter($data['mascot_poses']));
        $this->assertSame($data['mascot_poses']['greeting'], $data['mascot_url']);

        foreach (BrandingService::MASCOT_POSES as $pose) {
            $file = "branding/demo/bucaratransit/{$pose}.webp";
            Storage::disk('public')->assertExists($file);
            $this->assertSame('RIFF', substr(Storage::disk('public')->get($file), 0, 4), "$pose no es webp");
        }

        $this->assertSame(['classic', 'modern', 'minibus'], collect(['bucaratransit', 'metrobus', 'campus'])
            ->map(fn ($s) => $this->getJson("/api/v1/branding/$s")->json('data.bus_style.icon'))->all());
    }

    public function test_tenant_admin_saves_and_validates_bus_style(): void
    {
        $t = Transportadora::create(['nombre' => 'A', 'slug' => 'a']);
        $admin = User::factory()->create(['role' => 'tenant_admin', 'transportadora_id' => $t->id]);
        $branding = [
            'app_name' => 'A', 'font_family' => 'inter', 'corner_radius' => 'md',
            'features' => ['qr_payments' => true, 'wallet' => true, 'driver_mode' => true],
            'colors' => BrandingService::defaultColors(),
        ];

        Livewire::actingAs($admin)->test(BrandingPage::class)
            ->fillForm(['branding' => $branding, 'bus_style' => ['body' => 'red', 'icon' => 'classic']])
            ->call('save')
            ->assertHasErrors(['data.bus_style.body']);
        $this->assertNull($t->fresh()->bus_style);

        Livewire::actingAs($admin)->test(BrandingPage::class)
            ->fillForm(['branding' => $branding, 'bus_style' => ['body' => '#01265A', 'accent' => '#FCBB01', 'icon' => 'modern']])
            ->call('save')
            ->assertHasNoErrors();
        $this->assertSame('modern', $t->fresh()->bus_style['icon']);
    }
}
