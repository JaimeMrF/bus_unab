<?php

namespace App\Filament\Widgets;

use Filament\Widgets\Widget;

class GoogleCalendarWidget extends Widget
{
    protected static string $view = 'filament.widgets.google-calendar-widget';

    protected static ?int $sort = 2;

    protected int|string|array $columnSpan = 'full';

    // Sin calendario configurado Google muestra su pantalla de inicio de sesión: mejor ocultarlo.
    public static function canView(): bool
    {
        return filled(config('services.google.calendar_id'));
    }

    public function getCalendarUrl(): string
    {
        $calendarId = config('services.google.calendar_id');

        return 'https://calendar.google.com/calendar/embed?src='.urlencode($calendarId).'&ctz=America/Bogota';
    }
}
