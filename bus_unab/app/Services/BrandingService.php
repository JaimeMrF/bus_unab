<?php

namespace App\Services;

use App\Models\Transportadora;
use App\Rules\ContrastsWith;
use App\Rules\HexColor;
use App\Support\ColorContrast;
use Illuminate\Contracts\Validation\Validator as ValidatorContract;
use Illuminate\Support\Arr;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Facades\Validator;
use Illuminate\Validation\Rule;

/**
 * White-label — construye el perfil de branding público de un tenant
 * (defaults neutros + overrides del tenant) y valida ediciones.
 */
class BrandingService
{
    public const COLOR_KEYS = [
        'primary', 'on_primary', 'secondary', 'on_secondary', 'background',
        'surface', 'on_surface', 'accent', 'success', 'warning', 'error',
    ];

    public const FONTS = ['poppins', 'inter', 'system'];

    public const RADII = ['sm', 'md', 'lg'];

    public const FEATURES = ['qr_payments', 'wallet', 'driver_mode'];

    public const MIN_CONTRAST = 4.5;

    public const IMAGE_COLUMNS = [
        'logo_url' => 'logo_path',
        'logo_dark_url' => 'logo_dark_path',
        'icon_url' => 'icon_path',
        'mascot_url' => 'mascot_path',
    ];

    /** Pares texto/fondo que deben cumplir WCAG AA en ambos modos. */
    private const CONTRAST_PAIRS = [
        ['on_primary', 'primary'],
        ['on_secondary', 'secondary'],
        ['on_surface', 'surface'],
    ];

    /** Paleta neutra (sin marca) usada cuando el tenant no define un color. */
    public static function defaultColors(): array
    {
        return [
            'light' => [
                'primary' => '#2563EB', 'on_primary' => '#FFFFFF',
                'secondary' => '#475569', 'on_secondary' => '#FFFFFF',
                'background' => '#F8FAFC', 'surface' => '#FFFFFF', 'on_surface' => '#0F172A',
                'accent' => '#0EA5E9', 'success' => '#15803D', 'warning' => '#B45309', 'error' => '#B91C1C',
            ],
            'dark' => [
                'primary' => '#60A5FA', 'on_primary' => '#0B1220',
                'secondary' => '#94A3B8', 'on_secondary' => '#0B1220',
                'background' => '#0B1220', 'surface' => '#111827', 'on_surface' => '#E5E7EB',
                'accent' => '#38BDF8', 'success' => '#4ADE80', 'warning' => '#FBBF24', 'error' => '#F87171',
            ],
        ];
    }

    public static function defaultFeatures(): array
    {
        return ['qr_payments' => true, 'wallet' => true, 'driver_mode' => true];
    }

    /** Contrato público GET /api/v1/branding/{slug}. */
    public function payload(Transportadora $t): array
    {
        $b = $t->branding ?? [];
        $defaults = self::defaultColors();

        $colors = [];
        foreach (['light', 'dark'] as $mode) {
            foreach (self::COLOR_KEYS as $key) {
                $value = Arr::get($b, "colors.$mode.$key");
                $colors[$mode][$key] = ColorContrast::isHex($value)
                    ? strtoupper($value)
                    : $defaults[$mode][$key];
            }
        }

        $features = self::defaultFeatures();
        foreach (self::FEATURES as $f) {
            $value = Arr::get($b, "features.$f");
            if (is_bool($value)) {
                $features[$f] = $value;
            }
        }

        $font = Arr::get($b, 'font_family');
        $radius = Arr::get($b, 'corner_radius');

        $data = [
            'slug' => $t->slug,
            'app_name' => $this->text(Arr::get($b, 'app_name')) ?? $t->nombre,
            'tagline' => $this->text(Arr::get($b, 'tagline')),
            'support_email' => $this->text(Arr::get($b, 'support_email')) ?? $t->contacto_email,
        ];

        foreach (self::IMAGE_COLUMNS as $field => $column) {
            $data[$field] = $this->url($t->{$column});
        }

        return $data + [
            'colors' => $colors,
            'font_family' => in_array($font, self::FONTS, true) ? $font : 'system',
            'corner_radius' => in_array($radius, self::RADII, true) ? $radius : 'md',
            'features' => $features,
            'version' => (int) $t->branding_version,
        ];
    }

    private function text(mixed $v): ?string
    {
        return is_string($v) && trim($v) !== '' ? trim($v) : null;
    }

    private function url(?string $path): ?string
    {
        return $path ? Storage::disk('public')->url($path) : null;
    }

    /** Reglas de validación del array `branding` (prefijo configurable). */
    public static function rules(string $prefix = ''): array
    {
        $p = $prefix === '' ? '' : $prefix.'.';
        $rules = [
            $p.'app_name' => ['required', 'string', 'max:60'],
            $p.'tagline' => ['nullable', 'string', 'max:120'],
            $p.'support_email' => ['nullable', 'email', 'max:190'],
            $p.'font_family' => ['required', Rule::in(self::FONTS)],
            $p.'corner_radius' => ['required', Rule::in(self::RADII)],
        ];

        foreach (self::FEATURES as $f) {
            $rules[$p."features.$f"] = ['boolean'];
        }
        foreach (['light', 'dark'] as $mode) {
            foreach (self::COLOR_KEYS as $key) {
                $rules[$p."colors.$mode.$key"] = ['required', new HexColor];
            }
        }

        return $rules;
    }

    /** Valida el array completo, incluido el contraste de texto sobre fondo (WCAG AA). */
    public function validator(array $branding): ValidatorContract
    {
        $validator = Validator::make(['branding' => $branding], self::rules('branding'));

        $validator->after(function (ValidatorContract $v) use ($branding): void {
            foreach (['light', 'dark'] as $mode) {
                foreach (self::CONTRAST_PAIRS as [$fg, $bg]) {
                    $field = "branding.colors.$mode.$fg";
                    if ($v->errors()->has($field)) {
                        continue;
                    }
                    $rule = new ContrastsWith(Arr::get($branding, "colors.$mode.$bg"), self::MIN_CONTRAST);
                    $rule->validate($field, Arr::get($branding, "colors.$mode.$fg"), function (string $msg) use ($v, $field): void {
                        $v->errors()->add($field, $msg);
                    });
                }
            }
        });

        return $validator;
    }
}
