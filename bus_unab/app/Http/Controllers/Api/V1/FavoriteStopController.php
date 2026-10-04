<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\FavoriteStop;
use App\Models\Stop;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Paradas favoritas del usuario. El tenant se aplica con el middleware
 * tenant.scope (GlobalTenantScope sobre Stop): no se puede marcar ni listar
 * una parada de otra transportadora.
 */
class FavoriteStopController extends BaseController
{
    /** GET /api/v1/favorites/stops — campos justos, una sola consulta. */
    public function index(Request $request): JsonResponse
    {
        $stops = Stop::query()
            ->active()
            ->join('favorite_stops', 'favorite_stops.stop_id', '=', 'stops.id')
            ->where('favorite_stops.user_id', $request->user()->id)
            ->orderByDesc('favorite_stops.id')
            ->get(['stops.id', 'stops.name', 'stops.latitude', 'stops.longitude']);

        return $this->success($stops);
    }

    /** POST /api/v1/favorites/stops {stop_id} — idempotente. */
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate(['stop_id' => 'required|integer']);

        $stop = Stop::active()->find($data['stop_id']);

        if (! $stop) {
            return $this->notFound('Parada no encontrada');
        }

        $favorite = FavoriteStop::firstOrCreate(['user_id' => $request->user()->id, 'stop_id' => $stop->id]);

        $payload = ['stop_id' => $stop->id];

        return $favorite->wasRecentlyCreated
            ? $this->created($payload, 'Parada añadida a favoritos')
            : $this->success($payload, 'La parada ya era favorita');
    }

    /** DELETE /api/v1/favorites/stops/{stopId} — idempotente (204 exista o no). */
    public function destroy(Request $request, int $stopId): JsonResponse
    {
        FavoriteStop::where('user_id', $request->user()->id)->where('stop_id', $stopId)->delete();

        return $this->noContent();
    }
}
