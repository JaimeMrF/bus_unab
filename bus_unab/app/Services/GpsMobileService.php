<?php

namespace App\Services;

use Illuminate\Http\Client\ConnectionException;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class GpsMobileService
{
    private string $baseUrl;
    private int    $codUserInc;
    private float  $defaultLat;
    private float  $defaultLng;
    private int    $timeout;

    // Tiempo de caché en segundos (30 s para ubicaciones, 5 min para detalle)
    private const CACHE_BUSES_TTL  = 30;
    private const CACHE_DETAIL_TTL = 30;

    public function __construct()
    {
        $this->baseUrl    = config('gpsmobile.base_url');
        $this->codUserInc = config('gpsmobile.cod_user_inc');
        $this->defaultLat = config('gpsmobile.default_lat');
        $this->defaultLng = config('gpsmobile.default_lng');
        $this->timeout    = config('gpsmobile.timeout');
    }

    /**
     * Obtiene la ubicación en tiempo real de todos los buses.
     * Resultado cacheado 30 segundos.
     */
    public function getAllBuses(float $lat = null, float $lng = null): ?array
    {
        $lat ??= $this->defaultLat;
        $lng ??= $this->defaultLng;

        $cacheKey = "gps_buses_{$this->codUserInc}";

        return Cache::remember($cacheKey, self::CACHE_BUSES_TTL, function () use ($lat, $lng) {
            return $this->fetchAllBuses($lat, $lng);
        });
    }

    /**
     * Obtiene el detalle de un bus específico.
     * Resultado cacheado 30 segundos.
     */
    public function getBusDetail(int $externalVehicleId): ?array
    {
        $cacheKey = "gps_detail_{$externalVehicleId}";

        return Cache::remember($cacheKey, self::CACHE_DETAIL_TTL, function () use ($externalVehicleId) {
            return $this->fetchBusDetail($externalVehicleId);
        });
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
            Log::error('GpsMobileService::getAllBuses - error de conexión: ' . $e->getMessage());
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
                    'status'     => $response->status(),
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
            Log::error('GpsMobileService::getBusDetail - error de conexión: ' . $e->getMessage());
            return null;
        }
    }
}
