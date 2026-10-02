<?php

namespace Tests\Feature;

use App\Filament\Tenant\Pages\BrandingPage;
use App\Models\Transportadora;
use App\Models\User;
use App\Services\BrandingService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Livewire\Livewire;
use Tests\TestCase;

/**
 * White-label — edición de branding en el panel /empresa (Filament).
 */
class BrandingPageTest extends TestCase
{
    use RefreshDatabase;

    private Transportadora $a;

    private Transportadora $b;

    private User $adminA;

    protected function setUp(): void
    {
        parent::setUp();

        $this->a = Transportadora::create(['nombre' => 'A', 'slug' => 'a']);
        $this->b = Transportadora::create(['nombre' => 'B', 'slug' => 'b', 'branding' => ['app_name' => 'Marca B']]);
        $this->adminA = $this->user('tenant_admin', $this->a->id);
    }

    private function user(string $role, ?int $tenantId): User
    {
        static $n = 0;

        return User::factory()->create([
            'email' => 'u'.++$n.'@test.co',
            'role' => $role,
            'transportadora_id' => $tenantId,
        ]);
    }

    private function branding(array $override = []): array
    {
        return array_replace_recursive([
            'app_name' => 'Mi Marca',
            'font_family' => 'inter',
            'corner_radius' => 'lg',
            'features' => ['qr_payments' => true, 'wallet' => false, 'driver_mode' => true],
            'colors' => BrandingService::defaultColors(),
        ], $override);
    }

    public function test_tenant_admin_saves_branding_and_version_increments(): void
    {
        Livewire::actingAs($this->adminA)
            ->test(BrandingPage::class)
            ->fillForm(['branding' => $this->branding()])
            ->call('save')
            ->assertHasNoFormErrors();

        $a = $this->a->fresh();
        $this->assertSame('Mi Marca', $a->branding['app_name']);
        $this->assertSame(2, $a->branding_version);
    }

    public function test_low_contrast_is_rejected_and_nothing_is_saved(): void
    {
        Livewire::actingAs($this->adminA)
            ->test(BrandingPage::class)
            ->fillForm(['branding' => $this->branding(['colors' => ['light' => ['primary' => '#FFFF00', 'on_primary' => '#FFFFFF']]])])
            ->call('save')
            ->assertHasFormErrors(['branding.colors.light.on_primary']);

        $this->assertNull($this->a->fresh()->branding);
    }

    public function test_save_only_touches_own_tenant(): void
    {
        Livewire::actingAs($this->adminA)
            ->test(BrandingPage::class)
            ->fillForm(['branding' => $this->branding()])
            ->call('save');

        $b = $this->b->fresh();
        $this->assertSame('Marca B', $b->branding['app_name']);
        $this->assertSame(1, $b->branding_version);
    }

    public function test_only_tenant_admins_can_access(): void
    {
        foreach (['pasajero', 'driver'] as $role) {
            $this->actingAs($this->user($role, $this->a->id));
            $this->assertFalse(BrandingPage::canAccess(), $role);
        }

        $this->actingAs($this->user('admin', null));
        $this->assertFalse(BrandingPage::canAccess(), 'super admin usa el CRUD de Transportadoras');

        $this->actingAs($this->adminA);
        $this->assertTrue(BrandingPage::canAccess());
    }
}
