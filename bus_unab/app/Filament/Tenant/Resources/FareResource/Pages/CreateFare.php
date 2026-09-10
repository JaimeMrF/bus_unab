<?php

namespace App\Filament\Tenant\Resources\FareResource\Pages;

use App\Filament\Tenant\Resources\FareResource;
use App\Models\Concerns\TenantContext;
use Filament\Resources\Pages\CreateRecord;

/**
 * Fare NO usa el trait BelongsToTenant (ground truth: grep en app/Models/Fare.php)
 * → no hay hook `creating` que auto-asigne. La asignación del tenant es explícita
 * aquí, tomando el contexto activado por EnsureTenantScope en el panel /empresa.
 */
class CreateFare extends CreateRecord
{
    protected static string $resource = FareResource::class;

    protected function mutateFormDataBeforeCreate(array $data): array
    {
        $data['transportadora_id'] = TenantContext::id();

        return $data;
    }
}
