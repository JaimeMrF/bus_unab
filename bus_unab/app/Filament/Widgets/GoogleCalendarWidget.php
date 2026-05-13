<?php

namespace App\Filament\Widgets;

use Filament\Widgets\Widget;

class GoogleCalendarWidget extends Widget
{
    protected static string $view = 'filament.widgets.google-calendar-widget';

    protected static ?int $sort = 2;

    protected int | string | array $columnSpan = 'full';

    public function getCalendarUrl(): string
    {
        $calendarId = config('services.google.calendar_id', env('GOOGLE_CALENDAR_ID', 'primary'));
        $apiKey = config('services.maps.key');
        
        return "https://calendar.google.com/calendar/embed?src=" . urlencode($calendarId) . "&ctz=America/Bogota";
    }
}
