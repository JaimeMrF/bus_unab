<?php

namespace App\Filament\Resources\BusResource\Pages;

use App\Filament\Resources\BusResource;
use App\Models\Bus;
use App\Models\BusRouteWaypoint;
use App\Models\RouteStop;
use App\Models\Stop;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\Page;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;

class EditBusRoute extends Page
{
    protected static string $resource = BusResource::class;
    protected static string $view     = 'filament.resources.bus-resource.pages.edit-bus-route';

    public Bus $record;

    public function mount(int|string $record): void
    {
        $this->record = Bus::with([
            'routeStops.stop',
            'routeWaypoints' => fn ($q) => $q->orderBy('order'),
        ])->findOrFail($record);
    }

    public function getTitle(): string
    {
        return "Ruta: {$this->record->name}";
    }

    /**
     * Guarda paradas (en orden) y waypoints intermedios en una sola operación.
     *
     * @param array $routeStops  [{stop_id, order, estimated_minutes}]
     * @param array $waypoints   [{lat, lng, order, label}]
     */
    public function saveAll(array $routeStops, array $waypoints): void
    {
        DB::transaction(function () use ($routeStops, $waypoints) {
            // ── Paradas ────────────────────────────────────────────────────
            RouteStop::where('bus_id', $this->record->id)->delete();

            foreach ($routeStops as $rs) {
                RouteStop::create([
                    'bus_id'             => $this->record->id,
                    'stop_id'            => (int) $rs['stop_id'],
                    'order'              => (int) $rs['order'],
                    'estimated_minutes'  => (int) ($rs['estimated_minutes'] ?? 0),
                ]);
            }

            // ── Waypoints ──────────────────────────────────────────────────
            BusRouteWaypoint::where('bus_id', $this->record->id)->delete();

            foreach ($waypoints as $wp) {
                BusRouteWaypoint::create([
                    'bus_id'    => $this->record->id,
                    'order'     => (int)   ($wp['order'] ?? 999),
                    'latitude'  => (float) ($wp['lat']   ?? 0),
                    'longitude' => (float) ($wp['lng']   ?? 0),
                    'label'     => $wp['label'] ?? null,
                ]);
            }
        });

        Cache::forget("bus_route_polyline_{$this->record->plate}");

        Notification::make()
            ->title('Ruta guardada')
            ->body(count($routeStops) . ' paradas · ' . count($waypoints) . ' waypoints guardados.')
            ->success()
            ->send();
    }
}
