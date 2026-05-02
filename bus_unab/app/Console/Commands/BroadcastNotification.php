<?php

namespace App\Console\Commands;

use App\Services\NotificationService;
use Illuminate\Console\Command;

class BroadcastNotification extends Command
{
    protected $signature = 'notify:broadcast
                            {--title= : Título de la notificación}
                            {--body=  : Cuerpo del mensaje}
                            {--role=all : Rol destino: all, student, driver, admin}
                            {--type=general : Tipo de notificación (para el cliente)}';

    protected $description = 'Envía una notificación push global a todos los dispositivos registrados';

    public function handle(NotificationService $notificationService): int
    {
        $title = $this->option('title') ?: $this->ask('Título de la notificación');
        $body  = $this->option('body')  ?: $this->ask('Cuerpo del mensaje');
        $role  = $this->option('role');
        $type  = $this->option('type');

        $this->info("Enviando notificación...");
        $this->line("  Título : {$title}");
        $this->line("  Cuerpo : {$body}");
        $this->line("  Destino: {$role}");

        $notificationService->broadcast($title, $body, ['type' => $type], $role);

        $this->info('Notificación enviada.');
        return Command::SUCCESS;
    }
}
