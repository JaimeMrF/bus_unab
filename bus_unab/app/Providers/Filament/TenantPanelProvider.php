<?php

namespace App\Providers\Filament;

use App\Filament\Resources\BusResource;
use App\Filament\Resources\StopResource;
use App\Filament\Resources\UserResource;
use App\Filament\Tenant\Resources\FareResource;
use App\Http\Middleware\EnsureTenantScope;
use Filament\Http\Middleware\Authenticate;
use Filament\Http\Middleware\AuthenticateSession;
use Filament\Http\Middleware\DisableBladeIconComponents;
use Filament\Http\Middleware\DispatchServingFilamentEvent;
use Filament\Pages;
use Filament\Panel;
use Filament\PanelProvider;
use Filament\Support\Colors\Color;
use Filament\Widgets;
use Illuminate\Cookie\Middleware\AddQueuedCookiesToResponse;
use Illuminate\Cookie\Middleware\EncryptCookies;
use Illuminate\Foundation\Http\Middleware\VerifyCsrfToken;
use Illuminate\Routing\Middleware\SubstituteBindings;
use Illuminate\Session\Middleware\StartSession;
use Illuminate\View\Middleware\ShareErrorsFromSession;

/**
 * M3 · S3.3.1 — Panel /empresa (tenant_admin de una Transportadora).
 *
 * Opción A del doc cacheado `.opencode/docs/filament-3-multitenancy.md`:
 * panel PLANO (sin ->tenant()/HasTenances, que exige N:N) + aislamiento duro
 * por datos vía GlobalTenantScope (BelongsToTenant) activado por el
 * middleware EnsureTenantScope al envolver la request en TenantContext::run().
 *
 * Decisiones clave:
 *  - SIN discoverResources: registro explícito. Así Transportadora/Wallet
 *    (CRUD Super Admin) NUNCA aparecen aquí, y Fare solo vive en este panel.
 *  - EnsureTenantScope va en authMiddleware DESPUÉS de Authenticate: en el
 *    stack base $request->user() aún sería null y el contexto no se activaría.
 *  - El gate de acceso es User::canAccessPanel('empresa') (rol tenant_admin
 *    con transportadora_id no nulo) — S3.3.1 ya entregado en el modelo.
 */
class TenantPanelProvider extends PanelProvider
{
    public function panel(Panel $panel): Panel
    {
        return $panel
            ->id('empresa')
            ->path('empresa')
            ->login()
            ->brandName('BUCARATRANSIT — Empresa')
            ->colors([
                'primary' => Color::Blue,
            ])
            ->pages([
                Pages\Dashboard::class,
            ])
            ->widgets([
                Widgets\AccountWidget::class,
            ])
            ->resources([
                BusResource::class,
                StopResource::class,
                FareResource::class,
                UserResource::class,
            ])
            ->middleware([
                EncryptCookies::class,
                AddQueuedCookiesToResponse::class,
                StartSession::class,
                AuthenticateSession::class,
                ShareErrorsFromSession::class,
                VerifyCsrfToken::class,
                SubstituteBindings::class,
                DisableBladeIconComponents::class,
                DispatchServingFilamentEvent::class,
            ])
            ->authMiddleware([
                Authenticate::class,
                // Activa TenantContext (TenantContext::run) para todo el panel:
                // los modelos con BelongsToTenant quedan filtrados y las
                // creaciones auto-asignan transportadora_id (hook creating).
                EnsureTenantScope::class,
            ]);
    }
}
