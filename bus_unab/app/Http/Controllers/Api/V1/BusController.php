<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\Bus;
use App\Services\GpsMobileService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class BusController extends BaseController
{
    public function __construct(private readonly GpsMobileService $gpsService) {}


    public function index(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'lat' => 'nullable|numeric|between:-90,90',
            'lng' => 'nullable|numeric|between:-180,180',
        ]);

        $externalBuses = $this->gpsService->getAllBuses(
            isset($validated['lat']) ? (float) $validated['lat'] : null,
            isset($validated['lng']) ? (float) $validated['lng'] : null
        );

        if ($externalBuses === null) {
            return $this->serviceUnavailable();
        }

        $localBuses = Bus::active()->get()->keyBy('plate');

        $buses = collect($externalBuses)
            ->filter(fn($v) => isset($localBuses[$v['Placa']]))
            ->map(fn($v) => $this->formatBusSummary($v, $localBuses[$v['Placa']]))
            ->values();

        return $this->success($buses);
    }

    /**
     * Retorna el detalle de una ruta específica.
     *
     * @urlParam plate string required  Placa del bus (RUTA1, RUTA2, RUTA3). Example: RUTA1
     */
    public function show(string $plate): JsonResponse
    {
        // Sanitizar: solo alfanumérico, máx 20 caracteres
        $plate = strtoupper(preg_replace('/[^A-Z0-9]/', '', $plate));

        if (empty($plate) || strlen($plate) > 20) {
            return $this->error('Identificador de ruta inválido', 422);
        }

        $bus = Bus::active()->where('plate', $plate)->first();

        if (! $bus) {
            return $this->notFound("La ruta '{$plate}' no existe o no está activa");
        }

        $detail = $this->gpsService->getBusDetail($bus->external_vehicle_id);

        if ($detail === null) {
            return $this->serviceUnavailable("No se pudo obtener el detalle de {$bus->name}");
        }

        return $this->success($this->formatBusDetail($detail, $bus));
    }

    /**
     * Retorna la polilínea de la ruta actual del bus hacia su destino.
     * Proxy a Google Directions API.
     *
     * @urlParam plate string required  Placa del bus. Example: RUTA1
     */
    public function route(string $plate): JsonResponse
    {
        $plate = strtoupper(preg_replace('/[^A-Z0-9]/', '', $plate));

        if (empty($plate) || strlen($plate) > 20) {
            return $this->error('Identificador de ruta inválido', 422);
        }

        $bus = Bus::active()->where('plate', $plate)->first();

        if (! $bus) {
            return $this->notFound("La ruta '{$plate}' no existe o no está activa");
        }

        $stops = $bus->stops;

        if ($stops->isEmpty()) {
            return $this->error('Esta ruta no tiene paradas asignadas', 400);
        }

        $firstStop  = $stops->first();
        $lastStop   = $stops->last();

        $originLat      = $firstStop->latitude;
        $originLng      = $firstStop->longitude;
        $destinationLat = $lastStop->latitude;
        $destinationLng = $lastStop->longitude;

        // Combina paradas + waypoints personalizados ordenados para trazar la ruta completa
        $stopCoords = $stops->map(fn($s) => [
            'order'  => $s->pivot->order * 100,
            'coords' => [$s->longitude, $s->latitude],
        ]);

        $waypointCoords = $bus->routeWaypoints->map(fn($w) => [
            'order'  => $w->order,
            'coords' => [$w->longitude, $w->latitude],
        ]);

        $coordinatePath = $stopCoords->concat($waypointCoords)
            ->sortBy('order')
            ->map(fn($p) => "{$p['coords'][0]},{$p['coords'][1]}")
            ->implode(';');

        $cacheKey = "bus_route_polyline_{$plate}";

        $polyline = Cache::remember($cacheKey, 86400, function () use ($coordinatePath, $plate) {
            try {
                $response = Http::timeout(15)->get(
                    "https://router.project-osrm.org/route/v1/driving/{$coordinatePath}",
                    ['overview' => 'full', 'geometries' => 'polyline']
                );

                if ($response->successful()) {
                    $data = $response->json();
                    if (($data['code'] ?? '') === 'Ok' && isset($data['routes'][0]['geometry'])) {
                        return $data['routes'][0]['geometry'];
                    }
                    Log::warning('BusController::route - OSRM code: ' . ($data['code'] ?? 'unknown'), ['plate' => $plate]);
                }
            } catch (\Exception $e) {
                Log::error('BusController::route - OSRM error: ' . $e->getMessage(), ['plate' => $plate]);
            }

            return null;
        });

        if (!$polyline) {
            return response()->json([
                'status'        => 'ERROR',
                'error_message' => 'No se pudo calcular la ruta.',
            ], 502);
        }

        return response()->json([
            'routes' => [[
                'overview_polyline' => ['points' => $polyline],
            ]],
            'status' => 'OK',
        ]);
    }

    // -------------------------------------------------------------------------
    // Transformadores privados
    // -------------------------------------------------------------------------

    private function formatBusSummary(array $external, Bus $local): array
    {
        return [
            'id'        => $local->id,
            'name'      => $local->name,
            'plate'     => $local->plate,
            'latitude'  => (float) $external['Latitud'],
            'longitude' => (float) $external['Longitud'],
            'heading'   => (int) ($external['Sentido'] ?? 0),
        ];
    }

    private function formatBusDetail(array $v, Bus $local): array
    {
        // Limpiar el campo address: quitar el ruido técnico del GPS
        // Formato: "Ciudad, BARRIO. Calle X con Y CSQ:3 DSleep:0 IN1:1..."
        $address = preg_replace('/\s+CSQ:\d+.*$/i', '', $v['Info'] ?? '');

        return [
            'id'              => $local->id,
            'name'            => $local->name,
            'plate'           => $local->plate,
            'latitude'        => (float) $v['Lt'],
            'longitude'       => (float) $v['Lg'],
            'speed_kmh'       => (int) ($v['Vel'] ?? 0),
            'heading'         => (int) ($v['Std'] ?? 0),
            'address'         => trim($address),
            'driver'          => $v['Cond'] ?: null,
            'last_event'      => $v['NEv'] ?? null,
            'last_updated_at' => $v['FdS'] ?? null,
        ];
    }
}
