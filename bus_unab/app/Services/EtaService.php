<?php

namespace App\Services;

use App\Models\Bus;
use App\Models\Stop;
use App\Support\DriverLocation;
use App\Support\RoutePath;
use App\Support\RouteWalker;
use Illuminate\Support\Facades\Cache;

/**
 * ETA de un bus a una parada, medido sobre la ruta (waypoints; si no hay,
 * las paradas ordenadas) y la velocidad reciente. Caché de 3 s por bus+parada.
 */
class EtaService
{
    public const CACHE_TTL = 3;

    /** Velocidad urbana de rescate (~29 km/h) si no hay lectura fiable. */
    public const FALLBACK_SPEED_MPS = 8.0;

    /** Por debajo de esto el bus está detenido: la velocidad no sirve para estimar. */
    private const MIN_MOVING_MPS = 1.0;

    private const FRESH_SECONDS = 15;

    public function __construct(private readonly GpsMobileService $gps) {}

    /**
     * @return array{eta_seconds: int, distance_m: int, speed_mps: float, confidence: string}|null null = sin posición del bus
     */
    public function eta(Bus $bus, Stop $stop): ?array
    {
        $position = $this->position($bus);

        if ($position === null) {
            return null;
        }

        return $this->compute($bus, $stop, $position);
    }

    /**
     * ETA a varias paradas con una sola lectura de posición y una sola ruta.
     *
     * @param  iterable<Stop>  $stops
     * @return array<int, array{eta_seconds: int, distance_m: int, speed_mps: float, confidence: string}|null> por stop_id
     */
    public function etaForStops(Bus $bus, iterable $stops): array
    {
        $position = $this->position($bus);
        $path = null;
        $out = [];

        foreach ($stops as $stop) {
            $out[$stop->id] = $position === null ? null : $this->compute($bus, $stop, $position, $path);
        }

        return $out;
    }

    private function compute(Bus $bus, Stop $stop, array $position, ?RoutePath &$path = null): array
    {
        $key = "eta:{$bus->id}:{$stop->id}";

        if (($hit = Cache::get($key)) !== null) {
            return $hit;
        }

        $path ??= $this->routePath($bus);

        $confidence = $position['fresh'] ? 'high' : 'medium';
        if ($path->usable()) {
            $distance = $path->distanceAlong($position['lat'], $position['lng'], $stop->latitude, $stop->longitude);
        } else {
            $distance = RouteWalker::distance([$position['lat'], $position['lng']], [$stop->latitude, $stop->longitude]);
            $confidence = 'low'; // línea recta: subestima
        }

        $speed = $position['speed_mps'] >= self::MIN_MOVING_MPS ? $position['speed_mps'] : self::FALLBACK_SPEED_MPS;
        if ($speed === self::FALLBACK_SPEED_MPS && $confidence === 'high') {
            $confidence = 'medium';
        }

        $result = [
            'eta_seconds' => (int) round($distance / $speed),
            'distance_m' => (int) round($distance),
            'speed_mps' => round($speed, 1),
            'confidence' => $confidence,
        ];

        Cache::put($key, $result, self::CACHE_TTL);

        return $result;
    }

    /** @return array{lat: float, lng: float, speed_mps: float, fresh: bool}|null */
    private function position(Bus $bus): ?array
    {
        // Teléfono del conductor / simulador: la fuente más fresca.
        if ($live = DriverLocation::get($bus->plate)) {
            $age = $live['updated_at'] ? now()->diffInSeconds($live['updated_at'], true) : PHP_INT_MAX;

            return [
                'lat' => $live['latitude'],
                'lng' => $live['longitude'],
                'speed_mps' => ($live['speed_kmh'] ?? 0) / 3.6,
                'fresh' => $age <= self::FRESH_SECONDS,
            ];
        }

        $detail = $this->gps->getBusDetail($bus->external_vehicle_id);

        if ($detail === null || ! isset($detail['Lt'], $detail['Lg'])) {
            return null;
        }

        return [
            'lat' => (float) $detail['Lt'],
            'lng' => (float) $detail['Lg'],
            'speed_mps' => ((float) ($detail['Vel'] ?? 0)) / 3.6,
            'fresh' => true,
        ];
    }

    private function routePath(Bus $bus): RoutePath
    {
        $waypoints = $bus->routeWaypoints()->get(['latitude', 'longitude']);

        if ($waypoints->count() >= 2) {
            return new RoutePath($waypoints->map(fn ($w) => [(float) $w->latitude, (float) $w->longitude])->all());
        }

        return new RoutePath($bus->stops->map(fn ($s) => [(float) $s->latitude, (float) $s->longitude])->all());
    }
}
