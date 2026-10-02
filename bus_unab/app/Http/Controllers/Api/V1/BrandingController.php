<?php

namespace App\Http\Controllers\Api\V1;

use App\Services\BrandingService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class BrandingController extends BaseController
{
    public function __construct(private readonly BrandingService $branding) {}

    /**
     * Perfil de branding público de un tenant.
     * GET /api/v1/branding/{slug}
     *
     * 404 genérico tanto si el slug no existe como si el tenant está inactivo.
     */
    public function show(Request $request, string $slug): JsonResponse
    {
        $payload = $this->branding->cachedPayload($slug);

        if ($payload === null) {
            return $this->notFound();
        }

        $response = $this->success($payload);

        $response->setEtag(md5($response->getContent()));
        $response->headers->set('Cache-Control', 'public, max-age=300');
        $response->isNotModified($request);

        return $response;
    }
}
