<?php

namespace App\Filament\Tenant\Resources\FareResource\Pages;

use App\Filament\Tenant\Resources\FareResource;
use Filament\Actions;
use Filament\Resources\Pages\EditRecord;

class EditFare extends EditRecord
{
    protected static string $resource = FareResource::class;

    protected function getHeaderActions(): array
    {
        return [Actions\DeleteAction::make()];
    }
}
