<?php

use Illuminate\Auth\AuthenticationException;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpKernel\Exception\HttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        // Confiar en todos los proxies inversos (nginx sistema → nginx Docker)
        $middleware->trustProxies(at: '*');

        // Sanctum: autenticación stateless para la app móvil (tokens Bearer)
        $middleware->statefulApi();

        // Alias para el middleware de roles
        $middleware->alias([
            'role' => \App\Http\Middleware\EnsureUserHasRole::class,
            // M3 · S3.3.3 — activa el contexto de tenant en rutas de API
            'tenant.scope' => \App\Http\Middleware\EnsureTenantScope::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {

        // Todas las rutas /api/* siempre devuelven JSON, nunca HTML
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*')
        );

        // 401 — No autenticado
        $exceptions->render(function (AuthenticationException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'success' => false,
                    'message' => 'No autenticado. Token inválido o ausente.',
                ], 401);
            }
        });

        // 422 — Error de validación
        $exceptions->render(function (ValidationException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'success' => false,
                    'message' => 'Los datos enviados no son válidos.',
                    'errors'  => $e->errors(),
                ], 422);
            }
        });

        // 404 — Ruta o modelo no encontrado
        $exceptions->render(function (NotFoundHttpException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'success' => false,
                    'message' => 'Recurso no encontrado.',
                ], 404);
            }
        });

        // Cualquier otro HttpException (403, 429, 503, etc.)
        $exceptions->render(function (HttpException $e, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'success' => false,
                    'message' => $e->getMessage() ?: 'Error en el servidor.',
                ], $e->getStatusCode());
            }
        });

    })->create();
