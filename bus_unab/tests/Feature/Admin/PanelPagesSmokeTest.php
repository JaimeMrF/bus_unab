<?php

namespace Tests\Feature\Admin;

use App\Models\User;
use App\Services\GpsMobileService;
use Database\Seeders\DemoSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

/**
 * Abre cada pantalla de los paneles /admin y /empresa con datos demo.
 * Atrapa errores de render (íconos inexistentes, columnas mal nombradas) que
 * solo aparecen al pintar la tabla con registros.
 */
class PanelPagesSmokeTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        Storage::fake('public');
        // Sin red en pruebas: el GPS no devuelve posición.
        $this->mock(GpsMobileService::class, function ($m) {
            $m->shouldReceive('getBusDetail')->andReturn(null);
            $m->shouldIgnoreMissing([]);
        });
        $this->seed(DemoSeeder::class);
    }

    public function test_every_admin_page_renders_for_super_admin(): void
    {
        $this->assertPanelRenders('admin', User::where('email', DemoSeeder::SUPERADMIN_EMAIL)->firstOrFail());
    }

    public function test_every_empresa_page_renders_for_tenant_admin(): void
    {
        $this->assertPanelRenders('empresa', User::where('email', 'admin.bucaratransit@demo.test')->firstOrFail());
    }

    private function assertPanelRenders(string $panel, User $user): void
    {
        $failures = [];
        $checked = 0;

        foreach (Route::getRoutes() as $route) {
            $name = (string) $route->getName();
            if (! str_starts_with($name, "filament.{$panel}.") || str_contains($name, '.auth.')
                || ! in_array('GET', $route->methods(), true)) {
                continue;
            }

            $params = [];
            if (in_array('record', $route->parameterNames(), true)) {
                $resource = collect(\Filament\Facades\Filament::getPanel($panel)->getResources())
                    ->first(fn ($r) => str_starts_with($name, "filament.{$panel}.resources.".$r::getSlug().'.'));
                $model = $resource ? $resource::getEloquentQuery()->getModel() : null;
                $record = $model ? $resource::getEloquentQuery()->when($panel === 'empresa' && $model->isFillable('transportadora_id'),
                    fn ($q) => $q->where('transportadora_id', $user->transportadora_id))->first() : null;
                if (! $record) {
                    continue;
                }
                $params['record'] = $record->getKey();
            }

            $status = $this->actingAs($user)->get(route($name, $params))->getStatusCode();
            $checked++;
            if ($status !== 200) {
                $failures[] = "{$name} → {$status}";
            }
        }

        $this->assertGreaterThan(5, $checked);
        $this->assertSame([], $failures, implode("\n", $failures));
    }
}
