<?php

namespace App\Filament\Resources\WalletResource\Pages;

use App\Filament\Resources\WalletResource;
use Filament\Resources\Pages\ListRecords;

class ListWallets extends ListRecords
{
    protected static string $resource = WalletResource::class;

    // Sin CreateAction: la wallet se crea sola (Wallet::para($user)) y el
    // ledger es append-only; los ajustes van por acción de tabla.
}
