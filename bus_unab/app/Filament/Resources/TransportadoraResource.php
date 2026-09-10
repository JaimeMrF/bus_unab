<?php

namespace App\Filament\Resources;

use App\Filament\Resources\TransportadoraResource\Pages;
use App\Models\Transportadora;
use Filament\Forms;
use Filament\Forms\Form;
use Filament\Resources\Resource;
use Filament\Tables;
use Filament\Tables\Table;

/**
 * M3 · S3.3.2 — CRUD de TENANTS (Super Admin, panel /admin).
 *
 * Vive en app/Filament/Resources → lo recoge discoverResources del admin.
 * NO se registra en TenantPanelProvider (allí no hay discovery) → invisible
 * para los tenant_admins, que es exactamente la intención.
 */
class TransportadoraResource extends Resource
{
    protected static ?string $model = Transportadora::class;
    protected static ?string $navigationIcon  = 'heroicon-o-building-office-2';
    protected static ?string $navigationLabel = 'Transportadoras';
    protected static ?string $modelLabel      = 'Transportadora';
    protected static ?string $pluralModelLabel = 'Transportadoras';
    protected static ?string $navigationGroup = 'SaaS';
    protected static ?int    $navigationSort  = 1;

    public static function form(Form $form): Form
    {
        return $form->schema([
            Forms\Components\Section::make('Identidad')->schema([
                Forms\Components\TextInput::make('nombre')
                    ->label('Nombre comercial')
                    ->required()
                    ->maxLength(120),

                Forms\Components\TextInput::make('slug')
                    ->label('Slug (URL del panel)')
                    ->required()
                    ->maxLength(60)
                    ->alphaDash()
                    ->unique(ignoreRecord: true)
                    ->helperText('minúsculas, guiones. Ej: metromantica'),

                Forms\Components\TextInput::make('razon_social')
                    ->label('Razón social')
                    ->maxLength(160),

                Forms\Components\TextInput::make('nit')
                    ->label('NIT')
                    ->maxLength(20)
                    ->unique(ignoreRecord: true),
            ])->columns(2),

            Forms\Components\Section::make('Contacto')->schema([
                Forms\Components\TextInput::make('contacto_nombre')
                    ->label('Nombre de contacto')
                    ->maxLength(120),

                Forms\Components\TextInput::make('contacto_email')
                    ->label('Correo de contacto')
                    ->email()
                    ->maxLength(190),

                Forms\Components\TextInput::make('contacto_telefono')
                    ->label('Teléfono')
                    ->tel()
                    ->maxLength(30),
            ])->columns(2),

            Forms\Components\Section::make('SaaS')->schema([
                Forms\Components\Select::make('plan')
                    ->label('Plan')
                    ->options([
                        'trial'      => 'Trial',
                        'basico'     => 'Básico',
                        'pro'        => 'Pro',
                        'enterprise' => 'Enterprise',
                    ])
                    ->default('basico')
                    ->required(),

                Forms\Components\Toggle::make('activo')
                    ->label('Activa (kill-switch)')
                    ->default(true)
                    ->helperText('Desactivarla bloquea la operación del tenant.'),
            ])->columns(2),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                Tables\Columns\TextColumn::make('nombre')
                    ->label('Nombre')
                    ->searchable()
                    ->sortable(),

                Tables\Columns\TextColumn::make('slug')
                    ->label('Slug')
                    ->badge()
                    ->color('gray')
                    ->searchable(),

                Tables\Columns\TextColumn::make('plan')
                    ->label('Plan')
                    ->badge()
                    ->color(fn ($state) => match ($state) {
                        'enterprise' => 'primary',
                        'pro'        => 'info',
                        'basico'     => 'success',
                        default      => 'gray',
                    })
                    ->formatStateUsing(fn ($state) => strtoupper((string) $state)),

                Tables\Columns\TextColumn::make('buses_count')
                    ->label('Buses')
                    ->counts('buses')
                    ->sortable(),

                Tables\Columns\TextColumn::make('users_count')
                    ->label('Usuarios')
                    ->counts('users')
                    ->sortable(),

                Tables\Columns\IconColumn::make('activo')
                    ->label('Activa')
                    ->boolean()
                    ->sortable(),

                Tables\Columns\TextColumn::make('created_at')
                    ->label('Alta')
                    ->dateTime('d/m/Y')
                    ->sortable()
                    ->toggleable(isToggledHiddenByDefault: true),
            ])
            ->filters([
                Tables\Filters\TernaryFilter::make('activo')->label('Estado'),
                Tables\Filters\SelectFilter::make('plan')
                    ->label('Plan')
                    ->options([
                        'trial'      => 'Trial',
                        'basico'     => 'Básico',
                        'pro'        => 'Pro',
                        'enterprise' => 'Enterprise',
                    ]),
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
            'index'  => Pages\ListTransportadoras::route('/'),
            'create' => Pages\CreateTransportadora::route('/create'),
            'edit'   => Pages\EditTransportadora::route('/{record}/edit'),
        ];
    }
}
