<?php

namespace App\Filament\Resources;

use App\Filament\Resources\WalletResource\Pages;
use App\Filament\Resources\WalletResource\RelationManagers\TransactionsRelationManager;
use App\Models\Wallet;
use App\Services\WalletService;
use Filament\Forms;
use Filament\Infolists;
use Filament\Infolists\Infolist;
use Filament\Notifications\Notification;
use Filament\Resources\Resource;
use Filament\Tables;
use Filament\Tables\Table;
use Illuminate\Support\Str;

/**
 * M3 · S3.3.2 — Wallets del pasajero: panel Super Admin, LIST + VIEW ONLY.
 *
 * Regla DISEÑO_DB §5: el ledger es append-only; el saldo SOLO cambia con
 * asientos. Por eso: sin páginas create/edit, y el único write permitido
 * desde la UI es la acción "Ajustar saldo" → WalletService::adjust() con
 * reference única 'admin_adjust-{uuid}' (idempotencia en el servicio).
 */
class WalletResource extends Resource
{
    protected static ?string $model = Wallet::class;
    protected static ?string $navigationIcon  = 'heroicon-o-wallet';
    protected static ?string $navigationLabel = 'Wallets';
    protected static ?string $modelLabel      = 'Wallet';
    protected static ?string $pluralModelLabel = 'Wallets';
    protected static ?string $navigationGroup = 'SaaS';
    protected static ?int    $navigationSort  = 2;

    public static function canCreate(): bool
    {
        // La wallet se crea sola (Wallet::para($user)) al primer uso.
        return false;
    }

    public static function infolist(Infolist $infolist): Infolist
    {
        return $infolist->schema([
            Infolists\Components\Section::make('Wallet')->schema([
                Infolists\Components\TextEntry::make('user.name')->label('Pasajero'),
                Infolists\Components\TextEntry::make('user.email')->label('Correo'),
                Infolists\Components\TextEntry::make('balance_centavos')
                    ->label('Saldo')
                    ->formatStateUsing(fn ($state) => '$ ' . number_format(((int) $state) / 100, 0, ',', '.') . ' COP'),
                Infolists\Components\TextEntry::make('estado')
                    ->label('Estado')
                    ->badge()
                    ->color(fn ($state) => $state === Wallet::ESTADO_ACTIVA ? 'success' : 'danger'),
                Infolists\Components\TextEntry::make('version')->label('Versión (optimistic lock)'),
                Infolists\Components\TextEntry::make('transportadora_id')
                    ->label('Tenant (id)')
                    ->placeholder('— (ciudad/plataforma)'),
                Infolists\Components\TextEntry::make('created_at')->label('Creada')->dateTime('d/m/Y H:i'),
            ])->columns(2),
        ]);
    }

    public static function form(Forms\Form $form): Forms\Form
    {
        // Solo lectura: el ajuste manual vive en la acción de tabla.
        return $form->schema([]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                Tables\Columns\TextColumn::make('user.name')
                    ->label('Pasajero')
                    ->searchable()
                    ->sortable(),

                Tables\Columns\TextColumn::make('user.email')
                    ->label('Correo')
                    ->searchable(),

                Tables\Columns\TextColumn::make('balance_centavos')
                    ->label('Saldo')
                    ->formatStateUsing(fn ($state) => '$ ' . number_format(((int) $state) / 100, 0, ',', '.'))
                    ->sortable(),

                Tables\Columns\TextColumn::make('estado')
                    ->label('Tipo/Estado')
                    ->badge()
                    ->color(fn ($state) => $state === Wallet::ESTADO_ACTIVA ? 'success' : 'danger')
                    ->formatStateUsing(fn ($state) => $state === Wallet::ESTADO_ACTIVA ? 'Activa' : 'Congelada'),

                Tables\Columns\TextColumn::make('transactions_count')
                    ->label('Asientos')
                    ->counts('transactions'),

                Tables\Columns\TextColumn::make('updated_at')
                    ->label('Movida')
                    ->dateTime('d/m/Y H:i')
                    ->sortable(),
            ])
            ->filters([
                Tables\Filters\SelectFilter::make('estado')
                    ->label('Estado')
                    ->options([
                        Wallet::ESTADO_ACTIVA     => 'Activa',
                        Wallet::ESTADO_CONGELADA  => 'Congelada',
                    ]),
            ])
            ->actions([
                Tables\Actions\ViewAction::make(),

                Tables\Actions\Action::make('adjust')
                    ->label('Ajustar saldo')
                    ->icon('heroicon-o-plus-minus')
                    ->color('warning')
                    ->form([
                        Forms\Components\TextInput::make('monto_pesos')
                            ->label('Monto (pesos COP)')
                            ->numeric()
                            ->required()
                            ->helperText('Positivo acredita, negativo debita. Ej: 5000 o -2850'),
                        Forms\Components\TextInput::make('motivo')
                            ->label('Motivo')
                            ->required()
                            ->maxLength(190),
                    ])
                    ->action(function (Wallet $record, array $data): void {
                        $centavos = (int) round(((float) $data['monto_pesos']) * 100);

                        try {
                            $tx = app(WalletService::class)->adjust(
                                $record,
                                $centavos,
                                'admin_adjust-' . Str::uuid()->toString(),
                                (string) $data['motivo'],
                                auth()->user()?->email,
                            );
                        } catch (\Throwable $e) {
                            Notification::make()
                                ->danger()
                                ->title('Ajuste rechazado')
                                ->body($e->getMessage())
                                ->send();

                            return;
                        }

                        Notification::make()
                            ->success()
                            ->title('Ajuste aplicado')
                            ->body('Nuevo saldo: $ ' . number_format($tx->balance_after / 100, 0, ',', '.') . ' COP')
                            ->send();
                    }),
            ])
            ->bulkActions([]);
    }

    public static function getRelations(): array
    {
        return [
            TransactionsRelationManager::class,
        ];
    }

    public static function getPages(): array
    {
        return [
            'index' => Pages\ListWallets::route('/'),
            'view'  => Pages\ViewWallet::route('/{record}'),
        ];
    }
}
