<?php

namespace Tests\Feature;

use App\Models\Bus;
use App\Models\BusRouteWaypoint;
use App\Models\Fare;
use App\Models\RouteStop;
use App\Models\Stop;
use App\Models\Transportadora;
use App\Models\User;
use App\Models\Wallet;
use App\Models\WalletTransaction;
use App\Rules\SafeSvg;
use App\Services\BrandingService;
use App\Services\WalletService;
use App\Support\RouteWalker;
use Database\Seeders\DemoSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Facades\Validator;
use Tests\TestCase;

class DemoSeederTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        Storage::fake('public');
    }

    private function seedDemo(): void
    {
        $this->seed(DemoSeeder::class);
    }

    /** @return array<string, int> */
    private function counts(): array
    {
        return [
            'tenants' => Transportadora::count(),
            'users' => User::count(),
            'buses' => Bus::count(),
            'stops' => Stop::count(),
            'route_stops' => RouteStop::count(),
            'waypoints' => BusRouteWaypoint::count(),
            'wallets' => Wallet::count(),
            'ledger' => WalletTransaction::count(),
            'fares' => Fare::count(),
        ];
    }

    public function test_seeds_three_orgs_users_fleet_and_wallets(): void
    {
        $this->seedDemo();

        $this->assertSame(['campus', 'logistica', 'metrobus'], Transportadora::orderBy('slug')->pluck('slug')->all());
        $this->assertSame(3, User::where('role', 'tenant_admin')->count());
        $this->assertSame(3, User::where('role', 'driver')->count());
        $this->assertSame(4, User::where('role', 'pasajero')->count());
        $this->assertSame(1, User::where('role', 'super_admin')->count());
        $this->assertSame(6, Bus::count());
        $this->assertSame(4, Wallet::count());

        foreach (Wallet::all() as $wallet) {
            $this->assertSame(DemoSeeder::WALLET_CENTAVOS, $wallet->balance_centavos);
            $this->assertSame($wallet->balance_centavos, app(WalletService::class)->ledgerSum($wallet));
        }

        $admin = User::where('email', 'admin.metrobus@demo.test')->first();
        $this->assertTrue($admin->isTenantAdmin());
        $this->assertTrue(User::where('email', DemoSeeder::SUPERADMIN_EMAIL)->first()->isSuperAdmin());
        $this->assertTrue(Hash::check(DemoSeeder::PASSWORD, $admin->password));
    }

    public function test_is_idempotent(): void
    {
        $this->seedDemo();
        $before = $this->counts();
        $versions = Transportadora::orderBy('id')->pluck('branding_version', 'slug')->all();
        $hash = User::where('email', 'driver.campus@demo.test')->value('password');

        $this->seedDemo();
        $this->seedDemo();

        $this->assertSame($before, $this->counts());
        $this->assertSame($versions, Transportadora::orderBy('id')->pluck('branding_version', 'slug')->all(), 'branding_version no debe subir sin cambios');
        $this->assertSame($hash, User::where('email', 'driver.campus@demo.test')->value('password'));
        $this->assertSame(DemoSeeder::WALLET_CENTAVOS, Wallet::first()->balance_centavos, 'el saldo no se duplica');
    }

    public function test_each_user_belongs_to_the_right_tenant(): void
    {
        $this->seedDemo();

        foreach (['metrobus', 'campus', 'logistica'] as $slug) {
            $tenantId = Transportadora::where('slug', $slug)->value('id');
            foreach (['admin', 'driver', 'pasajero'] as $prefix) {
                $this->assertSame($tenantId, User::where('email', "{$prefix}.{$slug}@demo.test")->value('transportadora_id'));
            }
            $this->assertSame(2, Bus::where('transportadora_id', $tenantId)->count());
        }
        $this->assertNull(User::where('email', DemoSeeder::CITY_PASSENGER_EMAIL)->value('transportadora_id'));
    }

    public function test_branding_is_valid_and_very_different_between_orgs(): void
    {
        $this->seedDemo();

        $payloads = [];
        foreach (['metrobus', 'campus', 'logistica'] as $slug) {
            $branding = Transportadora::where('slug', $slug)->first()->branding;
            $this->assertTrue((new BrandingService)->validator($branding)->passes(), "$slug: ".json_encode((new BrandingService)->validator($branding)->errors()->all()));

            $res = $this->getJson("/api/v1/branding/{$slug}")->assertOk();
            $this->assertStringEndsWith("branding/demo/{$slug}-logo.svg", $res->json('data.logo_url'));
            $payloads[$slug] = $res->json('data');
        }

        $this->assertCount(3, array_unique(array_column($payloads, 'font_family')));
        $this->assertCount(3, array_unique(array_column($payloads, 'corner_radius')));
        $this->assertCount(3, array_unique(array_map(fn ($p) => $p['colors']['light']['primary'], $payloads)));
        $this->assertCount(3, array_unique(array_map(fn ($p) => json_encode($p['features']), $payloads)));
        $this->assertNull($payloads['logistica']['mascot_url']);
        $this->assertNotNull($payloads['metrobus']['mascot_url']);
        Storage::disk('public')->assertExists('branding/demo/metrobus-mascot.svg');
    }

    public function test_demo_svgs_pass_the_safe_svg_rule(): void
    {
        $this->seedDemo();

        foreach (Storage::disk('public')->allFiles('branding/demo') as $file) {
            $upload = UploadedFile::fake()->createWithContent('x.svg', Storage::disk('public')->get($file));
            $this->assertTrue(Validator::make(['f' => $upload], ['f' => [new SafeSvg]])->passes(), $file);
        }
    }

    public function test_refuses_to_run_outside_local_and_testing(): void
    {
        $this->app['env'] = 'production';

        $this->expectException(\RuntimeException::class);
        (new DemoSeeder)->run();
    }

    public function test_setup_command_refuses_outside_local_and_testing(): void
    {
        $this->app['env'] = 'production';

        $this->artisan('demo:setup')->assertFailed();
        $this->artisan('demo:simulate-buses --ticks=1')->assertFailed();
    }

    public function test_route_walker_headings_and_wraparound(): void
    {
        $walker = new RouteWalker([[7.0, -73.0], [7.01, -73.0]]); // hacia el norte

        $this->assertSame(0, $walker->at(10)['heading']);
        $out = $walker->at(10);
        $this->assertGreaterThan(7.0, $out['lat']);

        $back = $walker->at($walker->totalMeters() / 2 + 10); // tramo de vuelta
        $this->assertSame(180, $back['heading']);

        $this->assertEquals($walker->at(5), $walker->at($walker->totalMeters() + 5));
    }

    public function test_simulate_command_publishes_moving_locations_with_heading(): void
    {
        $this->seedDemo();

        $this->artisan('demo:simulate-buses --ticks=1 --interval=0 --speed=40')->assertSuccessful();
        $first = Cache::get('driver_location_MB101');
        $this->assertNotNull($first);
        $this->assertEqualsCanonicalizing(['latitude', 'longitude', 'heading', 'speed_kmh', 'updated_at'], array_keys($first));
        $this->assertContains($first['heading'], range(0, 360));

        foreach (['MB102', 'CP201', 'CP202', 'LG301', 'LG302'] as $plate) {
            $this->assertNotNull(Cache::get("driver_location_{$plate}"), $plate);
        }

        $this->artisan('demo:simulate-buses --ticks=3 --interval=0 --speed=3600')->assertSuccessful();
        $moved = Cache::get('driver_location_MB101');
        $this->assertNotEquals([$first['latitude'], $first['longitude']], [$moved['latitude'], $moved['longitude']]);
    }

    public function test_simulate_without_demo_data_fails_with_hint(): void
    {
        $this->artisan('demo:simulate-buses --ticks=1')->assertFailed();
    }
}
