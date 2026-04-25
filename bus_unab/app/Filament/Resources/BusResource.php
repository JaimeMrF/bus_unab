<?php

namespace App\Filament\Resources;

use App\Filament\Resources\BusResource\Pages;
use App\Filament\Resources\BusResource\RelationManagers\StopsRelationManager;
use App\Models\Bus;
use Filament\Forms;
use Filament\Forms\Form;
use Filament\Resources\Resource;
use Filament\Tables;
use Filament\Tables\Table;

class BusResource extends Resource
{
    protected static ?string $model = Bus::class;
    protected static ?string $navigationIcon  = 'heroicon-o-truck';
    protected static ?string $navigationLabel = 'Buses';
    protected static ?string $modelLabel      = 'Bus';
    protected static ?string $pluralModelLabel = 'Buses';
    protected static ?int    $navigationSort  = 1;

    public static function form(Form $form): Form
    {
        return $form->schema([
            Forms\Components\Section::make('Información del Bus')->schema([
                Forms\Components\TextInput::make('name')
                    ->label('Nombre')
                    ->required()
                    ->maxLength(50),

                Forms\Components\TextInput::make('plate')
                    ->label('Placa / Identificador')
                    ->required()
                    ->maxLength(20)
                    ->unique(ignoreRecord: true),

                Forms\Components\TextInput::make('capacity')
                    ->label('Capacidad (pasajeros)')
                    ->required()
                    ->numeric()
                    ->minValue(1)
                    ->maxValue(200)
                    ->default(40),

                Forms\Components\TextInput::make('external_vehicle_id')
                    ->label('ID en gpsmobile.co')
                    ->required()
                    ->numeric()
                    ->unique(ignoreRecord: true),

                Forms\Components\Toggle::make('is_active')
                    ->label('Activo')
                    ->default(true),
            ])->columns(2),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                Tables\Columns\TextColumn::make('name')
                    ->label('Nombre')
                    ->sortable()
                    ->searchable(),

                Tables\Columns\TextColumn::make('plate')
                    ->label('Placa')
                    ->badge()
                    ->color('primary'),

                Tables\Columns\TextColumn::make('capacity')
                    ->label('Capacidad')
                    ->suffix(' pasajeros')
                    ->sortable(),

                Tables\Columns\TextColumn::make('external_vehicle_id')
                    ->label('ID GPS externo')
                    ->color('gray'),

                Tables\Columns\IconColumn::make('is_active')
                    ->label('Activo')
                    ->boolean(),

                Tables\Columns\TextColumn::make('updated_at')
                    ->label('Actualizado')
                    ->dateTime('d/m/Y H:i')
                    ->sortable(),
            ])
            ->filters([
                Tables\Filters\TernaryFilter::make('is_active')->label('Estado'),
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

    public static function getRelationManagers(): array
    {
        return [
            StopsRelationManager::class,
        ];
    }

    public static function getPages(): array
    {
        return [
            'index'  => Pages\ListBuses::route('/'),
            'create' => Pages\CreateBus::route('/create'),
            'edit'   => Pages\EditBus::route('/{record}/edit'),
        ];
    }
}
