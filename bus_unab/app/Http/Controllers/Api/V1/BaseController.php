<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Traits\ApiResponseTrait;
use App\Services\IdempotencyConflict;
use App\Services\IdempotencyService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

abstract class BaseController extends Controller
{
    use ApiResponseTrait;

    /**
     * Añade ETag + Cache-Control privado y responde 304 si el cliente ya tiene la
     * misma versión. Para listas estables (paradas, catálogo) en redes lentas.
     */
    protected function withEtag(Request $request, JsonResponse $response, int $maxAge = 60): JsonResponse
    {
        $response->setEtag(md5((string) $response->getContent()));
        $response->headers->set('Cache-Control', "private, max-age={$maxAge}");
        $response->isNotModified($request);

        return $response;
    }

    /**
     * Ejecuta $action una sola vez por header `Idempotency-Key` (y conductor).
     * $action devuelve [status, envelope]; sin header se ejecuta normal.
     * Un reintento devuelve el resultado original con `Idempotent-Replayed: true`.
     *
     * @param  callable(): array{0:int,1:array}  $action
     */
    protected function idempotently(Request $request, string $scope, string $fingerprint, callable $action): JsonResponse
    {
        $key = $request->header('Idempotency-Key');

        if ($key !== null && ! preg_match(IdempotencyService::KEY_PATTERN, $key)) {
            return $this->error('Idempotency-Key inválida (1-190 caracteres: letras, números, . _ : -).', 422);
        }

        try {
            [$status, $body, $replayed] = app(IdempotencyService::class)
                ->run($request->user(), $scope, $key, hash('sha256', $fingerprint), $action);
        } catch (IdempotencyConflict $e) {
            return $this->error($e->getMessage(), 422);
        }

        $response = response()->json($body, $status);

        return $replayed ? $response->header('Idempotent-Replayed', 'true') : $response;
    }
}
