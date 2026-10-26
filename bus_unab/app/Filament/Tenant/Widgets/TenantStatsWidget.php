<?php

namespace App\Filament\Tenant\Widgets;

use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\Stop;
use App\Models\User;
use Filament\Widgets\StatsOverviewWidget as BaseWidget;
use Filament\Widgets\StatsOverviewWidget\Stat;

/**
 * Indicadores del escritorio /empresa. Filtra por la transportadora del usuario de
 * forma explícita: el polling de Livewire no pasa por EnsureTenantScope, así que no
 * se puede depender del GlobalTenantScope aquí.
 */
class TenantStatsWidget extends BaseWidget
{
    protected static ?int $sort = 1;

    protected function getStats(): array
    {
        $tenantId = auth()->user()?->transportadora_id;
        $busIds = Bus::withoutGlobalScopes()->where('transportadora_id', $tenantId)->pluck('id');
        $pending = BusRequest::whereIn('bus_id', $busIds)->where('status', 'pending')->count();

        return [
            Stat::make('Buses activos', Bus::withoutGlobalScopes()->where('transportadora_id', $tenantId)->where('is_active', true)->count())
                ->description('En servicio')
                ->descriptionIcon('heroicon-m-truck')
                ->color('success'),

            Stat::make('Pasajeros', User::withoutGlobalScopes()->where('transportadora_id', $tenantId)->where('role', 'pasajero')->count())
                ->description('Registrados en la app')
                ->descriptionIcon('heroicon-m-users')
                ->color('primary'),

            Stat::make('Solicitudes activas', $pending)
                ->description('Pasajeros esperando bus')
                ->descriptionIcon('heroicon-m-clock')
                ->color($pending > 0 ? 'warning' : 'gray'),

            Stat::make('Paradas', Stop::withoutGlobalScopes()->where('transportadora_id', $tenantId)->where('is_active', true)->count())
                ->description('Paradas activas')
                ->descriptionIcon('heroicon-m-map-pin')
                ->color('info'),
        ];
    }
}
