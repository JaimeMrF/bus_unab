<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\Bus;
use App\Models\Stop;
use App\Services\EtaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class StopController extends BaseController
{
    /**
     * Lista todas las paradas activas.
     * GET /api/v1/stops
     */
    public function index(Request $request): JsonResponse
    {
        $stops = Stop::active()->get(['id', 'name', 'address', 'latitude', 'longitude', 'radius_meters'])
            ->map(fn ($s) => [
                'id' => $s->id,
                'name' => $s->name,
                'address' => $s->address ?? '',
                'latitude' => $s->latitude,
                'longitude' => $s->longitude,
                'radius_meters' => $s->radius_meters ?? 50,
            ]);

        return $this->withEtag($request, $this->success($stops));
    }

    /**
     * Lista las paradas de una ruta en orden.
     * GET /api/v1/buses/{plate}/stops
     */
    public function byBus(Request $request, string $plate, EtaService $etas): JsonResponse
    {
        // Sanitizar entrada
        $plate = strtoupper(preg_replace('/[^A-Z0-9]/', '', $plate));

        if (empty($plate) || strlen($plate) > 20) {
            return $this->error('Identificador de ruta inválido', 422);
        }

        $bus = Bus::active()->where('plate', $plate)->first();

        if (! $bus) {
            return $this->notFound("La ruta '{$plate}' no existe");
        }

        // ?eta=1 suma eta_seconds por parada (una sola lectura de posición y ruta).
        $withEta = $request->boolean('eta');
        $etaByStop = $withEta ? $etas->etaForStops($bus, $bus->stops) : [];

        $stops = $bus->stops->map(fn ($stop) => [
            ...($withEta ? ['eta_seconds' => $etaByStop[$stop->id]['eta_seconds'] ?? null] : []),
            'id' => $stop->id,
            'name' => $stop->name,
            'address' => $stop->address ?? '',
            'latitude' => $stop->latitude,
            'longitude' => $stop->longitude,
            'radius_meters' => $stop->radius_meters ?? 50,
            'order' => $stop->pivot->order,
            'estimated_minutes' => $stop->pivot->estimated_minutes ?? 0,
        ]);

        return $this->success($stops);
    }
}
