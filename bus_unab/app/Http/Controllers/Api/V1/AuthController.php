<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\User;
use App\Services\AuthService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

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

        // 2. H1 (pivote): SIN restricción de dominio. cualquiera que entre a la
        //    app es pasajero; las credenciales de panel (admin/gerente/conductor)
        //    solo se emiten desde los paneles Filament, nunca por autogestión.
        //    (El viejo candado @unab.edu.co quedó eliminado junto con el rol student.)

        // 3. Buscar o crear usuario en nuestra BD (AuthService crea role=pasajero)
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
     * H3 · Registro autónomo de PASAJEROS (modelo SaaS del pivote).
     *
     * Solo crea pasajeros: las credenciales de panel (super_admin,
     * tenant_admin, driver) se emiten exclusivamente desde Filament.
     * Sin candado de dominio: un pasajero de la ciudad es quien sea.
     *
     * @bodyParam name string required. Example: Ana Pérez
     * @bodyParam email string required Email único. Example: ana@gmail.com
     * @bodyParam password string required Mínimo 8. Example: Secreta123
     * @bodyParam password_confirmation string required Debe coincidir. Example: Secreta123
     */
    public function register(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name'     => 'required|string|max:255',
            'email'    => 'required|email|max:190|unique:users,email',
            'password' => 'required|string|min:8|confirmed',
        ]);

        $user = User::create([
            'name'     => $data['name'],
            'email'    => $data['email'],
            'password' => Hash::make($data['password']),
            'role'     => 'pasajero',
        ]);

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
