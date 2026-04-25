<?php

namespace App\Services;

use App\Models\Bus;
use App\Models\DeviceToken;
use App\Models\Stop;
use App\Models\User;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class NotificationService
{
    public function notifyBusArrival(User $user, Bus $bus, Stop $stop): void
    {
        $this->sendToUser($user,
            title: "🚌 {$bus->name} llegó",
            body:  "Tu bus llegó a la parada: {$stop->name}",
            data:  ['type' => 'bus_arrival', 'bus_id' => (string) $bus->id, 'stop_id' => (string) $stop->id]
        );
    }

    public function notifyBusApproaching(Bus $bus, Stop $stop): void
    {
        $userIds = \App\Models\BusRequest::where('bus_id', $bus->id)
            ->where('stop_id', $stop->id)
            ->where('status', 'pending')
            ->pluck('user_id');

        $tokens = DeviceToken::whereIn('user_id', $userIds)->pluck('token')->toArray();

        $this->sendToTokens($tokens,
            title: "🚌 {$bus->name} está llegando",
            body:  "Tu bus estará en {$stop->name} en aproximadamente 2 minutos",
            data:  ['type' => 'bus_approaching', 'bus_id' => (string) $bus->id, 'stop_id' => (string) $stop->id]
        );
    }

    public function notifyBusAlmostFull(Bus $bus): void
    {
        $userIds = \App\Models\BusRequest::where('bus_id', $bus->id)
            ->where('status', 'pending')
            ->pluck('user_id');

        $tokens = DeviceToken::whereIn('user_id', $userIds)->pluck('token')->toArray();

        $pending = \App\Models\BusRequest::where('bus_id', $bus->id)->where('status', 'pending')->count();
        $pct     = $bus->capacity > 0 ? round(min(($pending / $bus->capacity) * 100, 100), 1) : 0;

        $this->sendToTokens($tokens,
            title: "⚠️ {$bus->name} casi lleno",
            body:  "El bus tiene {$pct}% de ocupación.",
            data:  ['type' => 'bus_almost_full', 'bus_id' => (string) $bus->id]
        );
    }

    /**
     * Broadcast global desde el panel admin.
     */
    public function broadcast(string $title, string $body, array $data = [], string $role = 'all'): void
    {
        $tokens = DeviceToken::when($role !== 'all', fn ($q) =>
            $q->whereHas('user', fn ($u) => $u->where('role', $role))
        )->pluck('token')->toArray();

        $this->sendToTokens($tokens, $title, $body, $data);
    }

    // -------------------------------------------------------------------------
    // Métodos privados
    // -------------------------------------------------------------------------

    private function sendToUser(User $user, string $title, string $body, array $data = []): void
    {
        $tokens = $user->deviceTokens()->pluck('token')->toArray();
        $this->sendToTokens($tokens, $title, $body, $data);
    }

    private function sendToTokens(array $tokens, string $title, string $body, array $data = []): void
    {
        if (empty($tokens)) {
            return;
        }

        $serverKey = config('services.fcm.server_key');

        if (! $serverKey || $serverKey === 'your-fcm-server-key') {
            Log::warning('NotificationService: FCM_SERVER_KEY no configurado. Notificación no enviada.');
            return;
        }

        foreach (array_chunk($tokens, 500) as $chunk) {
            try {
                $response = Http::withHeaders([
                    'Authorization' => "key={$serverKey}",
                    'Content-Type'  => 'application/json',
                ])->post('https://fcm.googleapis.com/fcm/send', [
                    'registration_ids' => $chunk,
                    'notification'     => [
                        'title' => $title,
                        'body'  => $body,
                        'sound' => 'default',
                    ],
                    'data' => $data,
                ]);

                if (! $response->successful()) {
                    // Solo loguear el status code, no el body completo (puede tener info sensible)
                    Log::error('NotificationService: FCM respondió con error', [
                        'status'      => $response->status(),
                        'token_count' => count($chunk),
                    ]);
                }

            } catch (\Exception $e) {
                Log::error('NotificationService: excepción al enviar FCM', [
                    'error' => $e->getMessage(),
                ]);
            }
        }
    }
}
