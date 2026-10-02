<?php

namespace App\Filament\Tenant\Pages;

use App\Filament\Support\BrandingForm;
use App\Models\Concerns\TenantContext;
use App\Models\Transportadora;
use App\Services\BrandingService;
use Filament\Forms\Concerns\InteractsWithForms;
use Filament\Forms\Contracts\HasForms;
use Filament\Forms\Form;
use Filament\Notifications\Notification;
use Filament\Pages\Page;
use Illuminate\Support\Arr;
use Illuminate\Validation\ValidationException;

/**
 * White-label — el admin del tenant edita el branding de SU transportadora.
 *
 * El registro se resuelve SIEMPRE desde el usuario autenticado (nunca desde
 * input de la request), por lo que no se puede editar el branding de otro
 * tenant. Solo tenant_admin con transportadora asignada accede.
 */
class BrandingPage extends Page implements HasForms
{
    use InteractsWithForms;

    protected static ?string $navigationIcon = 'heroicon-o-paint-brush';

    protected static ?string $navigationLabel = 'Marca';

    protected static ?string $title = 'Marca de la app';

    protected static ?string $navigationGroup = 'Configuración';

    protected static ?string $slug = 'marca';

    protected static string $view = 'filament.tenant.pages.branding';

    /** @var array<string, mixed> */
    public ?array $data = [];

    public static function canAccess(): bool
    {
        return (bool) auth()->user()?->isTenantAdmin();
    }

    public function mount(): void
    {
        $t = $this->tenant();
        $b = $t->branding ?? [];

        $this->form->fill([
            'branding' => array_replace_recursive([
                'app_name' => $t->nombre,
                'font_family' => 'system',
                'corner_radius' => 'md',
                'features' => BrandingService::defaultFeatures(),
                'colors' => BrandingService::defaultColors(),
            ], $b),
            'logo_path' => $t->logo_path,
            'logo_dark_path' => $t->logo_dark_path,
            'icon_path' => $t->icon_path,
            'mascot_path' => $t->mascot_path,
        ]);
    }

    public function form(Form $form): Form
    {
        return $form->schema(BrandingForm::schema())->statePath('data');
    }

    public function save(): void
    {
        $state = $this->form->getState();
        $branding = (array) Arr::get($state, 'branding', []);

        $validator = (new BrandingService)->validator($branding);
        if ($validator->fails()) {
            throw ValidationException::withMessages(
                collect($validator->errors()->messages())
                    ->mapWithKeys(fn ($m, $k) => ['data.'.$k => $m])->all()
            );
        }

        $this->tenant()->update([
            'branding' => $branding,
            'logo_path' => $state['logo_path'] ?? null,
            'logo_dark_path' => $state['logo_dark_path'] ?? null,
            'icon_path' => $state['icon_path'] ?? null,
            'mascot_path' => $state['mascot_path'] ?? null,
        ]);

        Notification::make()->title('Marca actualizada')->success()->send();
    }

    private function tenant(): Transportadora
    {
        return Transportadora::findOrFail(TenantContext::id() ?? auth()->user()->transportadora_id);
    }
}
