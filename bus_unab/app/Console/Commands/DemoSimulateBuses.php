<?php

namespace App\Console\Commands;

use App\Models\Bus;
use App\Support\DriverLocation;
use App\Support\RouteWalker;
use Database\Seeders\DemoSeeder;
use Illuminate\Console\Command;

class DemoSimulateBuses extends Command
{
    protected $signature = 'demo:simulate-buses
        {--interval=2 : Segundos entre actualizaciones}
        {--speed=40 : Velocidad simulada en km/h}
        {--ticks=0 : Número de actualizaciones antes de terminar (0 = infinito, Ctrl+C para salir)}';

    protected $description = 'Mueve los buses demo por su ruta (ida y vuelta) publicando posición y rumbo con App\Support\DriverLocation';

    public function handle(): int
    {
        if (! app()->environment('local', 'testing')) {
            $this->error('demo:simulate-buses solo puede ejecutarse en APP_ENV=local|testing.');

            return self::FAILURE;
        }

        $interval = max(0.0, (float) $this->option('interval'));
        $speed = max(1.0, (float) $this->option('speed'));
        $maxTicks = max(0, (int) $this->option('ticks'));

        $plates = collect(DemoSeeder::organizations())->pluck('plates')->flatten()->all();
        $buses = Bus::with('routeWaypoints')->whereIn('plate', $plates)->get();

        $walkers = [];
        foreach ($buses as $i => $bus) {
            $points = $bus->routeWaypoints->map(fn ($w) => [(float) $w->latitude, (float) $w->longitude])->all();
            if (count($points) < 2) {
                continue;
            }
            $walker = new RouteWalker($points);
            // Escalona los buses para que no viajen juntos.
            $walkers[$bus->plate] = ['walker' => $walker, 'distance' => $walker->totalMeters() * (($i % 4) / 4)];
        }

        if ($walkers === []) {
            $this->error('No hay buses demo con ruta. Ejecuta primero: php artisan demo:setup');

            return self::FAILURE;
        }

        $this->info(sprintf('Simulando %d buses a %.0f km/h cada %.1fs. Ctrl+C para detener.', count($walkers), $speed, $interval));

        $step = $speed / 3.6 * ($interval > 0 ? $interval : 1);
        $tick = 0;

        while ($maxTicks === 0 || $tick < $maxTicks) {
            foreach ($walkers as $plate => &$state) {
                $pos = $state['walker']->at($state['distance']);
                DriverLocation::put($plate, $pos['lat'], $pos['lng'], $pos['heading'], (int) round($speed));
                $state['distance'] += $step;
            }
            unset($state);

            $tick++;
            if ($interval > 0 && ($maxTicks === 0 || $tick < $maxTicks)) {
                usleep((int) ($interval * 1_000_000));
            }
        }

        return self::SUCCESS;
    }
}
