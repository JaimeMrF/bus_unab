<?php

namespace Database\Seeders;

use App\Models\Bus;
use App\Models\BusRouteWaypoint;
use App\Models\Fare;
use App\Models\RouteStop;
use App\Models\Stop;
use App\Models\Transportadora;
use App\Models\User;
use App\Models\Wallet;
use App\Models\WalletTransaction;
use App\Services\WalletService;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;
use RuntimeException;

/**
 * Datos de demostración white-label: 3 organizaciones con identidades muy
 * distintas, usuarios por rol, flota, paradas, rutas, tarifas y wallets.
 *
 * Solo local/testing. Idempotente: re-ejecutar no duplica nada (todo se
 * resuelve por claves naturales: slug, email, placa, nombre de parada).
 */
class DemoSeeder extends Seeder
{
    public const PASSWORD = 'Demo12345!';

    public const SUPERADMIN_EMAIL = 'superadmin@demo.test';

    public const CITY_PASSENGER_EMAIL = 'pasajero@demo.test';

    /** Saldo inicial de cada pasajero demo: $50.000 COP en centavos. */
    public const WALLET_CENTAVOS = 5_000_000;

    public function run(): void
    {
        if (! app()->environment('local', 'testing')) {
            throw new RuntimeException('DemoSeeder solo puede ejecutarse en local/testing.');
        }

        foreach (self::organizations() as $slug => $org) {
            $tenant = $this->tenant($slug, $org);
            $this->users($slug, $org, $tenant);
            $this->fleet($org, $tenant);
            $this->fare($org, $tenant);
        }

        $this->person(self::SUPERADMIN_EMAIL, 'Super Admin Demo', 'super_admin', null);
        $city = $this->person(self::CITY_PASSENGER_EMAIL, 'Pasajero de Ciudad', 'pasajero', null);
        $this->fund(Wallet::para($city), 'demo_seed_city');
    }

    /** @return array<string, array<string, mixed>> */
    public static function organizations(): array
    {
        return [
            'metrobus' => [
                'nombre' => 'MetroBus Bucaramanga',
                'app_name' => 'MetroBus',
                'tagline' => 'Tu ciudad en movimiento',
                'font_family' => 'poppins',
                'corner_radius' => 'lg',
                'features' => ['qr_payments' => true, 'wallet' => true, 'driver_mode' => true],
                'colors' => [
                    'light' => self::palette('#0F766E', '#FFFFFF', '#334155', '#FFFFFF', '#F0FDFA', '#FFFFFF', '#0F172A', '#F59E0B'),
                    'dark' => self::palette('#2DD4BF', '#042F2E', '#94A3B8', '#0B1220', '#042F2E', '#0B3B38', '#ECFEFF', '#FBBF24'),
                ],
                'mascot' => true,
                'bus_style' => ['body' => '#0F766E', 'accent' => '#F59E0B', 'icon' => 'modern'],
                'logo_color' => '#0F766E',
                'fare' => ['DEMO-ORD', 'Tarifa ordinaria', 280_000],
                'plates' => ['MB101', 'MB102'],
                'ext_id' => 900001,
                'stops' => [
                    ['Parque San Pío', 7.1193, -73.1227],
                    ['Clínica Chicamocha', 7.1255, -73.1190],
                    ['Centro Comercial Cabecera', 7.1310, -73.1150],
                    ['Terminal Norte', 7.1370, -73.1100],
                    ['Portal Norte', 7.1425, -73.1065],
                ],
            ],
            'campus' => [
                'nombre' => 'Campus Shuttle',
                'app_name' => 'Campus Go',
                'tagline' => 'Llega a clase a tiempo',
                'font_family' => 'inter',
                'corner_radius' => 'sm',
                'features' => ['qr_payments' => false, 'wallet' => false, 'driver_mode' => true],
                'colors' => [
                    'light' => self::palette('#6D28D9', '#FFFFFF', '#BE185D', '#FFFFFF', '#FAF5FF', '#FFFFFF', '#1E1B4B', '#EC4899'),
                    'dark' => self::palette('#A78BFA', '#1E1B4B', '#F472B6', '#1E1B4B', '#1E1B4B', '#2E2A5E', '#F5F3FF', '#F472B6'),
                ],
                'mascot' => true,
                'bus_style' => ['body' => null, 'accent' => '#EC4899', 'icon' => 'minibus'],
                'logo_color' => '#6D28D9',
                'fare' => ['DEMO-EST', 'Tarifa estudiantil', 100_000],
                'plates' => ['CP201', 'CP202'],
                'ext_id' => 900003,
                'stops' => [
                    ['Biblioteca Central', 7.1218, -73.1158],
                    ['Facultad de Ingeniería', 7.1236, -73.1139],
                    ['Coliseo', 7.1251, -73.1160],
                    ['Residencias', 7.1233, -73.1181],
                ],
            ],
            'logistica' => [
                'nombre' => 'Logística del Oriente',
                'app_name' => 'RutaCarga',
                'tagline' => null,
                'font_family' => 'system',
                'corner_radius' => 'md',
                'features' => ['qr_payments' => false, 'wallet' => true, 'driver_mode' => true],
                'colors' => [
                    'light' => self::palette('#C2410C', '#FFFFFF', '#1F2937', '#FFFFFF', '#FFF7ED', '#FFFFFF', '#1C1917', '#0369A1'),
                    'dark' => self::palette('#FB923C', '#1C1917', '#D1D5DB', '#1C1917', '#1C1917', '#292524', '#FAFAF9', '#38BDF8'),
                ],
                'mascot' => false,
                'bus_style' => ['body' => null, 'accent' => null, 'icon' => 'classic'],
                'logo_color' => '#C2410C',
                'fare' => ['DEMO-CARGA', 'Tarifa operativa', 350_000],
                'plates' => ['LG301', 'LG302'],
                'ext_id' => 900005,
                'stops' => [
                    ['Bodega Girón', 7.0640, -73.0860],
                    ['Parque Industrial', 7.0700, -73.0950],
                    ['Peaje Floridablanca', 7.0780, -73.1040],
                    ['Centro de Acopio', 7.0850, -73.1130],
                    ['Puerto Seco', 7.0920, -73.1200],
                ],
            ],
            'bucaratransit' => [
                'nombre' => 'BucaraTransit',
                'app_name' => 'BucaraTransit',
                'tagline' => 'Muévete por Bucaramanga',
                'font_family' => 'poppins',
                'corner_radius' => 'lg',
                'features' => ['qr_payments' => true, 'wallet' => true, 'driver_mode' => true],
                'colors' => [
                    'light' => self::palette('#01265A', '#FFFFFF', '#FCBB01', '#01265A', '#F4F7FC', '#FFFFFF', '#0B1B33', '#FCBB01'),
                    'dark' => self::palette('#FCBB01', '#01265A', '#7FA6E8', '#00142F', '#00142F', '#0A2F66', '#FFFFFF', '#FCBB01'),
                ],
                'mascot' => false,
                'poses' => true,
                'bus_style' => ['body' => '#01265A', 'accent' => '#FCBB01', 'icon' => 'classic'],
                'logo_color' => '#01265A',
                'fare' => ['DEMO-BT', 'Tarifa BucaraTransit', 230_000],
                'plates' => ['BT401', 'BT402'],
                'ext_id' => 900007,
                'stops' => [
                    ['Parque del Agua', 7.1090, -73.1150],
                    ['Plaza Guarín', 7.1130, -73.1190],
                    ['Parque Santander', 7.1190, -73.1210],
                    ['Cañaveral', 7.1000, -73.1070],
                    ['Terminal de Transporte', 7.1280, -73.1260],
                ],
            ],
        ];
    }

    private static function palette(string $primary, string $onPrimary, string $secondary, string $onSecondary, string $background, string $surface, string $onSurface, string $accent): array
    {
        $dark = $background[1] < '5'; // fondos oscuros: tonos de estado más claros

        return [
            'primary' => $primary, 'on_primary' => $onPrimary,
            'secondary' => $secondary, 'on_secondary' => $onSecondary,
            'background' => $background, 'surface' => $surface, 'on_surface' => $onSurface,
            'accent' => $accent,
            'success' => $dark ? '#4ADE80' : '#15803D',
            'warning' => $dark ? '#FBBF24' : '#B45309',
            'error' => $dark ? '#F87171' : '#B91C1C',
        ];
    }

    /** Archivo del set de assets => pose pública (ver contrato de branding). */
    private const POSE_FILES = [
        'greeting' => 'saludo', 'curious' => 'curioso', 'sad' => 'triste', 'waiting' => 'triste_espera',
        'phone' => 'celular', 'map' => 'mapa', 'driver' => 'conductor', 'ok' => 'ok', 'celebrating' => 'celebrando',
    ];

    /** Copia (idempotente) las 9 poses de database/seeders/assets/{slug}/ al disco público. */
    private function poses(string $slug): ?array
    {
        $paths = [];
        foreach (self::POSE_FILES as $pose => $file) {
            $source = database_path("seeders/assets/{$slug}/{$file}.webp");
            if (! is_file($source)) {
                return null;
            }
            $target = "branding/demo/{$slug}/{$pose}.webp";
            Storage::disk('public')->put($target, file_get_contents($source));
            $paths[$pose] = $target;
        }

        return $paths;
    }

    private function tenant(string $slug, array $org): Transportadora
    {
        $paths = ['mascot_path' => null, 'mascot_poses' => null];
        if (! empty($org['poses'])) {
            $paths['mascot_poses'] = $this->poses($slug);
        }
        $paths['logo_path'] = $this->logo($slug, $org);

        if ($org['mascot']) {
            $paths['mascot_path'] = "branding/demo/{$slug}-mascot.svg";
            Storage::disk('public')->put($paths['mascot_path'], $this->mascotSvg($org['logo_color']));
        }

        return Transportadora::updateOrCreate(['slug' => $slug], [
            'nombre' => $org['nombre'],
            'contacto_email' => "contacto@{$slug}.demo.test",
            'plan' => 'pro',
            'activo' => true,
            'bus_style' => $org['bus_style'],
            'branding' => [
                'app_name' => $org['app_name'],
                'tagline' => $org['tagline'],
                'support_email' => "soporte@{$slug}.demo.test",
                'font_family' => $org['font_family'],
                'corner_radius' => $org['corner_radius'],
                'features' => $org['features'],
                'colors' => $org['colors'],
            ],
        ] + $paths);
    }

    /**
     * Logo del tenant: el raster del set de assets si existe (Coil no decodifica SVG y el
     * placeholder con forma de bus no es la marca real), si no el SVG genérico.
     */
    private function logo(string $slug, array $org): string
    {
        $source = database_path("seeders/assets/{$slug}/logo.webp");
        $real = is_file($source);
        $target = "branding/demo/{$slug}-logo.".($real ? 'webp' : 'svg');

        Storage::disk('public')->put(
            $target,
            $real ? file_get_contents($source) : $this->logoSvg($org['app_name'], $org['logo_color']),
        );

        return $target;
    }

    private function users(string $slug, array $org, Transportadora $tenant): void
    {
        $this->person("admin.{$slug}@demo.test", "Admin {$org['app_name']}", 'tenant_admin', $tenant->id);
        $this->person("driver.{$slug}@demo.test", "Conductor {$org['app_name']}", 'driver', $tenant->id);
        $passenger = $this->person("pasajero.{$slug}@demo.test", "Pasajero {$org['app_name']}", 'pasajero', $tenant->id);

        $this->fund(Wallet::para($passenger), "demo_seed_{$slug}");
    }

    private function person(string $email, string $name, string $role, ?int $tenantId): User
    {
        $user = User::firstOrNew(['email' => $email]);
        if (! $user->exists) {
            $user->password = Hash::make(self::PASSWORD);
        }

        $user->forceFill(['name' => $name, 'role' => $role, 'transportadora_id' => $tenantId])->save();

        return $user;
    }

    /** Idempotente por reference única del ledger. */
    private function fund(Wallet $wallet, string $reference): void
    {
        app(WalletService::class)->move($wallet, self::WALLET_CENTAVOS, WalletTransaction::TIPO_CREDITO, $reference, 'demo');
    }

    private function fleet(array $org, Transportadora $tenant): void
    {
        $stops = [];
        foreach ($org['stops'] as [$name, $lat, $lng]) {
            $stops[] = Stop::updateOrCreate(
                ['name' => $name, 'transportadora_id' => $tenant->id],
                ['address' => $org['nombre'], 'latitude' => $lat, 'longitude' => $lng, 'radius_meters' => 60, 'is_active' => true],
            );
        }

        foreach ($org['plates'] as $i => $plate) {
            $bus = Bus::updateOrCreate(
                ['plate' => $plate],
                [
                    'name' => "{$org['app_name']} Línea ".($i + 1),
                    'external_vehicle_id' => $org['ext_id'] + $i,
                    'capacity' => 40,
                    'is_active' => true,
                    'transportadora_id' => $tenant->id,
                ],
            );

            // El segundo bus recorre la ruta en sentido contrario.
            $ordered = $i % 2 === 0 ? $stops : array_reverse($stops);

            foreach ($ordered as $order => $stop) {
                RouteStop::updateOrCreate(
                    ['bus_id' => $bus->id, 'stop_id' => $stop->id],
                    ['order' => $order + 1, 'estimated_minutes' => 4],
                );
            }

            $this->waypoints($bus, $ordered);
        }
    }

    /** Polilínea simple: paradas con 3 puntos intermedios por tramo. */
    private function waypoints(Bus $bus, array $stops): void
    {
        $points = [];
        foreach ($stops as $k => $stop) {
            $points[] = [$stop->latitude, $stop->longitude, $stop->name];
            if (isset($stops[$k + 1])) {
                for ($s = 1; $s <= 3; $s++) {
                    $f = $s / 4;
                    $points[] = [
                        $stop->latitude + ($stops[$k + 1]->latitude - $stop->latitude) * $f,
                        $stop->longitude + ($stops[$k + 1]->longitude - $stop->longitude) * $f,
                        null,
                    ];
                }
            }
        }

        foreach ($points as $order => [$lat, $lng, $label]) {
            BusRouteWaypoint::updateOrCreate(
                ['bus_id' => $bus->id, 'order' => $order + 1],
                ['latitude' => $lat, 'longitude' => $lng, 'label' => $label],
            );
        }

        BusRouteWaypoint::where('bus_id', $bus->id)->where('order', '>', count($points))->delete();
    }

    private function fare(array $org, Transportadora $tenant): void
    {
        [$codigo, $nombre, $monto] = $org['fare'];

        Fare::updateOrCreate(
            ['transportadora_id' => $tenant->id, 'codigo' => $codigo],
            ['nombre' => $nombre, 'monto_centavos' => $monto, 'activa' => true],
        );
    }

    private function logoSvg(string $name, string $color): string
    {
        $initial = htmlspecialchars(mb_strtoupper(mb_substr($name, 0, 1)), ENT_XML1);

        return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" width="128" height="128">'
            .'<rect width="128" height="128" rx="28" fill="'.$color.'"/>'
            .'<rect x="28" y="38" width="72" height="44" rx="10" fill="#FFFFFF"/>'
            .'<circle cx="46" cy="90" r="8" fill="#FFFFFF"/><circle cx="82" cy="90" r="8" fill="#FFFFFF"/>'
            .'<text x="64" y="70" font-size="32" font-family="sans-serif" font-weight="700" text-anchor="middle" fill="'.$color.'">'.$initial.'</text>'
            .'</svg>';
    }

    private function mascotSvg(string $color): string
    {
        return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">'
            .'<circle cx="100" cy="100" r="90" fill="'.$color.'"/>'
            .'<circle cx="70" cy="85" r="14" fill="#FFFFFF"/><circle cx="130" cy="85" r="14" fill="#FFFFFF"/>'
            .'<circle cx="72" cy="87" r="6" fill="#111111"/><circle cx="128" cy="87" r="6" fill="#111111"/>'
            .'<path d="M65 125 Q100 160 135 125" stroke="#FFFFFF" stroke-width="8" fill="none" stroke-linecap="round"/>'
            .'</svg>';
    }
}
