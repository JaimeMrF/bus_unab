<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Configuración del servicio GPS gpsmobile.co
    |--------------------------------------------------------------------------
    | Credenciales fijas para el servicio externo de rastreo GPS de UNAB.
    | Estos valores se sobreescriben desde el .env.
    */

    'base_url'        => env('GPSMOBILE_BASE_URL', 'http://gpsmobile.co:4000'),
    'user'            => env('GPSMOBILE_USER', 'unab.ruta'),
    'password'        => env('GPSMOBILE_PASSWORD', 'Unab2023'),

    // CodUserInc retornado por el login del servicio externo (fijo para UNAB)
    'cod_user_inc'    => env('GPSMOBILE_COD_USER_INC', 110571),

    // Coordenadas del campus UNAB (usadas como centro por defecto)
    'default_lat'     => env('GPSMOBILE_DEFAULT_LAT', 7.1218),
    'default_lng'     => env('GPSMOBILE_DEFAULT_LNG', -73.1158),

    // Timeout en segundos para llamadas al servicio externo
    'timeout'         => env('GPSMOBILE_TIMEOUT', 10),

];
