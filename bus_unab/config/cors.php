<?php

/*
|--------------------------------------------------------------------------
| CORS
|--------------------------------------------------------------------------
|
| La app móvil usa tokens Bearer y no depende de CORS; solo los clientes web
| listados en CORS_ALLOWED_ORIGINS (separados por coma, sin comodines) pueden
| consumir la API desde un navegador. Sin variable → ningún origen.
|
*/

return [

    'paths' => ['api/*', 'sanctum/csrf-cookie'],

    'allowed_methods' => ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],

    'allowed_origins' => array_values(array_filter(array_map(
        'trim',
        explode(',', (string) env('CORS_ALLOWED_ORIGINS', ''))
    ), fn (string $origin): bool => $origin !== '' && $origin !== '*')),

    'allowed_origins_patterns' => [],

    'allowed_headers' => ['Accept', 'Authorization', 'Content-Type', 'If-None-Match', 'X-Requested-With'],

    'exposed_headers' => ['ETag'],

    'max_age' => 600,

    'supports_credentials' => false,

];
