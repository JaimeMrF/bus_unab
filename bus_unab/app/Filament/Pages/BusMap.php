<?php

namespace App\Filament\Pages;

use App\Models\Bus;
use App\Models\Stop;
use Filament\Pages\Page;
use Filament\Support\Enums\MaxWidth;

class BusMap extends Page
{
    protected static ?string $navigationIcon  = 'heroicon-o-map';
    protected static ?string $navigationLabel = 'Mapa en Vivo';
    protected static ?string $title           = 'Mapa de Buses en Tiempo Real';
    protected static ?string $slug            = 'bus-map';
    protected static ?int    $navigationSort  = 0; // Primero en el menú

    protected static string $view = 'filament.pages.bus-map';

    // Ancho máximo: full para que el mapa aproveche todo el espacio
    public function getMaxContentWidth(): MaxWidth
    {
        return MaxWidth::Full;
    }

    /**
     * Datos iniciales que se inyectan en la vista para el primer render
     * (evita el parpadeo del mapa en la primera carga).
     */
    public function getViewData(): array
    {
        return [
            'busCount'  => Bus::active()->count(),
            'stopCount' => Stop::where('is_active', true)->count(),
            // URL del endpoint AJAX — generada desde PHP para no hardcodear en JS
            'mapDataUrl' => route('admin.map-data'),
        ];
    }
}
