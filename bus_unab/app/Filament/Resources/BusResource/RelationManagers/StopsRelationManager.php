<?php

namespace App\Filament\Resources\BusResource\RelationManagers;

use Filament\Forms;
use Filament\Forms\Form;
use Filament\Resources\RelationManagers\RelationManager;
use Filament\Tables;
use Filament\Tables\Table;

class StopsRelationManager extends RelationManager
{
    protected static string $relationship = 'stops';
    protected static ?string $title = 'Paradas de la ruta';
    protected static ?string $modelLabel = 'parada';
    protected static ?string $pluralModelLabel = 'paradas';

    // Usado por EditAction para editar los campos del pivot
    public function form(Form $form): Form
    {
        return $form->schema([
            Forms\Components\TextInput::make('order')
                ->label('Orden en la ruta')
                ->required()
                ->numeric()
                ->minValue(1),

            Forms\Components\TextInput::make('estimated_minutes')
                ->label('Minutos desde el inicio')
                ->required()
                ->numeric()
                ->minValue(0)
                ->suffix('min'),
        ]);
    }

    public function table(Table $table): Table
    {
        return $table
            ->recordTitleAttribute('name')
            ->columns([
                Tables\Columns\TextColumn::make('pivot.order')
                    ->label('#')
                    ->alignCenter()
                    ->width('60px'),

                Tables\Columns\TextColumn::make('name')
                    ->label('Parada')
                    ->searchable(),

                Tables\Columns\TextColumn::make('address')
                    ->label('Dirección')
                    ->color('gray')
                    ->limit(40),

                Tables\Columns\TextColumn::make('pivot.estimated_minutes')
                    ->label('Min. desde inicio')
                    ->suffix(' min')
                    ->alignCenter(),
            ])
            ->headerActions([
                Tables\Actions\AttachAction::make()
                    ->label('Agregar parada')
                    ->preloadRecordSelect()
                    ->form(fn (Tables\Actions\AttachAction $action): array => [
                        $action->getRecordSelect()
                            ->label('Parada')
                            ->searchable()
                            ->required(),

                        Forms\Components\TextInput::make('order')
                            ->label('Orden en la ruta')
                            ->required()
                            ->numeric()
                            ->minValue(1)
                            ->default(1),

                        Forms\Components\TextInput::make('estimated_minutes')
                            ->label('Minutos desde inicio de ruta')
                            ->required()
                            ->numeric()
                            ->minValue(0)
                            ->default(0)
                            ->suffix('min'),
                    ]),
            ])
            ->actions([
                Tables\Actions\EditAction::make()
                    ->label('Editar'),
                Tables\Actions\DetachAction::make()
                    ->label('Quitar'),
            ])
            ->bulkActions([
                Tables\Actions\DetachBulkAction::make()
                    ->label('Quitar seleccionadas'),
            ]);
    }
}
