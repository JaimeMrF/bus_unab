<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\Bus;
use App\Models\Stop;
use App\Services\BusRequestService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class BusRequestController extends BaseController
{
    public function __construct(private readonly BusRequestService $service) {}

    /**
     * El usuario solicita un bus en una parada.
     * POST /api/v1/requests
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'bus_id'  => 'required|integer|exists:buses,id',
            'stop_id' => 'required|integer|exists:stops,id,is_active,1',
        ]);

        $bus  = Bus::find($validated['bus_id']);
        $stop = Stop::find($validated['stop_id']);

        if (! $bus->is_active) {
            return $this->error("El bus {$bus->name} no está en servicio actualmente");
        }

        // Verificar que la parada pertenece a la ruta del bus
        if (! $bus->stops()->where('stops.id', $stop->id)->exists()) {
            return $this->error("La parada '{$stop->name}' no pertenece a {$bus->name}");
        }

        $result = $this->service->request($request->user(), $bus, $stop);

        $message = $result['is_full']
            ? 'Solicitud registrada. El bus está lleno, puede que no haya espacio.'
            : 'Solicitud registrada. Te notificaremos cuando el bus llegue.';

        return $this->created($result, $message);
    }

    /**
     * El usuario cancela su solicitud activa.
     * DELETE /api/v1/requests/{bus}
     */
    public function cancel(Request $request, Bus $bus): JsonResponse
    {
        $cancelled = $this->service->cancel($request->user(), $bus);

        if (! $cancelled) {
            return $this->notFound('No tienes una solicitud activa para este bus');
        }

        return $this->success(null, 'Solicitud cancelada');
    }

    /**
     * Aforo actual de un bus.
     * GET /api/v1/buses/{plate}/occupancy
     */
    public function occupancy(string $plate): JsonResponse
    {
        $plate = strtoupper(preg_replace('/[^A-Z0-9]/', '', $plate));
        $bus   = Bus::active()->where('plate', $plate)->first();

        if (! $bus) {
            return $this->notFound("La ruta '{$plate}' no existe");
        }

        return $this->success($this->service->getOccupancy($bus));
    }

    /**
     * [Solo admin/driver] Marca la llegada del bus a una parada y notifica usuarios.
     * POST /api/v1/buses/{plate}/arrived
     *
     * Protegido por middleware 'role:admin,driver' en routes/api.php.
     */
    public function busArrived(Request $request, string $plate): JsonResponse
    {
        $validated = $request->validate([
            'stop_id' => 'required|integer|exists:stops,id',
        ]);

        $plate = strtoupper(preg_replace('/[^A-Z0-9]/', '', $plate));
        $bus   = Bus::active()->where('plate', $plate)->first();

        if (! $bus) {
            return $this->notFound("La ruta '{$plate}' no existe");
        }

        $stop     = Stop::findOrFail($validated['stop_id']);
        $notified = $this->service->markBusArrivedAtStop($bus, $stop);

        return $this->success([
            'notified_users' => $notified,
            'stop'           => $stop->name,
            'bus'            => $bus->name,
        ], "{$notified} usuario(s) notificados");
    }
}
