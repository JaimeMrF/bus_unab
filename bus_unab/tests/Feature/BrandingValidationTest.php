<?php

namespace Tests\Feature;

use App\Rules\SafeSvg;
use App\Services\BrandingService;
use App\Support\ColorContrast;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Validator;
use Tests\TestCase;

/**
 * White-label — validación de ediciones de branding (hex, contraste, imágenes).
 */
class BrandingValidationTest extends TestCase
{
    private function valid(): array
    {
        return [
            'app_name' => 'Acme Go',
            'tagline' => null,
            'support_email' => 'ayuda@acme.test',
            'font_family' => 'poppins',
            'corner_radius' => 'md',
            'features' => ['qr_payments' => true, 'wallet' => true, 'driver_mode' => false],
            'colors' => BrandingService::defaultColors(),
        ];
    }

    private function validate(array $branding)
    {
        return (new BrandingService)->validator($branding);
    }

    public function test_default_palette_is_valid_and_passes_aa(): void
    {
        $this->assertTrue($this->validate($this->valid())->passes());
    }

    public function test_contrast_helper_known_values(): void
    {
        $this->assertEqualsWithDelta(21.0, ColorContrast::ratio('#000000', '#FFFFFF'), 0.01);
        $this->assertEqualsWithDelta(1.0, ColorContrast::ratio('#777777', '#777777'), 0.01);
    }

    /** @dataProvider invalidHex */
    public function test_rejects_invalid_hex(mixed $bad): void
    {
        $b = $this->valid();
        $b['colors']['light']['accent'] = $bad;

        $v = $this->validate($b);

        $this->assertTrue($v->fails());
        $this->assertTrue($v->errors()->has('branding.colors.light.accent'));
    }

    public static function invalidHex(): array
    {
        return [
            'sin almohadilla' => ['2563EB'],
            'corto' => ['#FFF'],
            'no hex' => ['#GGGGGG'],
            'nombre' => ['red'],
            'rgb()' => ['rgb(1,2,3)'],
            'inyeccion' => ['#FFFFFF;}</style><script>'],
            'alpha 8' => ['#2563EBFF'],
            'vacio' => [''],
            'array' => [['#FFFFFF']],
        ];
    }

    public function test_rejects_missing_color_key(): void
    {
        $b = $this->valid();
        unset($b['colors']['dark']['error']);

        $this->assertTrue($this->validate($b)->errors()->has('branding.colors.dark.error'));
    }

    public function test_rejects_low_contrast_on_primary_in_light_mode(): void
    {
        $b = $this->valid();
        $b['colors']['light']['primary'] = '#FFFF00';
        $b['colors']['light']['on_primary'] = '#FFFFFF'; // ~1.07:1

        $v = $this->validate($b);

        $this->assertTrue($v->fails());
        $this->assertTrue($v->errors()->has('branding.colors.light.on_primary'));
    }

    public function test_rejects_low_contrast_in_dark_mode(): void
    {
        $b = $this->valid();
        $b['colors']['dark']['on_primary'] = '#60A5FA'; // igual al primary (1:1)

        $this->assertTrue($this->validate($b)->errors()->has('branding.colors.dark.on_primary'));
    }

    public function test_rejects_low_contrast_on_surface_and_secondary(): void
    {
        $b = $this->valid();
        $b['colors']['light']['on_surface'] = '#EEEEEE';
        $b['colors']['light']['on_secondary'] = '#555555';

        $errors = $this->validate($b)->errors();

        $this->assertTrue($errors->has('branding.colors.light.on_surface'));
        $this->assertTrue($errors->has('branding.colors.light.on_secondary'));
    }

    public function test_contrast_threshold_is_inclusive_at_4_5(): void
    {
        // #767676 sobre blanco ≈ 4.54:1 (pasa); #777777 ≈ 4.48:1 (falla)
        $ok = $this->valid();
        $ok['colors']['light']['primary'] = '#767676';
        $this->assertTrue($this->validate($ok)->passes());

        $bad = $this->valid();
        $bad['colors']['light']['primary'] = '#777777';
        $this->assertTrue($this->validate($bad)->errors()->has('branding.colors.light.on_primary'));
    }

    public function test_rejects_non_whitelisted_font_and_radius(): void
    {
        $b = $this->valid();
        $b['font_family'] = 'comic-sans';
        $b['corner_radius'] = 'xxl';

        $errors = $this->validate($b)->errors();

        $this->assertTrue($errors->has('branding.font_family'));
        $this->assertTrue($errors->has('branding.corner_radius'));
    }

    public function test_requires_app_name_and_limits_length(): void
    {
        $b = $this->valid();
        $b['app_name'] = '';
        $this->assertTrue($this->validate($b)->errors()->has('branding.app_name'));

        $b['app_name'] = str_repeat('x', 61);
        $this->assertTrue($this->validate($b)->errors()->has('branding.app_name'));
    }

    public function test_rejects_invalid_support_email(): void
    {
        $b = $this->valid();
        $b['support_email'] = 'no-es-email';

        $this->assertTrue($this->validate($b)->errors()->has('branding.support_email'));
    }

    // --- Imágenes -------------------------------------------------------

    private function svg(string $body): UploadedFile
    {
        return UploadedFile::fake()->createWithContent('logo.svg', $body);
    }

    public function test_safe_svg_accepts_clean_svg(): void
    {
        $f = $this->svg('<svg xmlns="http://www.w3.org/2000/svg"><rect width="1" height="1"/></svg>');

        $this->assertTrue(Validator::make(['f' => $f], ['f' => [new SafeSvg]])->passes());
    }

    /** @dataProvider maliciousSvg */
    public function test_safe_svg_rejects_active_content(string $body): void
    {
        $this->assertTrue(Validator::make(['f' => $this->svg($body)], ['f' => [new SafeSvg]])->fails());
    }

    public static function maliciousSvg(): array
    {
        return [
            'script' => ['<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>'],
            'onload' => ['<svg xmlns="http://www.w3.org/2000/svg" onload="alert(1)"></svg>'],
            'javascript href' => ['<svg xmlns="http://www.w3.org/2000/svg"><a href="javascript:alert(1)"><text>x</text></a></svg>'],
            'foreignObject' => ['<svg xmlns="http://www.w3.org/2000/svg"><foreignObject><div/></foreignObject></svg>'],
            'xxe' => ['<?xml version="1.0"?><!DOCTYPE svg [<!ENTITY x SYSTEM "file:///etc/passwd">]><svg xmlns="http://www.w3.org/2000/svg">&x;</svg>'],
            'href externo' => ['<svg xmlns="http://www.w3.org/2000/svg"><image href="https://evil.test/x.png"/></svg>'],
        ];
    }

    /**
     * Las reglas de subida (mime real png/webp/svg, <=1MB) hoy viven solo en el
     * formulario Filament, que aún no existe. Cuando backend exponga las reglas
     * (p.ej. BrandingService::imageRules()) este test las ejercita; hasta entonces
     * se reporta como pendiente en vez de pasar en falso.
     */
    public function test_image_rules_reject_fake_mime_and_oversize(): void
    {
        if (! method_exists(BrandingService::class, 'imageRules')) {
            $this->markTestIncomplete('Pendiente backend (T2): exponer BrandingService::imageRules() o FormRequest de subida.');
        }

        $rules = BrandingService::imageRules();

        // UploadedFile::fake() deduce el mime por la extensión; usamos un archivo real para que cuente el mime real.
        $tmp = tempnam(sys_get_temp_dir(), 'brand');
        file_put_contents($tmp, '<?php echo 1; ?>');
        $fakePng = new UploadedFile($tmp, 'logo.png', 'image/png', null, true);
        // PNG 1x1 real (sin GD, que no está habilitado en todos los entornos).
        $png = base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==');
        $big = UploadedFile::fake()->createWithContent('big.png', $png.str_repeat("\0", 1025 * 1024));
        $ok = UploadedFile::fake()->createWithContent('ok.png', $png);

        $this->assertTrue(Validator::make(['f' => $fakePng], ['f' => $rules])->fails(), 'mime falso');
        $this->assertTrue(Validator::make(['f' => $big], ['f' => $rules])->fails(), '>1MB');
        $this->assertTrue(Validator::make(['f' => $ok], ['f' => $rules])->passes(), 'png válido');
    }

    public function test_tenant_admin_cannot_edit_other_tenant_branding(): void
    {
        $this->markTestIncomplete('Pendiente backend (T2): Filament Tenant branding page/policy aún no existe; escribir test cross-tenant (403/404) al aterrizar.');
    }
}
