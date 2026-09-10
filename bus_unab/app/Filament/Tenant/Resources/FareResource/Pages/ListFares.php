<?php

namespace App\Filament\Tenant\Resources\FareResource\Pages;

use App\Filament\Tenant\Resources\FareResource;
use Filament\Actions;
use Filament\Resources\Pages\ListRecords;

class ListFares extends ListRecords
{
    protected static string $resource = FareResource::class;

    protected function getHeaderActions(): array
    {
        return [Actions\CreateAction::make()->label('Nueva Tarifa')];
    }
}
