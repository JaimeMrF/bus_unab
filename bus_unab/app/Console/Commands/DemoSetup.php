<?php

namespace App\Console\Commands;

use Database\Seeders\DemoSeeder;
use Illuminate\Console\Command;

class DemoSetup extends Command
{
    protected $signature = 'demo:setup {--no-fresh : No recrear la BD; solo (re)ejecutar DemoSeeder, que es idempotente}';

    protected $description = 'Prepara el entorno demo white-label: migrate:fresh --seed + DemoSeeder + storage:link (solo local/testing)';

    public function handle(): int
    {
        if (! app()->environment('local', 'testing')) {
            $this->error('demo:setup solo puede ejecutarse en APP_ENV=local|testing.');

            return self::FAILURE;
        }

        if ($this->option('no-fresh')) {
            $this->call('migrate', ['--force' => true]);
        } else {
            $this->call('migrate:fresh', ['--seed' => true, '--force' => true]);
        }

        $this->call('db:seed', ['--class' => DemoSeeder::class, '--force' => true]);
        if (! file_exists(public_path('storage')) && ! is_link(public_path('storage'))) {
            $this->call('storage:link');
        }

        $this->summary();

        return self::SUCCESS;
    }

    private function summary(): void
    {
        $this->newLine();
        $this->info('Demo lista. Password de todos los usuarios: '.DemoSeeder::PASSWORD);

        $rows = [['super_admin', DemoSeeder::SUPERADMIN_EMAIL, '-', '/admin']];
        foreach (DemoSeeder::organizations() as $slug => $org) {
            $rows[] = ['tenant_admin', "admin.{$slug}@demo.test", $slug, '/empresa'];
            $rows[] = ['driver', "driver.{$slug}@demo.test", $slug, 'app'];
            $rows[] = ['pasajero', "pasajero.{$slug}@demo.test", $slug, 'app'];
        }
        $rows[] = ['pasajero', DemoSeeder::CITY_PASSENGER_EMAIL, '(sin org)', 'app'];

        $this->table(['Rol', 'Email', 'Organización', 'Acceso'], $rows);
        $this->line('Branding: GET /api/v1/branding/{'.implode('|', array_keys(DemoSeeder::organizations())).'}');
        $this->line('Mapa vivo: php artisan demo:simulate-buses');
    }
}
