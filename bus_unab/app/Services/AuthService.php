<?php

namespace App\Services;

use App\Models\Transportadora;
use App\Models\User;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use Illuminate\Validation\ValidationException;
use RuntimeException;

class AuthService
{
    /**
     * Intenta autenticar un usuario con email y contraseña.
     */
    public function attemptLogin(string $email, string $password): ?User
    {
        $user = User::where('email', $email)->first();

        if (! $user || ! Hash::check($password, $user->password)) {
            return null;
        }

        return $user;
    }

    /**
     * Valida un Google ID Token contra la API de Google.
     *
     * @return array|null Payload del token o null si es inválido.
     *
     * @throws RuntimeException Si GOOGLE_CLIENT_ID no está configurado.
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
                    'received' => substr($payload['aud'] ?? '', 0, 30).'...',
                ]);

                return null;
            }

            // Verificar que el emisor sea Google
            if (! in_array($payload['iss'] ?? '', ['accounts.google.com', 'https://accounts.google.com'], true)) {
                Log::warning('AuthService: token Google con iss incorrecto', [
                    'received' => substr((string) ($payload['iss'] ?? ''), 0, 40),
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
    /**
     * Busca/vincula/crea el usuario de un id_token de Google ya verificado.
     * Devuelve null si habría que VINCULAR o CREAR con un email que Google no
     * marca como verificado (evita tomar cuentas existentes, p. ej. admin/driver).
     * Una cuenta ya vinculada por `sub` entra siempre. `$organizationSlug` solo
     * se usa al CREAR un pasajero nuevo; nunca cambia la org de un usuario existente.
     *
     * @throws ValidationException slug de organización inválido al crear
     */
    public function findOrCreateUser(array $googlePayload, ?string $organizationSlug = null): ?User
    {
        $googleId = $googlePayload['sub'];
        $email = $googlePayload['email'] ?? '';
        $name = $googlePayload['name'] ?? 'Sin nombre';
        $avatar = $googlePayload['picture'] ?? null;

        // Intento 1: buscar por google_id
        $user = User::where('google_id', $googleId)->first();

        if ($user) {
            // Actualizar avatar y nombre por si cambiaron en Google
            $user->update(['name' => $name, 'avatar' => $avatar]);

            return $user;
        }

        // Vincular o crear exige email verificado por Google.
        if (! $this->emailVerified($googlePayload) || $email === '') {
            return null;
        }

        // Intento 2: vincular una cuenta existente por email
        $user = User::where('email', $email)->first();

        if ($user) {
            $user->update([
                'google_id' => $googleId,
                'name' => $name,
                'avatar' => $avatar,
            ]);

            return $user;
        }

        // Intento 3: crear nuevo usuario — H1: quien llega por la app es PASAJERO
        // (el antiguo 'student' quedó eliminado del producto; ver migration 130000).
        $organizationId = $this->resolveOrganizationId($organizationSlug);

        return User::create([
            'transportadora_id' => $organizationId,
            'google_id' => $googleId,
            'name' => $name,
            'email' => $email,
            'avatar' => $avatar,
            'role' => 'pasajero',
        ]);
    }

    /** Google envía email_verified como bool o como string "true"/"false". */
    private function emailVerified(array $payload): bool
    {
        $v = $payload['email_verified'] ?? false;

        return $v === true || $v === 'true';
    }

    /** Slug opcional de organización: solo tenants existentes y activos. */
    public function resolveOrganizationId(?string $slug): ?int
    {
        if ($slug === null || $slug === '') {
            return null;
        }

        $id = Transportadora::where('slug', $slug)->where('activo', true)->value('id');

        if (! $id) {
            throw ValidationException::withMessages(['organization' => 'Organización no válida.']);
        }

        return $id;
    }

    /** Máximo de sesiones (dispositivos) activas por usuario. */
    public const MAX_ACTIVE_TOKENS = 5;

    /**
     * Genera un token Sanctum por dispositivo. Reemplaza solo el token del mismo
     * nombre y conserva como máximo MAX_ACTIVE_TOKENS (borra los más viejos).
     */
    public function generateToken(User $user, ?string $deviceName = null): string
    {
        $name = trim((string) $deviceName) !== '' ? trim((string) $deviceName) : 'mobile_app';

        $user->tokens()->where('name', $name)->delete();

        $excess = $user->tokens()->count() - (self::MAX_ACTIVE_TOKENS - 1);
        if ($excess > 0) {
            $user->tokens()->orderBy('id')->limit($excess)->get()->each->delete();
        }

        return $user->createToken($name)->plainTextToken;
    }
}
