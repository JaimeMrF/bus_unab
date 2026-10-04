<?php

namespace App\Filament\Support;

use App\Rules\ContrastsWith;
use App\Rules\HexColor;
use App\Services\BrandingService;
use Filament\Forms;
use Filament\Forms\Get;
use Illuminate\Support\Str;

/**
 * Esquema Filament de branding compartido por el panel /empresa (admin del
 * tenant) y el CRUD de Transportadoras (super admin). Las reglas replican
 * BrandingService::validator() para dar feedback inline.
 */
class BrandingForm
{
    private const POSE_LABELS = [
        'greeting' => 'Saludo (login/splash)',
        'curious' => 'Curiosa (vacíos)',
        'sad' => 'Triste (errores)',
        'waiting' => 'Espera del bus',
        'phone' => 'Celular (QR/wallet)',
        'map' => 'Mapa',
        'driver' => 'Conductor',
        'ok' => 'Confirmación',
        'celebrating' => 'Celebración',
    ];

    private const COLOR_LABELS = [
        'primary' => 'Primario',
        'on_primary' => 'Texto sobre primario',
        'secondary' => 'Secundario',
        'on_secondary' => 'Texto sobre secundario',
        'background' => 'Fondo',
        'surface' => 'Superficie',
        'on_surface' => 'Texto sobre superficie',
        'accent' => 'Acento',
        'success' => 'Éxito',
        'warning' => 'Advertencia',
        'error' => 'Error',
    ];

    /** Pares texto => fondo con contraste mínimo AA. */
    private const CONTRAST_PAIRS = [
        'on_primary' => 'primary',
        'on_secondary' => 'secondary',
        'on_surface' => 'surface',
    ];

    /** @return array<int, Forms\Components\Component> */
    public static function schema(): array
    {
        return [
            Forms\Components\Section::make('Identidad de la app')->schema([
                Forms\Components\TextInput::make('branding.app_name')
                    ->label('Nombre de la app')->required()->maxLength(60),
                Forms\Components\TextInput::make('branding.tagline')
                    ->label('Eslogan')->maxLength(120),
                Forms\Components\TextInput::make('branding.support_email')
                    ->label('Correo de soporte')->email()->maxLength(190),
                Forms\Components\Select::make('branding.font_family')
                    ->label('Tipografía')->required()->default('system')
                    ->options(['poppins' => 'Poppins', 'inter' => 'Inter', 'system' => 'Sistema']),
                Forms\Components\Select::make('branding.corner_radius')
                    ->label('Radio de esquinas')->required()->default('md')
                    ->options(['sm' => 'Pequeño', 'md' => 'Medio', 'lg' => 'Grande']),
            ])->columns(2),

            Forms\Components\Section::make('Imágenes')
                ->description('PNG, WebP o SVG (sin scripts), máximo 1 MB.')
                ->schema([
                    self::image('logo_path', 'Logo'),
                    self::image('logo_dark_path', 'Logo (modo oscuro)'),
                    self::image('icon_path', 'Ícono'),
                    self::image('mascot_path', 'Mascota'),
                ])->columns(2),

            Forms\Components\Section::make('Mascota (poses)')
                ->description('Opcional. Sin pose se usa la mascota general; sin ninguna, el cliente no muestra hueco.')
                ->schema(array_map(
                    fn (string $pose) => self::image("mascot_poses.{$pose}", self::POSE_LABELS[$pose]),
                    BrandingService::MASCOT_POSES
                ))->columns(3)->collapsible()->collapsed(),

            Forms\Components\Section::make('Estilo del bus')
                ->description('Sin colores se usan el primario y el secundario de la marca.')
                ->schema([
                    Forms\Components\ColorPicker::make('bus_style.body')->label('Color de carrocería')->rule(new HexColor),
                    Forms\Components\ColorPicker::make('bus_style.accent')->label('Color de acento')->rule(new HexColor),
                    Forms\Components\Select::make('bus_style.icon')->label('Silueta')->required()->default('classic')
                        ->options(['classic' => 'Clásico', 'modern' => 'Moderno', 'minibus' => 'Minibús']),
                    self::image('bus_style.icon_path', 'Ícono propio (vista superior)'),
                ])->columns(2)->collapsible(),

            Forms\Components\Section::make('Colores — modo claro')
                ->schema(self::colors('light'))->columns(3)->collapsible(),
            Forms\Components\Section::make('Colores — modo oscuro')
                ->schema(self::colors('dark'))->columns(3)->collapsible(),

            Forms\Components\Section::make('Funciones')->schema([
                Forms\Components\Toggle::make('branding.features.qr_payments')->label('Pagos con QR')->default(true),
                Forms\Components\Toggle::make('branding.features.wallet')->label('Billetera')->default(true),
                Forms\Components\Toggle::make('branding.features.driver_mode')->label('Modo conductor')->default(true),
            ])->columns(3),
        ];
    }

    private static function image(string $column, string $label): Forms\Components\FileUpload
    {
        return Forms\Components\FileUpload::make($column)
            ->label($label)
            ->disk('public')
            ->directory('branding')
            ->visibility('public')
            ->acceptedFileTypes(['image/png', 'image/webp', 'image/svg+xml'])
            ->maxSize(1024)
            ->rules(BrandingService::imageRules())
            ->getUploadedFileNameForStorageUsing(
                fn ($file): string => Str::random(40).'.'.($file->guessExtension() ?: 'png')
            );
    }

    /** @return array<int, Forms\Components\Component> */
    private static function colors(string $mode): array
    {
        $fields = [];
        foreach (self::COLOR_LABELS as $key => $label) {
            $field = Forms\Components\ColorPicker::make("branding.colors.$mode.$key")
                ->label($label)
                ->required()
                ->default(BrandingService::defaultColors()[$mode][$key])
                ->rule(new HexColor);

            if ($background = self::CONTRAST_PAIRS[$key] ?? null) {
                $field->rule(fn (Get $get) => new ContrastsWith($get("branding.colors.$mode.$background"), BrandingService::MIN_CONTRAST));
            }

            $fields[] = $field;
        }

        return $fields;
    }
}
