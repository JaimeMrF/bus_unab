<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\BusRequest;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class QrController extends BaseController
{
    private const MAX_CLOCK_SKEW_MS = 5_000;

    /**
     * Valida el QR de un estudiante y lo marca como abordado.
     * POST /api/v1/qr/validate
     * Solo conductores y admins. Idempotente con el header `Idempotency-Key`.
     */
    public function validate(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'request_id' => 'required|integer',
            'user_id' => 'required|integer',
            'bus_id' => 'required|integer',
            'stop_id' => 'required|integer',
            'ts' => 'required|integer',
        ]);

        ksort($validated);

        return $this->idempotently($request, 'qr_validate', json_encode($validated), fn (): array => $this->board($validated));
    }

    /** @return array{0:int,1:array} */
    private function board(array $validated): array
    {
        $fail = fn (string $message, int $status = 422): array => [$status, ['success' => false, 'message' => $message]];

        // Verificar que el QR no esté expirado (60 segundos)
        $nowMs = (int) (microtime(true) * 1000);
        $ageMs = $nowMs - (int) $validated['ts'];

        if ($ageMs > 60_000) {
            return $fail('El código QR ha expirado');
        }

        // `ts` lo envía el cliente sin firma: un ts futuro nunca expiraría. Solo se
        // tolera un desfase de reloj pequeño.
        if ($ageMs < -self::MAX_CLOCK_SKEW_MS) {
            return $fail('QR inválido: marca de tiempo futura');
        }

        $busRequest = BusRequest::find($validated['request_id']);

        if (! $busRequest) {
            return $fail('Solicitud no encontrada', 404);
        }

        // Verificar que los datos del QR coincidan con la solicitud real
        if ($busRequest->user_id !== (int) $validated['user_id'] ||
            $busRequest->bus_id !== (int) $validated['bus_id'] ||
            $busRequest->stop_id !== (int) $validated['stop_id']) {
            return $fail('QR inválido: los datos no coinciden');
        }

        if ($busRequest->status === 'boarded') {
            return $fail('Este QR ya fue utilizado');
        }

        if ($busRequest->status !== 'pending') {
            return $fail("La solicitud no está activa (estado: {$busRequest->status})");
        }

        $busRequest->update([
            'status' => 'boarded',
            'boarded_at' => now(),
        ]);

        $busRequest->load(['user', 'bus', 'stop']);

        return [200, [
            'success' => true,
            'message' => 'Acceso validado correctamente',
            'data' => [
                'user' => [
                    'id' => $busRequest->user->id,
                    'name' => $busRequest->user->name,
                    'email' => $busRequest->user->email,
                ],
                'bus' => $busRequest->bus->name,
                'stop' => $busRequest->stop->name,
            ],
        ]];
    }
}
