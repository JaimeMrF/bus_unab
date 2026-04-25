<?php

namespace App\Filament\Resources\PointOfInterestResource\Pages;

use App\Filament\Resources\PointOfInterestResource;
use Filament\Actions;
use Filament\Resources\Pages\EditRecord;

class EditPointOfInterest extends EditRecord
{
    protected static string $resource = PointOfInterestResource::class;

    protected function getHeaderActions(): array
    {
        return [Actions\DeleteAction::make()];
    }
}
