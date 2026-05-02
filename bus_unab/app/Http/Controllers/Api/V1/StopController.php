<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\Bus;
use App\Models\Stop;
use Illuminate\Http\JsonResponse;

class StopController extends BaseController
{
    /**
     * Lista todas las paradas activas.
     * GET /api/v1/stops
     */
    public function index(): JsonResponse
    {
        $stops = Stop::active()->get(['id', 'name', 'address', 'latitude', 'longitude', 'radius_meters'])
            ->map(fn ($s) => [
                'id'            => $s->id,
                'name'          => $s->name,
                'address'       => $s->address ?? '',
                'latitude'      => $s->latitude,
                'longitude'     => $s->longitude,
                'radius_meters' => $s->radius_meters ?? 50,
            ]);

        return $this->success($stops);
    }

    /**
     * Lista las paradas de una ruta en orden.
     * GET /api/v1/buses/{plate}/stops
     */
    public function byBus(string $plate): JsonResponse
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

        $stops = $bus->stops->map(fn ($stop) => [
            'id'                => $stop->id,
            'name'              => $stop->name,
            'address'           => $stop->address ?? '',
            'latitude'          => $stop->latitude,
            'longitude'         => $stop->longitude,
            'radius_meters'     => $stop->radius_meters ?? 50,
            'order'             => $stop->pivot->order,
            'estimated_minutes' => $stop->pivot->estimated_minutes ?? 0,
        ]);

        return $this->success($stops);
    }
}
