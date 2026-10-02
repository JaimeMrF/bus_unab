<?php

namespace App\Support;

/**
 * Recorre una polilínea [[lat, lng], ...] de ida y vuelta (ping-pong) y
 * devuelve posición y rumbo (grados, 0 = norte) para una distancia recorrida.
 */
final class RouteWalker
{
    /** @var array<int, array{0: float, 1: float}> */
    private array $path;

    /** @var array<int, float> longitud de cada tramo en metros */
    private array $segments = [];

    private float $total = 0.0;

    /** @param array<int, array{0: float, 1: float}> $points mínimo 2 puntos */
    public function __construct(array $points)
    {
        $points = array_values($points);
        // Ida y vuelta sin repetir los extremos.
        $this->path = array_merge($points, array_reverse(array_slice($points, 1, -1)));
        if (count($points) >= 2) {
            $this->path[] = $points[0];
        }

        for ($i = 0; $i < count($this->path) - 1; $i++) {
            $len = self::distance($this->path[$i], $this->path[$i + 1]);
            $this->segments[] = $len;
            $this->total += $len;
        }
    }

    public function totalMeters(): float
    {
        return $this->total;
    }

    /** @return array{lat: float, lng: float, heading: int} */
    public function at(float $meters): array
    {
        if ($this->total <= 0.0) {
            return ['lat' => $this->path[0][0], 'lng' => $this->path[0][1], 'heading' => 0];
        }

        $remaining = fmod(fmod($meters, $this->total) + $this->total, $this->total);

        foreach ($this->segments as $i => $len) {
            if ($remaining <= $len) {
                $f = $len > 0 ? $remaining / $len : 0.0;
                [$a, $b] = [$this->path[$i], $this->path[$i + 1]];

                return [
                    'lat' => round($a[0] + ($b[0] - $a[0]) * $f, 7),
                    'lng' => round($a[1] + ($b[1] - $a[1]) * $f, 7),
                    'heading' => (int) round(self::bearing($a, $b)),
                ];
            }
            $remaining -= $len;
        }

        return ['lat' => $this->path[0][0], 'lng' => $this->path[0][1], 'heading' => 0];
    }

    /** Haversine en metros. */
    public static function distance(array $a, array $b): float
    {
        $dLat = deg2rad($b[0] - $a[0]);
        $dLng = deg2rad($b[1] - $a[1]);
        $h = sin($dLat / 2) ** 2 + cos(deg2rad($a[0])) * cos(deg2rad($b[0])) * sin($dLng / 2) ** 2;

        return 2 * 6371000 * asin(min(1.0, sqrt($h)));
    }

    /** Rumbo inicial de a hacia b en grados [0, 360). */
    public static function bearing(array $a, array $b): float
    {
        $lat1 = deg2rad($a[0]);
        $lat2 = deg2rad($b[0]);
        $dLng = deg2rad($b[1] - $a[1]);

        $y = sin($dLng) * cos($lat2);
        $x = cos($lat1) * sin($lat2) - sin($lat1) * cos($lat2) * cos($dLng);

        return fmod(rad2deg(atan2($y, $x)) + 360, 360);
    }
}
