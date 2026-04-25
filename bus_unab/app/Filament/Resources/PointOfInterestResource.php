<?php

namespace App\Filament\Resources;

use App\Filament\Resources\PointOfInterestResource\Pages;
use App\Models\PointOfInterest;
use Filament\Forms;
use Filament\Forms\Form;
use Filament\Resources\Resource;
use Filament\Tables;
use Filament\Tables\Table;

class PointOfInterestResource extends Resource
{
    protected static ?string $model = PointOfInterest::class;
    protected static ?string $navigationIcon  = 'heroicon-o-star';
    protected static ?string $navigationLabel = 'Puntos de Interés';
    protected static ?string $modelLabel      = 'Punto de Interés';
    protected static ?string $pluralModelLabel = 'Puntos de Interés';
    protected static ?int    $navigationSort  = 3;

    public static function form(Form $form): Form
    {
        return $form->schema([
            Forms\Components\Section::make()->schema([
                Forms\Components\TextInput::make('name')
                    ->label('Nombre')
                    ->required()
                    ->maxLength(150)
                    ->columnSpanFull(),

                Forms\Components\Textarea::make('description')
                    ->label('Descripción')
                    ->rows(3)
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

                Forms\Components\Select::make('category')
                    ->label('Categoría')
                    ->options([
                        'campus'    => 'Campus UNAB',
                        'parking'   => 'Parqueadero',
                        'food'      => 'Alimentación',
                        'health'    => 'Salud',
                        'transport' => 'Transporte',
                        'other'     => 'Otro',
                    ])
                    ->required()
                    ->default('other'),

                Forms\Components\TextInput::make('icon')
                    ->label('Ícono (nombre en app)')
                    ->maxLength(50)
                    ->placeholder('ej: building, parking, food'),

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
                    ->searchable()
                    ->sortable(),

                Tables\Columns\BadgeColumn::make('category')
                    ->label('Categoría')
                    ->colors([
                        'primary' => 'campus',
                        'warning' => 'parking',
                        'success' => 'food',
                        'danger'  => 'health',
                        'info'    => 'transport',
                        'gray'    => 'other',
                    ]),

                Tables\Columns\TextColumn::make('latitude')->label('Lat')->numeric(5),
                Tables\Columns\TextColumn::make('longitude')->label('Lng')->numeric(5),

                Tables\Columns\IconColumn::make('is_active')
                    ->label('Activo')
                    ->boolean(),
            ])
            ->filters([
                Tables\Filters\SelectFilter::make('category')
                    ->label('Categoría')
                    ->options([
                        'campus'    => 'Campus UNAB',
                        'parking'   => 'Parqueadero',
                        'food'      => 'Alimentación',
                        'health'    => 'Salud',
                        'transport' => 'Transporte',
                        'other'     => 'Otro',
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
            'index'  => Pages\ListPointsOfInterest::route('/'),
            'create' => Pages\CreatePointOfInterest::route('/create'),
            'edit'   => Pages\EditPointOfInterest::route('/{record}/edit'),
        ];
    }
}
