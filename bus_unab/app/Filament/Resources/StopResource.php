<?php

namespace App\Filament\Resources;

use App\Filament\Resources\StopResource\Pages;
use App\Models\Stop;
use Filament\Forms;
use Filament\Forms\Form;
use Filament\Resources\Resource;
use Filament\Tables;
use Filament\Tables\Table;

class StopResource extends Resource
{
    protected static ?string $model = Stop::class;
    protected static ?string $navigationIcon  = 'heroicon-o-map-pin';
    protected static ?string $navigationLabel = 'Paradas';
    protected static ?string $modelLabel      = 'Parada';
    protected static ?string $pluralModelLabel = 'Paradas';
    protected static ?int    $navigationSort  = 2;

    public static function form(Form $form): Form
    {
        return $form->schema([
            Forms\Components\Section::make('Información de la Parada')->schema([
                Forms\Components\TextInput::make('name')
                    ->label('Nombre')
                    ->required()
                    ->maxLength(150)
                    ->columnSpanFull(),

                Forms\Components\TextInput::make('address')
                    ->label('Dirección')
                    ->maxLength(255)
                    ->columnSpanFull(),

                Forms\Components\View::make('filament.forms.components.map-picker')
                    ->columnSpanFull(),

                Forms\Components\TextInput::make('latitude')
                    ->label('Latitud')
                    ->required()
                    ->numeric()
                    ->step(0.0000001),

                Forms\Components\TextInput::make('longitude')
                    ->label('Longitud')
                    ->required()
                    ->numeric()
                    ->step(0.0000001),

                Forms\Components\TextInput::make('radius_meters')
                    ->label('Radio de detección (metros)')
                    ->required()
                    ->numeric()
                    ->default(100)
                    ->suffix('m'),

                Forms\Components\Toggle::make('is_active')
                    ->label('Activa')
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
                    ->searchable()
                    ->sortable(),

                Tables\Columns\TextColumn::make('address')
                    ->label('Dirección')
                    ->limit(50)
                    ->color('gray'),

                Tables\Columns\TextColumn::make('latitude')
                    ->label('Latitud')
                    ->numeric(7),

                Tables\Columns\TextColumn::make('longitude')
                    ->label('Longitud')
                    ->numeric(7),

                Tables\Columns\TextColumn::make('radius_meters')
                    ->label('Radio')
                    ->suffix(' m'),

                Tables\Columns\IconColumn::make('is_active')
                    ->label('Activa')
                    ->boolean(),
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

    public static function getPages(): array
    {
        return [
            'index'  => Pages\ListStops::route('/'),
            'create' => Pages\CreateStop::route('/create'),
            'edit'   => Pages\EditStop::route('/{record}/edit'),
        ];
    }
}
