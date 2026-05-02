<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\BusRequest;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class QrController extends BaseController
{
    /**
     * Valida el QR de un estudiante y lo marca como abordado.
     * POST /api/v1/qr/validate
     * Solo conductores y admins.
     */
    public function validate(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'request_id' => 'required|integer',
            'user_id'    => 'required|integer',
            'bus_id'     => 'required|integer',
            'stop_id'    => 'required|integer',
            'ts'         => 'required|integer',
        ]);

        // Verificar que el QR no esté expirado (60 segundos)
        $nowMs = (int) (microtime(true) * 1000);
        $ageMs = $nowMs - (int) $validated['ts'];

        if ($ageMs > 60_000) {
            return $this->error('El código QR ha expirado', 422);
        }

        $busRequest = BusRequest::find($validated['request_id']);

        if (! $busRequest) {
            return $this->notFound('Solicitud no encontrada');
        }

        // Verificar que los datos del QR coincidan con la solicitud real
        if ($busRequest->user_id !== (int) $validated['user_id'] ||
            $busRequest->bus_id  !== (int) $validated['bus_id']  ||
            $busRequest->stop_id !== (int) $validated['stop_id']) {
            return $this->error('QR inválido: los datos no coinciden', 422);
        }

        if ($busRequest->status === 'boarded') {
            return $this->error('Este QR ya fue utilizado', 422);
        }

        if ($busRequest->status !== 'pending') {
            return $this->error("La solicitud no está activa (estado: {$busRequest->status})", 422);
        }

        $busRequest->update([
            'status'     => 'boarded',
            'boarded_at' => now(),
        ]);

        $busRequest->load(['user', 'bus', 'stop']);

        return $this->success([
            'user' => [
                'id'    => $busRequest->user->id,
                'name'  => $busRequest->user->name,
                'email' => $busRequest->user->email,
            ],
            'bus'  => $busRequest->bus->name,
            'stop' => $busRequest->stop->name,
        ], 'Acceso validado correctamente');
    }
}
