<?php

use App\Http\Controllers\Api\V1\AuthController;
use App\Http\Controllers\Api\V1\BrandingController;
use App\Http\Controllers\Api\V1\BusController;
use App\Http\Controllers\Api\V1\BusRequestController;
use App\Http\Controllers\Api\V1\DeviceTokenController;
use App\Http\Controllers\Api\V1\FavoriteStopController;
use App\Http\Controllers\Api\V1\NotificationController;
use App\Http\Controllers\Api\V1\PointOfInterestController;
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
|   - throttle:api-N  → N requests por minuto, contador propio por ruta y usuario/IP
|     (limitadores nombrados en AppServiceProvider; auth y branding tienen el suyo)
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
    Route::prefix('auth')->middleware('throttle:auth')->group(function () {
        // POST /api/v1/auth/google
        Route::post('google', [AuthController::class, 'googleLogin']);
        // POST /api/v1/auth/login
        Route::post('login', [AuthController::class, 'login']);
        // H3 · POST /api/v1/auth/register — solo pasajeros (autogestión app)
        Route::post('register', [AuthController::class, 'register']);
    });

    // ------------------------------------------------------------------
    // Branding white-label — público (el cliente aún no conoce el tenant)
    // ------------------------------------------------------------------
    // GET /api/v1/branding/{slug}
    Route::get('branding/{slug}', [BrandingController::class, 'show'])
        ->middleware('throttle:branding')
        ->where('slug', '[A-Za-z0-9_-]{1,60}');

    // ------------------------------------------------------------------
    // Rutas protegidas — requieren token Sanctum válido
    // ------------------------------------------------------------------
    Route::middleware('auth:sanctum')->group(function () {

        // Auth
        Route::prefix('auth')->middleware('throttle:api-30')->group(function () {
            Route::post('logout', [AuthController::class, 'logout']); // POST /api/v1/auth/logout
            Route::get('me', [AuthController::class, 'me']);     // GET  /api/v1/auth/me
        });

        // Token FCM — 5 registros por minuto máximo
        Route::prefix('device-token')->middleware('throttle:api-5')->group(function () {
            Route::post('/', [DeviceTokenController::class, 'store']);   // POST   /api/v1/device-token
            Route::delete('/', [DeviceTokenController::class, 'destroy']); // DELETE /api/v1/device-token
        });

        // Buses — Lectura: rate generoso. Escritura admin/driver: rate estricto
        // tenant.scope: miembros de una transportadora solo ven SU flota (S3.3.3)
        Route::prefix('buses')->middleware('tenant.scope')->group(function () {
            Route::middleware('throttle:api-60')->group(function () {
                Route::get('/', [BusController::class, 'index']);              // GET  /api/v1/buses
                Route::get('catalog', [BusController::class, 'catalog']);           // GET  /api/v1/buses/catalog
                Route::get('{plate}', [BusController::class, 'show']);              // GET  /api/v1/buses/RUTA1
                Route::get('{plate}/eta', [BusController::class, 'eta']);               // GET  /api/v1/buses/RUTA1/eta?stop_id=
                Route::get('{plate}/stops', [StopController::class, 'byBus']);            // GET  /api/v1/buses/RUTA1/stops
                Route::get('{plate}/route', [BusController::class, 'route']);             // GET  /api/v1/buses/RUTA1/route
                Route::get('{plate}/occupancy', [BusRequestController::class, 'occupancy']); // GET  /api/v1/buses/RUTA1/occupancy
            });

            // Solo conductores y admins pueden reportar llegada / proximidad / aforo
            Route::post('{plate}/arrived', [BusRequestController::class, 'busArrived'])
                ->middleware(['throttle:api-30', 'role:admin,driver']); // POST /api/v1/buses/RUTA1/arrived
            Route::post('{plate}/approaching', [BusRequestController::class, 'busApproaching'])
                ->middleware(['throttle:api-30', 'role:admin,driver']); // POST /api/v1/buses/RUTA1/approaching
            Route::post('{plate}/occupancy', [BusRequestController::class, 'updateOccupancy'])
                ->middleware(['throttle:api-30', 'role:admin,driver']); // POST /api/v1/buses/RUTA1/occupancy
            Route::post('{plate}/location', [BusController::class, 'updateDriverLocation'])
                ->middleware(['throttle:api-60', 'role:admin,driver']); // POST   /api/v1/buses/RUTA1/location
            Route::delete('{plate}/location', [BusController::class, 'clearDriverLocation'])
                ->middleware(['throttle:api-60', 'role:admin,driver']); // DELETE /api/v1/buses/RUTA1/location
        });

        // Paradas
        Route::get('stops', [StopController::class, 'index'])
            ->middleware(['throttle:api-60', 'tenant.scope']); // GET /api/v1/stops

        // Paradas favoritas (tenant-scoped)
        Route::prefix('favorites/stops')->middleware(['throttle:api-60', 'tenant.scope'])->group(function () {
            Route::get('/', [FavoriteStopController::class, 'index']);              // GET    /api/v1/favorites/stops
            Route::post('/', [FavoriteStopController::class, 'store']);             // POST   /api/v1/favorites/stops
            Route::delete('{stopId}', [FavoriteStopController::class, 'destroy'])->whereNumber('stopId'); // DELETE /api/v1/favorites/stops/{id}
        });

        // Solicitudes de bus (aforo) — 20 solicitudes por minuto máximo
        Route::prefix('requests')->middleware(['throttle:api-20', 'tenant.scope'])->group(function () {
            Route::get('/', [BusRequestController::class, 'myRequests']); // GET    /api/v1/requests
            Route::post('/', [BusRequestController::class, 'store']);      // POST   /api/v1/requests
            Route::delete('{bus}', [BusRequestController::class, 'cancel']);     // DELETE /api/v1/requests/{busId}
        });

        // Validación de QR — solo conductores y admins
        Route::post('qr/validate', [QrController::class, 'validate'])
            ->middleware(['throttle:api-60', 'tenant.scope', 'role:admin,driver']); // POST /api/v1/qr/validate

        // Wallet prepago + QR dinámico de pago (M3) — NO toca /qr/validate
        Route::prefix('wallet')->group(function () {
            Route::get('/', [WalletController::class, 'show'])
                ->middleware('throttle:api-60'); // GET  /api/v1/wallet
            Route::post('recharge-mock', [WalletController::class, 'rechargeMock'])
                ->middleware('throttle:api-10'); // POST /api/v1/wallet/recharge-mock (local|testing)
            Route::post('qr/issue', [WalletController::class, 'issueQr'])
                ->middleware('throttle:api-20'); // POST /api/v1/wallet/qr/issue
        });
        Route::post('qr/pay', [WalletController::class, 'pay'])
            ->middleware(['throttle:api-60', 'tenant.scope', 'role:admin,driver']); // POST /api/v1/qr/pay

        // Broadcast global — solo admin
        Route::post('admin/broadcast', [NotificationController::class, 'broadcast'])
            ->middleware(['throttle:api-10', 'role:admin']); // POST /api/v1/admin/broadcast

        // Puntos de interés
        Route::get('poi', [PointOfInterestController::class, 'index'])
            ->middleware('throttle:api-60'); // GET /api/v1/poi
    });
});
