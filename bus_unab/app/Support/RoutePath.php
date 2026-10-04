<?php

namespace App\Support;

/**
 * Polilínea de una ruta [[lat, lng], ...] con proyección de puntos para medir
 * distancias A LO LARGO de la ruta (no en línea recta). La ruta se trata como
 * circuito: si el destino queda "detrás" del bus, se rodea por el cierre.
 */
final class RoutePath
{
    /** @var array<int, array{0: float, 1: float}> */
    private array $points;

    /** @var array<int, float> metros acumulados al inicio de cada punto */
    private array $cumulative = [0.0];

    private float $total = 0.0;

    /** @param array<int, array{0: float, 1: float}> $points */
    public function __construct(array $points)
    {
        $this->points = array_values($points);

        for ($i = 1; $i < count($this->points); $i++) {
            $this->total += RouteWalker::distance($this->points[$i - 1], $this->points[$i]);
            $this->cumulative[$i] = $this->total;
        }
    }

    public function usable(): bool
    {
        return count($this->points) >= 2 && $this->total > 0.0;
    }

    public function totalMeters(): float
    {
        return $this->total;
    }

    /**
     * Posición del punto a lo largo de la ruta y su desvío respecto a ella.
     *
     * @return array{along: float, off: float}
     */
    public function project(float $lat, float $lng): array
    {
        $best = ['along' => 0.0, 'off' => INF];
        $cosLat = cos(deg2rad($lat));

        for ($i = 0; $i < count($this->points) - 1; $i++) {
            [$aLat, $aLng] = $this->points[$i];
            [$bLat, $bLng] = $this->points[$i + 1];

            // Plano local en metros (suficiente a escala urbana).
            $ax = ($aLng - $lng) * 111320 * $cosLat;
            $ay = ($aLat - $lat) * 110540;
            $bx = ($bLng - $lng) * 111320 * $cosLat;
            $by = ($bLat - $lat) * 110540;

            $dx = $bx - $ax;
            $dy = $by - $ay;
            $len2 = $dx * $dx + $dy * $dy;
            $t = $len2 > 0 ? max(0.0, min(1.0, -($ax * $dx + $ay * $dy) / $len2)) : 0.0;

            $off = hypot($ax + $t * $dx, $ay + $t * $dy);

            if ($off < $best['off']) {
                $best = ['along' => $this->cumulative[$i] + $t * ($this->cumulative[$i + 1] - $this->cumulative[$i]), 'off' => $off];
            }
        }

        return $best;
    }

    /** Metros a recorrer del bus al destino siguiendo la ruta (circuito). */
    public function distanceAlong(float $fromLat, float $fromLng, float $toLat, float $toLng): float
    {
        $from = $this->project($fromLat, $fromLng)['along'];
        $to = $this->project($toLat, $toLng)['along'];

        return $to >= $from ? $to - $from : ($this->total - $from) + $to;
    }
}
