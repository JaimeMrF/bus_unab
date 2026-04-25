<?php

namespace App\Services;

use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\Stop;
use App\Models\User;
use Illuminate\Support\Facades\DB;

class BusRequestService
{
    public function __construct(private readonly NotificationService $notificationService) {}

    /**
     * El usuario solicita un bus en una parada específica.
     *
     * Usa transacción con lock pesimista para evitar race conditions
     * en el conteo de aforo.
     */
    public function request(User $user, Bus $bus, Stop $stop): array
    {
        return DB::transaction(function () use ($user, $bus, $stop) {
            // Lock pesimista: bloquea el registro del bus hasta que termine la transacción
            $bus = Bus::lockForUpdate()->findOrFail($bus->id);

            // Cancelar solicitud activa previa del mismo usuario para este bus
            BusRequest::where('user_id', $user->id)
                ->where('bus_id', $bus->id)
                ->where('status', 'pending')
                ->update(['status' => 'cancelled']);

            // Contar dentro de la transacción (conteo real y consistente)
            $pending = BusRequest::where('bus_id', $bus->id)
                ->where('status', 'pending')
                ->count();

            $isFull = $pending >= $bus->capacity;

            // Crear la nueva solicitud
            $request = BusRequest::create([
                'user_id' => $user->id,
                'bus_id'  => $bus->id,
                'stop_id' => $stop->id,
                'status'  => 'pending',
            ]);

            return [
                'request'           => $request->load(['bus:id,name,plate', 'stop:id,name,address']),
                'current_occupancy' => $pending + 1,
                'capacity'          => $bus->capacity,
                'percentage'        => $this->calcPercentage($pending + 1, $bus->capacity),
                'level'             => $this->calcLevel($pending + 1, $bus->capacity),
                'is_full'           => $isFull,
            ];
        });
    }

    /**
     * El usuario cancela su solicitud activa.
     */
    public function cancel(User $user, Bus $bus): bool
    {
        return (bool) BusRequest::where('user_id', $user->id)
            ->where('bus_id', $bus->id)
            ->where('status', 'pending')
            ->update(['status' => 'cancelled']);
    }

    /**
     * Llamado cuando el bus llega a una parada.
     * Marca como "boarded" todas las solicitudes pendientes y notifica usuarios.
     *
     * Las notificaciones FCM se envían FUERA de la transacción para no mantener
     * locks de base de datos mientras se espera una respuesta HTTP externa.
     */
    public function markBusArrivedAtStop(Bus $bus, Stop $stop): int
    {
        // Fase 1 — Transacción corta: solo operaciones de DB
        $requests = DB::transaction(function () use ($bus, $stop) {
            $requests = BusRequest::with('user.deviceTokens')
                ->where('bus_id', $bus->id)
                ->where('stop_id', $stop->id)
                ->where('status', 'pending')
                ->lockForUpdate()
                ->get();

            if ($requests->isEmpty()) {
                return collect();
            }

            BusRequest::whereIn('id', $requests->pluck('id'))->update([
                'status'     => 'boarded',
                'boarded_at' => now(),
            ]);

            return $requests;
        });

        if ($requests->isEmpty()) {
            return 0;
        }

        // Fase 2 — Notificaciones FCM fuera de la transacción (I/O externo)
        $requests->each(fn ($req) => $this->notificationService->notifyBusArrival(
            $req->user, $bus, $stop
        ));

        return $requests->count();
    }

    /**
     * Retorna el aforo actual de un bus.
     */
    public function getOccupancy(Bus $bus): array
    {
        $pending = BusRequest::where('bus_id', $bus->id)
            ->where('status', 'pending')
            ->count();

        return [
            'bus_id'            => $bus->id,
            'bus_name'          => $bus->name,
            'current_occupancy' => $pending,
            'capacity'          => $bus->capacity,
            'percentage'        => $this->calcPercentage($pending, $bus->capacity),
            'level'             => $this->calcLevel($pending, $bus->capacity),
        ];
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private function calcPercentage(int $count, int $capacity): float
    {
        if ($capacity === 0) return 0.0;
        return round(min(($count / $capacity) * 100, 100), 1);
    }

    private function calcLevel(int $count, int $capacity): string
    {
        $pct = $this->calcPercentage($count, $capacity);
        return match (true) {
            $pct >= 90 => 'full',
            $pct >= 60 => 'high',
            $pct >= 30 => 'medium',
            default    => 'low',
        };
    }
}
