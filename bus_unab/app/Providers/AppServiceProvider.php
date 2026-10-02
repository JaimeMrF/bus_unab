<?php

namespace App\Providers;

use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\Facades\URL;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void {}

    public function boot(): void
    {
        if (config('app.env') === 'production') {
            URL::forceScheme('https');
        }

        $this->configureRateLimiting();
    }

    /**
     * Limitadores nombrados con contador independiente. Los `throttle:N,M`
     * inline comparten un único contador por IP entre TODAS las rutas.
     */
    private function configureRateLimiting(): void
    {
        // Login/registro/Google comparten cupo a propósito (anti fuerza bruta).
        RateLimiter::for('auth', fn (Request $r) => Limit::perMinute(10)->by('auth|'.$r->ip()));
        RateLimiter::for('branding', fn (Request $r) => Limit::perMinute(60)->by('branding|'.$r->ip()));

        // Resto de la API: un contador por método+ruta y usuario (o IP si es anónimo).
        foreach ([5, 10, 20, 30, 60] as $perMinute) {
            RateLimiter::for("api-{$perMinute}", fn (Request $r) => Limit::perMinute($perMinute)->by(
                "api-{$perMinute}|".$r->method().'|'.($r->route()?->uri() ?? $r->path()).'|'.($r->user()?->getAuthIdentifier() ?? $r->ip())
            ));
        }
    }
}
