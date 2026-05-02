<?php

namespace App\Filament\Resources\BusResource\RelationManagers;

use Filament\Forms;
use Filament\Forms\Form;
use Filament\Resources\RelationManagers\RelationManager;
use Filament\Tables;
use Filament\Tables\Table;
use Illuminate\Support\Facades\Cache;

class RouteWaypointsRelationManager extends RelationManager
{
    protected static string $relationship  = 'routeWaypoints';
    protected static ?string $title        = 'Puntos de ruta (calles específicas)';
    protected static ?string $modelLabel   = 'punto';
    protected static ?string $pluralModelLabel = 'puntos';
    protected static bool $shouldSkipAuthorization = true;

    public function form(Form $form): Form
    {
        return $form->schema([
            Forms\Components\TextInput::make('order')
                ->label('Orden')
                ->required()
                ->numeric()
                ->minValue(1)
                ->helperText('Debe ser un valor ENTRE las paradas que conecta. Parada 1→2: usa 101–199. Parada 2→3: usa 201–299. Ejemplo: si el punto va entre parada 1 y 2, pon 150.'),

            Forms\Components\TextInput::make('label')
                ->label('Referencia (opcional)')
                ->maxLength(100)
                ->placeholder('Ej: Cra 33 con Calle 45'),

            Forms\Components\TextInput::make('latitude')
                ->label('Latitud')
                ->required()
                ->numeric()
                ->helperText('Clic derecho en Google Maps → "¿Qué hay aquí?" para obtener coordenadas'),

            Forms\Components\TextInput::make('longitude')
                ->label('Longitud')
                ->required()
                ->numeric(),
        ])->columns(2);
    }

    public function table(Table $table): Table
    {
        return $table
            ->recordTitleAttribute('label')
            ->defaultSort('order')
            ->columns([
                Tables\Columns\TextColumn::make('order')
                    ->label('#')
                    ->sortable()
                    ->alignCenter()
                    ->width('50px'),

                Tables\Columns\TextColumn::make('label')
                    ->label('Referencia')
                    ->placeholder('Sin nombre')
                    ->searchable(),

                Tables\Columns\TextColumn::make('latitude')
                    ->label('Latitud')
                    ->numeric(decimalPlaces: 6),

                Tables\Columns\TextColumn::make('longitude')
                    ->label('Longitud')
                    ->numeric(decimalPlaces: 6),
            ])
            ->headerActions([
                Tables\Actions\CreateAction::make()
                    ->after(fn () => Cache::forget('bus_route_polyline_' . $this->getOwnerRecord()->plate)),
            ])
            ->actions([
                Tables\Actions\EditAction::make()
                    ->after(fn () => Cache::forget('bus_route_polyline_' . $this->getOwnerRecord()->plate)),
                Tables\Actions\DeleteAction::make()
                    ->after(fn () => Cache::forget('bus_route_polyline_' . $this->getOwnerRecord()->plate)),
            ])
            ->bulkActions([
                Tables\Actions\BulkActionGroup::make([
                    Tables\Actions\DeleteBulkAction::make()
                        ->after(fn () => Cache::forget('bus_route_polyline_' . $this->getOwnerRecord()->plate)),
                ]),
            ]);
    }
}
