<?php

namespace App\Filament\Resources\WalletResource\RelationManagers;

use App\Models\WalletTransaction;
use Filament\Resources\RelationManagers\RelationManager;
use Filament\Tables;
use Filament\Tables\Table;

/**
 * Ledger APPEND-ONLY (DISEÑO_DB §5): solo lectura. Sin acciones de escritura:
 * las correcciones existen como asientos inversos vía "Ajustar saldo".
 */
class TransactionsRelationManager extends RelationManager
{
    protected static string $relationship = 'transactions';

    protected static ?string $title = 'Asientos del ledger';

    public function isReadOnly(): bool
    {
        return true;
    }

    public function table(Table $table): Table
    {
        return $table
            ->columns([
                Tables\Columns\TextColumn::make('created_at')
                    ->label('Fecha')
                    ->dateTime('d/m/Y H:i')
                    ->sortable(),
                Tables\Columns\TextColumn::make('tipo')
                    ->label('Tipo')
                    ->badge()
                    ->color(fn (string $state): string => match ($state) {
                        WalletTransaction::TIPO_CREDITO => 'success',
                        WalletTransaction::TIPO_DEBITO  => 'danger',
                        default                         => 'warning',
                    }),
                Tables\Columns\TextColumn::make('monto_centavos')
                    ->label('Monto')
                    ->formatStateUsing(fn ($state) => '$ ' . number_format(abs((int) $state) / 100, 0, ',', '.')),
                Tables\Columns\TextColumn::make('balance_after')
                    ->label('Saldo luego')
                    ->formatStateUsing(fn ($state) => '$ ' . number_format(((int) $state) / 100, 0, ',', '.')),
                Tables\Columns\TextColumn::make('reference')
                    ->label('Referencia')
                    ->searchable()
                    ->limit(30),
                Tables\Columns\TextColumn::make('contraparte')
                    ->label('Contraparte')
                    ->placeholder('—'),
            ])
            ->actions([])
            ->bulkActions([])
            ->defaultSort('created_at', 'desc');
    }
}
