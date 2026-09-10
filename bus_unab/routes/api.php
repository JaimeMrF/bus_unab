<?php

use App\Http\Controllers\Api\V1\AuthController;
use App\Http\Controllers\Api\V1\BusController;
use App\Http\Controllers\Api\V1\BusRequestController;
use App\Http\Controllers\Api\V1\DeviceTokenController;
use App\Http\Controllers\Api\V1\PointOfInterestController;
use App\Http\Controllers\Api\V1\NotificationController;
use App\Http\Controllers\Api\V1\QrController;
use App\Http\Controllers\Api\V1\StopController;
use App\Http\Controllers\Api\V1\WalletController;
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
 |   - tenant.scope   → activa contexto de transportadora (S3.3.3);
 |                      usuarios con transportadora NULL ven todo (intencional)
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
        // H3 · POST /api/v1/auth/register — solo pasajeros (autogestión app)
        Route::post('register', [AuthController::class, 'register']);
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
        // tenant.scope: miembros de una transportadora solo ven SU flota (S3.3.3)
        Route::prefix('buses')->middleware('tenant.scope')->group(function () {
            Route::middleware('throttle:60,1')->group(function () {
                Route::get('/',               [BusController::class, 'index']);              // GET  /api/v1/buses
                Route::get('catalog',         [BusController::class, 'catalog']);           // GET  /api/v1/buses/catalog
                Route::get('{plate}',         [BusController::class, 'show']);              // GET  /api/v1/buses/RUTA1
                Route::get('{plate}/stops',   [StopController::class, 'byBus']);            // GET  /api/v1/buses/RUTA1/stops
                Route::get('{plate}/route',   [BusController::class, 'route']);             // GET  /api/v1/buses/RUTA1/route
                Route::get('{plate}/occupancy', [BusRequestController::class, 'occupancy']); // GET  /api/v1/buses/RUTA1/occupancy
            });

            // Solo conductores y admins pueden reportar llegada / proximidad / aforo
            Route::post('{plate}/arrived', [BusRequestController::class, 'busArrived'])
                ->middleware(['throttle:30,1', 'role:admin,driver']); // POST /api/v1/buses/RUTA1/arrived
            Route::post('{plate}/approaching', [BusRequestController::class, 'busApproaching'])
                ->middleware(['throttle:30,1', 'role:admin,driver']); // POST /api/v1/buses/RUTA1/approaching
            Route::post('{plate}/occupancy', [BusRequestController::class, 'updateOccupancy'])
                ->middleware(['throttle:30,1', 'role:admin,driver']); // POST /api/v1/buses/RUTA1/occupancy
            Route::post('{plate}/location', [BusController::class, 'updateDriverLocation'])
                ->middleware(['throttle:60,1', 'role:admin,driver']); // POST   /api/v1/buses/RUTA1/location
            Route::delete('{plate}/location', [BusController::class, 'clearDriverLocation'])
                ->middleware(['throttle:60,1', 'role:admin,driver']); // DELETE /api/v1/buses/RUTA1/location
        });

        // Paradas
        Route::get('stops', [StopController::class, 'index'])
            ->middleware(['throttle:60,1', 'tenant.scope']); // GET /api/v1/stops

        // Solicitudes de bus (aforo) — 20 solicitudes por minuto máximo
        Route::prefix('requests')->middleware(['throttle:20,1', 'tenant.scope'])->group(function () {
            Route::get('/',        [BusRequestController::class, 'myRequests']); // GET    /api/v1/requests
            Route::post('/',       [BusRequestController::class, 'store']);      // POST   /api/v1/requests
            Route::delete('{bus}', [BusRequestController::class, 'cancel']);     // DELETE /api/v1/requests/{busId}
        });

        // Validación de QR — solo conductores y admins
        Route::post('qr/validate', [QrController::class, 'validate'])
            ->middleware(['throttle:60,1', 'tenant.scope', 'role:admin,driver']); // POST /api/v1/qr/validate

        // Wallet prepago + QR dinámico de pago (M3) — NO toca /qr/validate
        Route::prefix('wallet')->group(function () {
            Route::get('/', [WalletController::class, 'show'])
                ->middleware('throttle:60,1'); // GET  /api/v1/wallet
            Route::post('recharge-mock', [WalletController::class, 'rechargeMock'])
                ->middleware('throttle:10,1'); // POST /api/v1/wallet/recharge-mock (local|testing)
            Route::post('qr/issue', [WalletController::class, 'issueQr'])
                ->middleware('throttle:20,1'); // POST /api/v1/wallet/qr/issue
        });
        Route::post('qr/pay', [WalletController::class, 'pay'])
            ->middleware(['throttle:60,1', 'tenant.scope', 'role:admin,driver']); // POST /api/v1/qr/pay

        // Broadcast global — solo admin
        Route::post('admin/broadcast', [NotificationController::class, 'broadcast'])
            ->middleware(['throttle:10,1', 'role:admin']); // POST /api/v1/admin/broadcast

        // Puntos de interés
        Route::get('poi', [PointOfInterestController::class, 'index'])
            ->middleware('throttle:60,1'); // GET /api/v1/poi
    });
});
