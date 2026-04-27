<?php

namespace App\Services;

use App\Models\User;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Hash;
use RuntimeException;

class AuthService
{
    /**
     * Intenta autenticar un usuario con email y contraseña.
     */
    public function attemptLogin(string $email, string $password): ?User
    {
        $user = User::where('email', $email)->first();

        if (!$user || !Hash::check($password, $user->password)) {
            return null;
        }

        return $user;
    }

    /**
     * Valida un Google ID Token contra la API de Google.
     *
     * @throws RuntimeException  Si GOOGLE_CLIENT_ID no está configurado.
     * @return array|null         Payload del token o null si es inválido.
     */
    public function verifyGoogleToken(string $idToken): ?array
    {
        $clientId = config('services.google.client_id');

        // Fallo estricto: sin Client ID no se puede validar el audience
        if (! $clientId || $clientId === 'your-google-client-id.apps.googleusercontent.com') {
            throw new RuntimeException(
                'GOOGLE_CLIENT_ID no está configurado. Configure el Client ID de Google en .env.'
            );
        }

        try {
            $response = Http::timeout(10)
                ->get('https://oauth2.googleapis.com/tokeninfo', [
                    'id_token' => $idToken,
                ]);

            if (! $response->successful()) {
                return null;
            }

            $payload = $response->json();

            // Verificar que el token fue emitido exactamente para nuestra app
            if (($payload['aud'] ?? '') !== $clientId) {
                Log::warning('AuthService: token Google con aud incorrecto', [
                    'expected' => $clientId,
                    'received' => substr($payload['aud'] ?? '', 0, 30) . '...',
                ]);
                return null;
            }

            // Verificar que el token no está expirado (Google lo valida, pero doble check)
            if (isset($payload['exp']) && $payload['exp'] < time()) {
                return null;
            }

            return $payload;

        } catch (ConnectionException $e) {
            Log::error('AuthService: error conectando a Google', [
                'error' => $e->getMessage(),
            ]);
            return null;
        }
    }

    /**
     * Busca o crea un usuario a partir del payload de Google.
     *
     * Estrategia de búsqueda:
     *   1. Primero por google_id (vínculo permanente).
     *   2. Si no existe, por email — para vincular cuentas preexistentes
     *      (ej. admin creado manualmente) sin duplicar registros.
     */
    public function findOrCreateUser(array $googlePayload): User
    {
        $googleId = $googlePayload['sub'];
        $email    = $googlePayload['email']   ?? '';
        $name     = $googlePayload['name']    ?? 'Sin nombre';
        $avatar   = $googlePayload['picture'] ?? null;

        // Intento 1: buscar por google_id
        $user = User::where('google_id', $googleId)->first();

        if ($user) {
            // Actualizar avatar y nombre por si cambiaron en Google
            $user->update(['name' => $name, 'avatar' => $avatar]);
            return $user;
        }

        // Intento 2: vincular una cuenta existente por email
        $user = User::where('email', $email)->first();

        if ($user) {
            $user->update([
                'google_id' => $googleId,
                'name'      => $name,
                'avatar'    => $avatar,
            ]);
            return $user;
        }

        // Intento 3: crear nuevo usuario
        return User::create([
            'google_id' => $googleId,
            'name'      => $name,
            'email'     => $email,
            'avatar'    => $avatar,
            'role'      => 'student',
        ]);
    }

    /**
     * Genera un token Sanctum para el usuario.
     * Revoca tokens anteriores (un token activo por usuario).
     */
    public function generateToken(User $user): string
    {
        $user->tokens()->delete();

        return $user->createToken('mobile_app')->plainTextToken;
    }
}
