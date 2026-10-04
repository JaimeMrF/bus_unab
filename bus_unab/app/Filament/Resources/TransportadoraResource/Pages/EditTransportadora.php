<?php

namespace App\Filament\Resources\TransportadoraResource\Pages;

use App\Filament\Resources\TransportadoraResource;
use App\Services\BrandingService;
use Filament\Actions;
use Filament\Resources\Pages\EditRecord;

class EditTransportadora extends EditRecord
{
    protected static string $resource = TransportadoraResource::class;

    /** Tenants sin branding guardado arrancan con los defaults neutros en el form. */
    protected function mutateFormDataBeforeFill(array $data): array
    {
        $data['branding'] = array_replace_recursive([
            'app_name' => $data['nombre'] ?? '',
            'font_family' => 'system',
            'corner_radius' => 'md',
            'features' => BrandingService::defaultFeatures(),
            'colors' => BrandingService::defaultColors(),
        ], $data['branding'] ?? []);

        $data['bus_style'] = array_replace(['icon' => 'classic'], $data['bus_style'] ?? []);

        return $data;
    }

    protected function getHeaderActions(): array
    {
        return [Actions\DeleteAction::make()];
    }
}
