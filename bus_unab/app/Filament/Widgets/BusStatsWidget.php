<?php

namespace App\Filament\Widgets;

use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\Stop;
use App\Models\User;
use Filament\Widgets\StatsOverviewWidget as BaseWidget;
use Filament\Widgets\StatsOverviewWidget\Stat;

class BusStatsWidget extends BaseWidget
{
    protected static ?int $sort = 1;

    protected function getStats(): array
    {
        $totalPending = BusRequest::where('status', 'pending')->count();

        return [
            Stat::make('Buses Activos', Bus::where('is_active', true)->count())
                ->description('En servicio')
                ->descriptionIcon('heroicon-m-truck')
                ->color('success'),

            Stat::make('Usuarios Registrados', User::whereIn('role', ['pasajero', 'student'])->count())
                ->description('Pasajeros')
                ->descriptionIcon('heroicon-m-users')
                ->color('primary'),

            Stat::make('Solicitudes Activas', $totalPending)
                ->description('Usuarios esperando bus')
                ->descriptionIcon('heroicon-m-clock')
                ->color($totalPending > 0 ? 'warning' : 'gray'),

            Stat::make('Paradas Configuradas', Stop::where('is_active', true)->count())
                ->description('Paradas activas')
                ->descriptionIcon('heroicon-m-map-pin')
                ->color('info'),
        ];
    }
}
