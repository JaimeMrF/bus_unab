<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\PointOfInterest;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PointOfInterestController extends BaseController
{
    /**
     * Lista todos los puntos de interés activos.
     * Permite filtrar por categoría.
     *
     * GET /api/v1/poi
     * GET /api/v1/poi?category=campus
     */
    public function index(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'category' => 'nullable|in:campus,parking,food,health,transport,other',
        ]);

        $pois = PointOfInterest::active()
            ->when($validated['category'] ?? null, fn ($q, $cat) => $q->byCategory($cat))
            ->get(['id', 'name', 'description', 'latitude', 'longitude', 'category', 'icon']);

        return $this->success($pois);
    }
}
