<?php

namespace App\Http\Controllers\Api\V1;

use App\Services\NotificationService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class NotificationController extends BaseController
{
    public function __construct(private readonly NotificationService $notificationService) {}

    /**
     * Broadcast global de notificación.
     * POST /api/v1/admin/broadcast
     * Solo administradores.
     */
    public function broadcast(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'title'   => 'required|string|max:255',
            'body'    => 'required|string|max:1000',
            'role'    => 'nullable|in:all,student,driver,admin',
            'type'    => 'nullable|string|max:50',
        ]);

        $role = $validated['role'] ?? 'all';
        $type = $validated['type'] ?? 'general';

        $this->notificationService->broadcast(
            $validated['title'],
            $validated['body'],
            ['type' => $type],
            $role,
        );

        return $this->success(null, "Notificación enviada al grupo: {$role}");
    }
}
