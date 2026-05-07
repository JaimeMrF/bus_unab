<?php

namespace App\Filament\Resources\BusResource\Pages;

use App\Filament\Resources\BusResource;
use App\Models\Bus;
use App\Models\BusRouteWaypoint;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\Page;
use Illuminate\Support\Facades\Cache;

class EditBusRoute extends Page
{
    protected static string $resource = BusResource::class;
    protected static string $view     = 'filament.resources.bus-resource.pages.edit-bus-route';

    public Bus $record;

    public function mount(int|string $record): void
    {
        $this->record = Bus::with([
            'stops'         => fn ($q) => $q->orderByPivot('order'),
            'routeWaypoints'=> fn ($q) => $q->orderBy('order'),
        ])->findOrFail($record);
    }

    public function getTitle(): string
    {
        return "Ruta: {$this->record->name}";
    }

    /**
     * Recibe el array de waypoints desde Alpine.js y los persiste.
     * Cada elemento: {lat, lng, order, label}
     */
    public function saveRoute(array $waypoints): void
    {
        BusRouteWaypoint::where('bus_id', $this->record->id)->delete();

        foreach ($waypoints as $wp) {
            BusRouteWaypoint::create([
                'bus_id'    => $this->record->id,
                'order'     => (int)   ($wp['order']  ?? 999),
                'latitude'  => (float) ($wp['lat']    ?? 0),
                'longitude' => (float) ($wp['lng']    ?? 0),
                'label'     => $wp['label'] ?: null,
            ]);
        }

        Cache::forget("bus_route_polyline_{$this->record->plate}");

        Notification::make()
            ->title('Ruta guardada')
            ->body(count($waypoints) . ' puntos de waypoint guardados. Caché invalidada.')
            ->success()
            ->send();
    }
}
