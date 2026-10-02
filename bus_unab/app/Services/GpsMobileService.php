<?php

namespace App\Services;

use App\Models\Bus;
use App\Support\DriverLocation;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class GpsMobileService
{
    private string $baseUrl;

    private int $codUserInc;

    private float $defaultLat;

    private float $defaultLng;

    private int $timeout;

    // Tiempo de caché en segundos (30 s para ubicaciones, 5 min para detalle)
    private const CACHE_BUSES_TTL = 30;

    private const CACHE_DETAIL_TTL = 30;

    public function __construct()
    {
        // Casts defensivos: sin GPSMOBILE_* en .env, config() puede devolver
        // string/null y rompe las propiedades estrictamente tipadas (TypeError 500).
        // Defaults coinciden con config/gpsmobile.php.
        $this->baseUrl = (string) config('gpsmobile.base_url', 'http://gpsmobile.co:4000');
        $this->codUserInc = (int) config('gpsmobile.cod_user_inc', 110571);
        $this->defaultLat = (float) config('gpsmobile.default_lat', 7.1218);
        $this->defaultLng = (float) config('gpsmobile.default_lng', -73.1158);
        $this->timeout = (int) config('gpsmobile.timeout', 10);
    }

    /**
     * Obtiene la ubicación en tiempo real de todos los buses.
     * Resultado cacheado 30 segundos.
     */
    public function getAllBuses(?float $lat = null, ?float $lng = null): ?array
    {
        $lat ??= $this->defaultLat;
        $lng ??= $this->defaultLng;

        $cacheKey = "gps_buses_{$this->codUserInc}";

        $vehicles = Cache::remember($cacheKey, self::CACHE_BUSES_TTL, function () use ($lat, $lng) {
            return $this->fetchAllBuses($lat, $lng);
        });

        return $this->overlaySimulatedBuses($vehicles);
    }

    /**
     * Obtiene el detalle de un bus específico.
     * Resultado cacheado 30 segundos.
     */
    public function getBusDetail(int $externalVehicleId): ?array
    {
        $cacheKey = "gps_detail_{$externalVehicleId}";

        $detail = Cache::remember($cacheKey, self::CACHE_DETAIL_TTL, function () use ($externalVehicleId) {
            return $this->fetchBusDetail($externalVehicleId);
        });

        return $this->overlaySimulatedDetail($externalVehicleId, $detail);
    }

    // -------------------------------------------------------------------------
    // Demo local: posiciones en vivo del simulador / teléfono (DriverLocation)
    // -------------------------------------------------------------------------

    private function usesLiveLocations(): bool
    {
        return app()->environment('local', 'testing');
    }

    /**
     * Solo local/testing: los buses con DriverLocation activa reemplazan (o
     * añaden) su entrada, aunque el GPS externo no responda. Sin ubicaciones
     * activas el resultado original (incluido null) no se toca.
     */
    private function overlaySimulatedBuses(?array $vehicles): ?array
    {
        if (! $this->usesLiveLocations()) {
            return $vehicles;
        }

        $live = [];
        foreach (Bus::active()->get(['plate']) as $bus) {
            if ($loc = DriverLocation::get($bus->plate)) {
                $live[$bus->plate] = $loc;
            }
        }

        if ($live === []) {
            return $vehicles;
        }

        $merged = collect($vehicles ?? [])->reject(fn ($v) => isset($live[$v['Placa'] ?? '']))->values()->all();

        foreach ($live as $plate => $loc) {
            $merged[] = [
                'Placa' => $plate,
                'Latitud' => $loc['latitude'],
                'Longitud' => $loc['longitude'],
                'Sentido' => $loc['heading'],
            ];
        }

        return $merged;
    }

    private function overlaySimulatedDetail(int $externalVehicleId, ?array $detail): ?array
    {
        if (! $this->usesLiveLocations()) {
            return $detail;
        }

        $plate = Bus::where('external_vehicle_id', $externalVehicleId)->value('plate');
        $loc = $plate ? DriverLocation::get($plate) : null;

        if (! $loc) {
            return $detail;
        }

        return array_merge($detail ?? ['Info' => '', 'Cond' => null, 'NEv' => null], [
            'Lt' => $loc['latitude'],
            'Lg' => $loc['longitude'],
            'Std' => $loc['heading'],
            'Vel' => $loc['speed_kmh'] ?? ($detail['Vel'] ?? 0),
            'FdS' => $loc['updated_at'],
        ]);
    }

    // -------------------------------------------------------------------------
    // Métodos privados — llamadas HTTP reales
    // -------------------------------------------------------------------------

    private function fetchAllBuses(float $lat, float $lng): ?array
    {
        try {
            $response = Http::timeout($this->timeout)
                ->get("{$this->baseUrl}/api/Home/{$this->codUserInc}/{$lat}/{$lng}");

            if (! $response->successful()) {
                Log::warning('GpsMobileService::getAllBuses - respuesta no exitosa', [
                    'status' => $response->status(),
                ]);

                return null;
            }

            $data = $response->json();

            if (! ($data['sucess'] ?? false)) {
                Log::warning('GpsMobileService::getAllBuses - sucess=false');

                return null;
            }

            $vehicles = $data['response']['veh'] ?? [];
            Log::info('GpsMobileService::getAllBuses - vehículos recibidos', ['vehicles' => $vehicles]);

            return $vehicles;

        } catch (ConnectionException $e) {
            Log::error('GpsMobileService::getAllBuses - error de conexión: '.$e->getMessage());

            return null;
        }
    }

    private function fetchBusDetail(int $externalVehicleId): ?array
    {
        try {
            $response = Http::timeout($this->timeout)
                ->get("{$this->baseUrl}/api/DetalleVehiculo/{$externalVehicleId}/{$this->codUserInc}");

            if (! $response->successful()) {
                Log::warning('GpsMobileService::getBusDetail - respuesta no exitosa', [
                    'vehicle_id' => $externalVehicleId,
                    'status' => $response->status(),
                ]);

                return null;
            }

            $data = $response->json();

            if (! ($data['sucess'] ?? false)) {
                Log::warning('GpsMobileService::getBusDetail - sucess=false');

                return null;
            }

            return $data['response']['veh'] ?? null;

        } catch (ConnectionException $e) {
            Log::error('GpsMobileService::getBusDetail - error de conexión: '.$e->getMessage());

            return null;
        }
    }
}
