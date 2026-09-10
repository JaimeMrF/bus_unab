<?php

namespace App\Filament\Resources;

use App\Filament\Resources\UserResource\Pages;
use App\Models\User;
use Filament\Forms;
use Filament\Forms\Form;
use Filament\Resources\Resource;
use Filament\Tables;
use Filament\Tables\Table;

class UserResource extends Resource
{
    protected static ?string $model = User::class;
    protected static ?string $navigationIcon  = 'heroicon-o-users';
    protected static ?string $navigationLabel = 'Usuarios';
    protected static ?string $modelLabel      = 'Usuario';
    protected static ?string $pluralModelLabel = 'Usuarios';
    protected static ?int    $navigationSort  = 4;

    public static function form(Form $form): Form
    {
        return $form->schema([
            Forms\Components\Section::make()->schema([
                Forms\Components\TextInput::make('name')
                    ->label('Nombre')
                    ->required()
                    ->maxLength(255),

                Forms\Components\TextInput::make('email')
                    ->label('Correo')
                    ->email()
                    ->required()
                    ->unique(ignoreRecord: true),

                // H1/H2 · Roles asignables SEGÚN PANEL (User::assignableRoles es
                // la única fuente de verdad). Filament valida contra las opciones,
                // así que un gerente de /empresa no puede crear admins ni por UI
                // ni manipulando el payload.
                Forms\Components\Select::make('role')
                    ->label('Rol')
                    ->options(fn () => User::assignableRoles(
                        \Filament\Facades\Filament::getCurrentPanel()?->getId()
                    ))
                    ->default(fn () => User::defaultRoleForPanel(
                        \Filament\Facades\Filament::getCurrentPanel()?->getId()
                    ))
                    ->disabled(fn (?User $record) => $record?->id === auth()->id())
                    ->required(),

                Forms\Components\TextInput::make('password')
                    ->label('Contraseña')
                    ->password()
                    ->dehydrated(fn ($state) => filled($state))
                    ->required(fn (string $context) => $context === 'create'),
            ])->columns(2),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                Tables\Columns\ImageColumn::make('avatar')
                    ->label('')
                    ->circular()
                    ->defaultImageUrl(fn ($record) => 'https://ui-avatars.com/api/?name=' . urlencode($record->name) . '&color=7F9CF5&background=EBF4FF'),

                Tables\Columns\TextColumn::make('name')
                    ->label('Nombre')
                    ->searchable()
                    ->sortable(),

                Tables\Columns\TextColumn::make('email')
                    ->label('Correo')
                    ->searchable(),

                Tables\Columns\TextColumn::make('role')
                    ->label('Rol')
                    ->badge()
                    ->color(fn ($state) => match($state) {
                        'admin', 'super_admin'  => 'danger',
                        'tenant_admin'          => 'primary',
                        'driver'                => 'warning',
                        'pasajero', 'student'   => 'gray',
                        default                 => 'gray',
                    })
                    ->formatStateUsing(fn ($state) => match($state) {
                        'admin'       => 'Super Admin (legado)',
                        'super_admin' => 'Super Admin',
                        'tenant_admin'=> 'Admin de Empresa',
                        'driver'      => 'Conductor',
                        'pasajero'    => 'Pasajero',
                        'student'     => 'Pasajero (legado)',
                        default       => $state,
                    }),

                Tables\Columns\TextColumn::make('created_at')
                    ->label('Registro')
                    ->dateTime('d/m/Y')
                    ->sortable(),
            ])
            ->filters([
                Tables\Filters\SelectFilter::make('role')
                    ->label('Rol')
                    ->options([
                        'super_admin'  => 'Super Admin',
                        'tenant_admin' => 'Admin de Empresa',
                        'driver'       => 'Conductor',
                        'pasajero'     => 'Pasajero',
                        'admin'        => 'Super Admin (legado)',
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
            'index'  => Pages\ListUsers::route('/'),
            'create' => Pages\CreateUser::route('/create'),
            'edit'   => Pages\EditUser::route('/{record}/edit'),
        ];
    }
}
