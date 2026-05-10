<?php

namespace App\Services;

use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\DeviceToken;
use App\Models\Stop;
use App\Models\User;
use Google\Auth\Credentials\ServiceAccountCredentials;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class NotificationService
{
    private const SCOPE = 'https://www.googleapis.com/auth/firebase.messaging';

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
        $userIds = BusRequest::where('bus_id', $bus->id)
            ->where('stop_id', $stop->id)
            ->where('status', 'pending')
            ->pluck('user_id');

        $tokens = DeviceToken::whereIn('user_id', $userIds)->pluck('token')->toArray();

        $minutes  = $this->estimateEtaMinutes($bus->latitude, $bus->longitude, $stop->latitude, $stop->longitude);
        $etaText  = $minutes === 1 ? '1 minuto' : "{$minutes} minutos";

        $this->sendToTokens($tokens,
            title: "🚌 {$bus->name} está llegando",
            body:  "Llegará a {$stop->name} en ~{$etaText}",
            data:  [
                'type'         => 'bus_approaching',
                'bus_id'       => (string) $bus->id,
                'stop_id'      => (string) $stop->id,
                'minutes_away' => (string) $minutes,
            ]
        );
    }

    /** Distancia Haversine → tiempo estimado a velocidad urbana (25 km/h). */
    private function estimateEtaMinutes(float $busLat, float $busLon, float $stopLat, float $stopLon): int
    {
        $R    = 6371;
        $dLat = deg2rad($stopLat - $busLat);
        $dLon = deg2rad($stopLon - $busLon);
        $a    = sin($dLat / 2) ** 2
              + cos(deg2rad($busLat)) * cos(deg2rad($stopLat)) * sin($dLon / 2) ** 2;
        $km   = 2 * $R * asin(sqrt($a));

        return max(1, min((int) ceil($km / 25 * 60), 30));
    }

    public function notifyBusAlmostFull(Bus $bus): void
    {
        $userIds = BusRequest::where('bus_id', $bus->id)
            ->where('status', 'pending')
            ->pluck('user_id');

        $tokens = DeviceToken::whereIn('user_id', $userIds)->pluck('token')->toArray();

        $pending = BusRequest::where('bus_id', $bus->id)->where('status', 'pending')->count();
        $pct     = $bus->capacity > 0 ? round(min(($pending / $bus->capacity) * 100, 100), 1) : 0;

        $this->sendToTokens($tokens,
            title: "⚠️ {$bus->name} casi lleno",
            body:  "El bus tiene {$pct}% de ocupación.",
            data:  ['type' => 'bus_almost_full', 'bus_id' => (string) $bus->id]
        );
    }

    public function broadcast(string $title, string $body, array $data = [], string $role = 'all'): void
    {
        $tokens = DeviceToken::when($role !== 'all', fn ($q) =>
            $q->whereHas('user', fn ($u) => $u->where('role', $role))
        )->pluck('token')->toArray();

        $this->sendToTokens($tokens, $title, $body, $data);
    }

    // -------------------------------------------------------------------------

    private function sendToUser(User $user, string $title, string $body, array $data = []): void
    {
        $tokens = $user->deviceTokens()->pluck('token')->toArray();
        $this->sendToTokens($tokens, $title, $body, $data);
    }

    private function sendToTokens(array $tokens, string $title, string $body, array $data = []): void
    {
        if (empty($tokens)) return;

        $accessToken = $this->getAccessToken();
        if (! $accessToken) return;

        $projectId = $this->getProjectId();
        $endpoint  = "https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send";

        // FCM v1 no admite multicast nativo → enviar de uno en uno (máx 500/s)
        foreach ($tokens as $token) {
            try {
                $response = Http::withToken($accessToken)
                    ->post($endpoint, [
                        'message' => [
                            'token'        => $token,
                            'notification' => ['title' => $title, 'body' => $body],
                            'data'         => array_map('strval', $data),
                            'android'      => ['notification' => ['sound' => 'default', 'icon' => 'ic_notification', 'color' => '#5B2C8C']],
                        ],
                    ]);

                if (! $response->successful()) {
                    Log::warning('NotificationService FCM v1 error', [
                        'status' => $response->status(),
                        'body'   => $response->json('error.message'),
                    ]);
                }
            } catch (\Exception $e) {
                Log::error('NotificationService exception', ['error' => $e->getMessage()]);
            }
        }
    }

    private function getAccessToken(): ?string
    {
        try {
            $b64 = config('services.fcm.credentials_b64');

            if (! $b64) {
                Log::warning('NotificationService: FIREBASE_CREDENTIALS_B64 no configurado.');
                return null;
            }

            $json = base64_decode($b64);
            $credentials = new ServiceAccountCredentials(self::SCOPE, json_decode($json, true));
            $token = $credentials->fetchAuthToken();
            return $token['access_token'] ?? null;

        } catch (\Exception $e) {
            Log::error('NotificationService: error obteniendo access token', ['error' => $e->getMessage()]);
            return null;
        }
    }

    private function getProjectId(): string
    {
        $b64  = config('services.fcm.credentials_b64', '');
        $json = json_decode(base64_decode($b64), true);
        return $json['project_id'] ?? '';
    }
}
