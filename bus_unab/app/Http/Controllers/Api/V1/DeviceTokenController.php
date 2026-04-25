<?php

namespace App\Http\Controllers\Api\V1;

use App\Models\DeviceToken;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class DeviceTokenController extends BaseController
{
    /**
     * Registra o actualiza el token FCM del dispositivo.
     * POST /api/v1/device-token
     */
    public function store(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'token'    => 'required|string|min:10|max:4096',
            'platform' => 'required|in:android,ios',
        ]);

        DeviceToken::updateOrCreate(
            ['user_id' => $request->user()->id, 'token' => $validated['token']],
            ['platform' => $validated['platform']]
        );

        return $this->success(null, 'Token registrado correctamente');
    }

    /**
     * Elimina el token FCM del dispositivo (al hacer logout).
     * DELETE /api/v1/device-token
     */
    public function destroy(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'token' => 'required|string|min:10|max:4096',
        ]);

        $request->user()->deviceTokens()->where('token', $validated['token'])->delete();

        return $this->noContent();
    }
}
