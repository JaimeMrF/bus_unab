<?php

use App\Http\Controllers\Api\V1\AuthController;
use App\Http\Controllers\Api\V1\BusController;
use App\Http\Controllers\Api\V1\BusRequestController;
use App\Http\Controllers\Api\V1\DeviceTokenController;
use App\Http\Controllers\Api\V1\PointOfInterestController;
use App\Http\Controllers\Api\V1\QrController;
use App\Http\Controllers\Api\V1\StopController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes — Bus UNAB v1
|--------------------------------------------------------------------------
|
| Convenciones:
|   - throttle:60,1  → 60 requests por minuto por IP
|   - throttle:10,1  → 10 requests por minuto (endpoints sensibles)
|   - role:admin     → solo administradores
|   - role:admin,driver → administradores o conductores
|
*/

Route::prefix('v1')->group(function () {

    // ------------------------------------------------------------------
    // Auth — Públicas con rate limiting estricto
    // ------------------------------------------------------------------
    Route::prefix('auth')->middleware('throttle:10,1')->group(function () {
        // POST /api/v1/auth/google
        Route::post('google', [AuthController::class, 'googleLogin']);
        // POST /api/v1/auth/login
        Route::post('login', [AuthController::class, 'login']);
    });

    // ------------------------------------------------------------------
    // Rutas protegidas — requieren token Sanctum válido
    // ------------------------------------------------------------------
    Route::middleware('auth:sanctum')->group(function () {

        // Auth
        Route::prefix('auth')->middleware('throttle:30,1')->group(function () {
            Route::post('logout', [AuthController::class, 'logout']); // POST /api/v1/auth/logout
            Route::get('me',      [AuthController::class, 'me']);     // GET  /api/v1/auth/me
        });

        // Token FCM — 5 registros por minuto máximo
        Route::prefix('device-token')->middleware('throttle:5,1')->group(function () {
            Route::post('/',   [DeviceTokenController::class, 'store']);   // POST   /api/v1/device-token
            Route::delete('/', [DeviceTokenController::class, 'destroy']); // DELETE /api/v1/device-token
        });

        // Buses — Lectura: rate generoso. Escritura admin/driver: rate estricto
        Route::prefix('buses')->group(function () {
            Route::middleware('throttle:60,1')->group(function () {
                Route::get('/',               [BusController::class, 'index']);              // GET  /api/v1/buses
                Route::get('{plate}',         [BusController::class, 'show']);              // GET  /api/v1/buses/RUTA1
                Route::get('{plate}/stops',   [StopController::class, 'byBus']);            // GET  /api/v1/buses/RUTA1/stops
                Route::get('{plate}/occupancy', [BusRequestController::class, 'occupancy']); // GET  /api/v1/buses/RUTA1/occupancy
            });

            // Solo conductores y admins pueden reportar llegada
            Route::post('{plate}/arrived', [BusRequestController::class, 'busArrived'])
                ->middleware(['throttle:30,1', 'role:admin,driver']); // POST /api/v1/buses/RUTA1/arrived
        });

        // Paradas
        Route::get('stops', [StopController::class, 'index'])
            ->middleware('throttle:60,1'); // GET /api/v1/stops

        // Solicitudes de bus (aforo) — 20 solicitudes por minuto máximo
        Route::prefix('requests')->middleware('throttle:20,1')->group(function () {
            Route::post('/',       [BusRequestController::class, 'store']);  // POST   /api/v1/requests
            Route::delete('{bus}', [BusRequestController::class, 'cancel']); // DELETE /api/v1/requests/{busId}
        });

        // Validación de QR — solo conductores y admins
        Route::post('qr/validate', [QrController::class, 'validate'])
            ->middleware(['throttle:60,1', 'role:admin,driver']); // POST /api/v1/qr/validate

        // Puntos de interés
        Route::get('poi', [PointOfInterestController::class, 'index'])
            ->middleware('throttle:60,1'); // GET /api/v1/poi
    });
});
