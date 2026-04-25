<?php

use App\Http\Controllers\Admin\BusMapController;
use Illuminate\Support\Facades\Route;

Route::get('/', fn () => redirect('/admin'));

/*
|--------------------------------------------------------------------------
| Admin — rutas internas del panel (protegidas por sesión Filament)
|--------------------------------------------------------------------------
|
| Estas rutas son llamadas vía AJAX desde las páginas de Filament.
| La sesión del usuario admin ya está presente en la cookie de sesión.
|
*/
Route::prefix('admin')
    ->middleware(['web'])
    ->group(function () {
        // Datos del mapa en tiempo real para el dashboard de Filament
        Route::get('map-data', [BusMapController::class, 'data'])
            ->name('admin.map-data');
    });
