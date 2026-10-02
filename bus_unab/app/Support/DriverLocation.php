<?php

namespace App\Support;

use Illuminate\Support\Facades\Cache;

/**
 * Contrato único de la ubicación en vivo de un bus (teléfono del conductor o
 * simulador demo), guardada en Cache bajo `driver_location_{PLACA}`.
 *
 * Forma: {latitude: float, longitude: float, heading: int 0-360,
 *         speed_kmh: int|null, updated_at: ISO-8601}
 */
final class DriverLocation
{
    public const TTL_SECONDS = 60;

    public static function normalizePlate(string $plate): string
    {
        return strtoupper(preg_replace('/[^A-Z0-9]/i', '', $plate));
    }

    public static function key(string $plate): string
    {
        return 'driver_location_'.self::normalizePlate($plate);
    }

    public static function put(string $plate, float $latitude, float $longitude, int $heading, ?int $speedKmh = null): array
    {
        $location = [
            'latitude' => $latitude,
            'longitude' => $longitude,
            'heading' => $heading,
            'speed_kmh' => $speedKmh,
            'updated_at' => now()->toIso8601String(),
        ];

        Cache::put(self::key($plate), $location, self::TTL_SECONDS);

        return $location;
    }

    /** @return array{latitude: float, longitude: float, heading: int, speed_kmh: int|null, updated_at: string|null}|null */
    public static function get(string $plate): ?array
    {
        $raw = Cache::get(self::key($plate));

        if (! is_array($raw)) {
            return null;
        }

        // Tolera entradas con el formato anterior (lat/lng) aún presentes en caché.
        $lat = $raw['latitude'] ?? $raw['lat'] ?? null;
        $lng = $raw['longitude'] ?? $raw['lng'] ?? null;

        if ($lat === null || $lng === null) {
            return null;
        }

        return [
            'latitude' => (float) $lat,
            'longitude' => (float) $lng,
            'heading' => (int) ($raw['heading'] ?? 0),
            'speed_kmh' => isset($raw['speed_kmh']) ? (int) $raw['speed_kmh'] : null,
            'updated_at' => $raw['updated_at'] ?? null,
        ];
    }

    public static function forget(string $plate): void
    {
        Cache::forget(self::key($plate));
    }
}
