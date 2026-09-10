<?php

namespace App\Filament\Tenant\Resources;

use App\Filament\Tenant\Resources\FareResource\Pages;
use App\Models\Fare;
use Filament\Forms;
use Filament\Resources\Resource;
use Filament\Tables;
use Filament\Tables\Table;

/**
 * M3 · S3.3.2 — Tarifas del TENANT (panel /empresa).
 *
 * Vive en App\Filament\Tenant\Resources (fuera del discoverResources del
 * panel admin, que solo escanea app/Filament/Resources) → SOLO existe en el
 * panel empresa, registrado explícitamente en TenantPanelProvider.
 *
 * Aislamiento: NO hay campo transportadora_id en el form. Las creaciones se
 * auto-asignan al tenant por el hook `creating` de BelongsToTenant, gracias a
 * que EnsureTenantScope (authMiddleware del panel) envuelve la request en
 * TenantContext::run(). Las lecturas ya vienen filtradas por GlobalTenantScope.
 */
class FareResource extends Resource
{
    protected static ?string $model = Fare::class;
    protected static ?string $navigationIcon  = 'heroicon-o-ticket';
    protected static ?string $navigationLabel = 'Tarifas';
    protected static ?string $modelLabel      = 'Tarifa';
    protected static ?string $pluralModelLabel = 'Tarifas';
    protected static ?string $navigationGroup = 'Operación';
    protected static ?int    $navigationSort  = 3;

    public static function getEloquentQuery(): \Illuminate\Database\Eloquent\Builder
    {
        // Fare NO tiene GlobalTenantScope (sin trait BelongsToTenant) → la
        // lectura se filtra a mano por el contexto que fija EnsureTenantScope.
        return parent::getEloquentQuery()
            ->where('transportadora_id', \App\Models\Concerns\TenantContext::id());
    }

    public static function form(Forms\Form $form): Forms\Form
    {
        return $form->schema([
            Forms\Components\Section::make('Tarifa')->schema([
                Forms\Components\TextInput::make('codigo')
                    ->label('Código')
                    ->required()
                    ->maxLength(30)
                    ->unique(ignoreRecord: true)
                    ->helperText('Identificador interno. Ej: URB-ORD'),

                Forms\Components\TextInput::make('nombre')
                    ->label('Nombre')
                    ->required()
                    ->maxLength(120),

                // La BD guarda centavos (BIGINT). La UI trabaja en pesos:
                // hydrate divide /100, dehydrate multiplica /100 → sin floats
                // sueltos en el ledger.
                Forms\Components\TextInput::make('monto_centavos')
                    ->label('Monto (pesos COP)')
                    ->numeric()
                    ->required()
                    ->minValue(1)
                    ->prefix('$')
                    ->afterStateHydrated(function (Forms\Components\TextInput $component, $state): void {
                        if ($state !== null) {
                            $component->state(((int) $state) / 100);
                        }
                    })
                    ->dehydrateStateUsing(fn ($state) => (int) round(((float) $state) * 100)),

                Forms\Components\Select::make('ruta_id')
                    ->label('Ruta (opcional)')
                    ->numeric()
                    ->placeholder('Sin ruta específica')
                    ->helperText('ID numérico de la ruta; vacío = tarifa general del tenant.'),
            ])->columns(2),

            Forms\Components\Section::make('Vigencia')->schema([
                Forms\Components\DateTimePicker::make('vigente_desde')
                    ->label('Vigente desde')
                    ->native(false)
                    ->displayFormat('d/m/Y H:i'),

                Forms\Components\DateTimePicker::make('vigente_hasta')
                    ->label('Vigente hasta')
                    ->native(false)
                    ->displayFormat('d/m/Y H:i')
                    ->after('vigente_desde'),

                Forms\Components\Toggle::make('activa')
                    ->label('Activa')
                    ->default(true)
                    ->helperText('Tarifa candidate para el cobro QR.'),
            ])->columns(3),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                Tables\Columns\TextColumn::make('codigo')
                    ->label('Código')
                    ->badge()
                    ->searchable(),

                Tables\Columns\TextColumn::make('nombre')
                    ->label('Nombre')
                    ->searchable()
                    ->sortable(),

                Tables\Columns\TextColumn::make('monto_centavos')
                    ->label('Monto')
                    ->formatStateUsing(fn ($state) => '$ ' . number_format(((int) $state) / 100, 0, ',', '.') . ' COP')
                    ->sortable(),

                Tables\Columns\TextColumn::make('ruta_id')
                    ->label('Ruta')
                    ->placeholder('General')
                    ->sortable(),

                Tables\Columns\IconColumn::make('activa')
                    ->label('Activa')
                    ->boolean()
                    ->sortable(),

                Tables\Columns\TextColumn::make('vigencia')
                    ->label('Vigencia')
                    ->getStateUsing(fn (Fare $r): string => trim(
                        ($r->vigente_desde?->format('d/m/Y') ?? '—') . ' → ' . ($r->vigente_hasta?->format('d/m/Y') ?? '∞')
                    ))
                    ->badge()
                    ->color('gray'),
            ])
            ->defaultSort('codigo')
            ->filters([
                Tables\Filters\TernaryFilter::make('activa')->label('Activa'),
            ])
            ->actions([
                Tables\Actions\EditAction::make(),
                Tables\Actions\DeleteAction::make(),
            ])
            ->bulkActions([
                Tables\Actions\BulkActionGroup::make([
                    Tables\Actions\DeleteBulkAction::make(),
                ]),
            ]);
    }

    public static function getPages(): array
    {
        return [
            'index'  => Pages\ListFares::route('/'),
            'create' => Pages\CreateFare::route('/create'),
            'edit'   => Pages\EditFare::route('/{record}/edit'),
        ];
    }
}
