<?php

namespace App\Http\Controllers\Api\V1;

use App\Services\AuthService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AuthController extends BaseController
{
    public function __construct(private readonly AuthService $authService) {}

    /**
     * Inicia sesión con Google SSO.
     *
     * La app Kotlin obtiene el ID Token de Google y lo envía aquí.
     * El servidor lo valida, busca o crea el usuario, y devuelve un JWT propio.
     *
     * @bodyParam id_token string required  Google ID Token. Example: eyJhbGci...
     */
    public function googleLogin(Request $request): JsonResponse
    {
        $request->validate([
            'id_token' => 'required|string',
        ]);

        // 1. Verificar token con Google
        $payload = $this->authService->verifyGoogleToken($request->id_token);

        if (! $payload) {
            return $this->unauthorized('Token de Google inválido o expirado');
        }

        // 2. Validar dominio institucional
        $email = $payload['email'] ?? '';
        if (! str_ends_with($email, '@unab.edu.co')) {
            return $this->forbidden('Solo se permiten cuentas institucionales @unab.edu.co');
        }

        // 3. Buscar o crear usuario en nuestra BD
        $user = $this->authService->findOrCreateUser($payload);

        // 4. Generar token Sanctum propio
        $token = $this->authService->generateToken($user);

        return $this->successResponse($user, $token);
    }

    /**
     * Inicia sesión tradicional con email y contraseña.
     *
     * @bodyParam email string required. Example: test@example.com
     * @bodyParam password string required. Example: password
     */
    public function login(Request $request): JsonResponse
    {
        $request->validate([
            'email'    => 'required|string|email',
            'password' => 'required|string',
        ]);

        $user = $this->authService->attemptLogin($request->email, $request->password);

        if (! $user) {
            return $this->unauthorized('Credenciales incorrectas');
        }

        $token = $this->authService->generateToken($user);

        return $this->successResponse($user, $token);
    }

    /**
     * Formatea la respuesta exitosa de login.
     */
    private function successResponse($user, $token): JsonResponse
    {
        return $this->success([
            'access_token' => $token,
            'token_type'   => 'Bearer',
            'user'         => [
                'id'     => $user->id,
                'name'   => $user->name,
                'email'  => $user->email,
                'avatar' => $user->avatar,
                'role'   => $user->role,
            ],
        ], 'Autenticación exitosa');
    }

    /**
     * Cierra la sesión revocando el token actual.
     */
    public function logout(Request $request): JsonResponse
    {
        $request->user()->currentAccessToken()->delete();

        return $this->success(null, 'Sesión cerrada exitosamente');
    }

    /**
     * Retorna la info del usuario autenticado.
     */
    public function me(Request $request): JsonResponse
    {
        $user = $request->user();

        return $this->success([
            'id'     => $user->id,
            'name'   => $user->name,
            'email'  => $user->email,
            'avatar' => $user->avatar,
            'role'   => $user->role,
        ]);
    }
}
