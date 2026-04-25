<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\Stop;
use App\Services\GpsMobileService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class BusMapController extends Controller
{
    public function __construct(private readonly GpsMobileService $gpsService) {}

    /**
     * Devuelve el snapshot de todos los buses + paradas para el mapa del admin.
     *
     * GET /admin/map-data
     * Protegido: solo usuarios autenticados con rol admin (validado manualmente).
     */
    public function data(Request $request): JsonResponse
    {
        // Doble validación de seguridad — el middleware web + sesión Filament
        $user = auth()->user();

        if (! $user || ! $user->isAdmin()) {
            abort(403);
        }

        $buses = Bus::active()->get();

        $busData = $buses->map(function (Bus $bus) {
            $detail  = $this->gpsService->getBusDetail($bus->external_vehicle_id);
            $pending = BusRequest::where('bus_id', $bus->id)
                ->where('status', 'pending')
                ->count();

            $pct   = $bus->capacity > 0
                ? round(min(($pending / $bus->capacity) * 100, 100), 1)
                : 0.0;

            $level = match (true) {
                $pct >= 90 => 'full',
                $pct >= 60 => 'high',
                $pct >= 30 => 'medium',
                default    => 'low',
            };

            $gps = null;
            if ($detail) {
                $rawAddress = $detail['Info'] ?? '';
                $address    = trim(preg_replace('/\s+CSQ:\d+.*$/i', '', $rawAddress));

                $gps = [
                    'latitude'        => (float) ($detail['Lt']  ?? 0),
                    'longitude'       => (float) ($detail['Lg']  ?? 0),
                    'speed_kmh'       => (int)   ($detail['Vel'] ?? 0),
                    'heading'         => (int)   ($detail['Std'] ?? 0),
                    'address'         => $address ?: null,
                    'driver'          => ($detail['Cond'] ?? null) ?: null,
                    'last_event'      => $detail['NEv'] ?? null,
                    'last_updated_at' => $detail['FdS'] ?? null,
                    'online'          => true,
                ];
            }

            return [
                'id'       => $bus->id,
                'name'     => $bus->name,
                'plate'    => $bus->plate,
                'capacity' => $bus->capacity,
                'occupancy' => [
                    'count'      => $pending,
                    'percentage' => $pct,
                    'level'      => $level,
                ],
                'gps' => $gps ?? ['online' => false],
            ];
        });

        $stops = Stop::where('is_active', true)
            ->get(['id', 'name', 'address', 'latitude', 'longitude', 'radius_meters']);

        return response()->json([
            'buses'      => $busData,
            'stops'      => $stops,
            'fetched_at' => now()->toIso8601String(),
        ]);
    }
}
