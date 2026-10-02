<?php

namespace Tests\Feature;

use App\Models\Bus;
use App\Models\Fare;
use App\Models\Stop;
use App\Models\Transportadora;
use App\Models\User;
use App\Models\Wallet;
use App\Services\GpsMobileService;
use App\Services\QrPaymentService;
use App\Services\WalletService;
use DomainException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class QrTenantAndGpsPerfTest extends TestCase
{
    use RefreshDatabase;

    private function tenant(string $slug): Transportadora
    {
        return Transportadora::create(['nombre' => $slug, 'slug' => $slug]);
    }

    private function qrFor(Transportadora $tenant): array
    {
        $passenger = User::factory()->create(['role' => 'pasajero']);
        app(WalletService::class)->credit(Wallet::para($passenger), 1_000_000, 'seed_'.$passenger->id, 'mock');
        $bus = Bus::create(['name' => 'B', 'plate' => 'T'.$passenger->id, 'external_vehicle_id' => 7000 + $passenger->id, 'capacity' => 40, 'is_active' => true, 'transportadora_id' => $tenant->id]);
        Fare::create(['transportadora_id' => $tenant->id, 'codigo' => 'X', 'nombre' => 'X', 'monto_centavos' => 200_000, 'activa' => true]);

        [, $qr] = app(QrPaymentService::class)->issueToken($passenger, $bus->id);

        return [$passenger, $qr];
    }

    public function test_driver_cannot_charge_qr_of_another_tenant(): void
    {
        $a = $this->tenant('a');
        $b = $this->tenant('b');
        [$passenger, $qr] = $this->qrFor($b);
        $driverA = User::factory()->create(['role' => 'driver', 'transportadora_id' => $a->id]);

        try {
            app(QrPaymentService::class)->pay($qr, $driverA);
            $this->fail('Debió rechazar el cobro cross-tenant');
        } catch (DomainException $e) {
            $this->assertSame('QR inválido o inexistente.', $e->getMessage());
        }

        $this->assertSame(1_000_000, Wallet::para($passenger)->fresh()->balance_centavos);
        $this->assertNull(DB::table('qr_payment_tokens')->where('user_id', $passenger->id)->value('used_at'));

        // El conductor del tenant correcto sí cobra, una sola vez.
        $driverB = User::factory()->create(['role' => 'driver', 'transportadora_id' => $b->id]);
        app(QrPaymentService::class)->pay($qr, $driverB);
        $this->assertSame(800_000, Wallet::para($passenger)->fresh()->balance_centavos);
        $this->expectException(DomainException::class);
        app(QrPaymentService::class)->pay($qr, $driverB);
    }

    public function test_failed_external_gps_is_remembered_and_not_retried_each_request(): void
    {
        config(['gpsmobile.base_url' => 'http://gps.test']);
        Http::fake(['*' => Http::response([], 500)]);

        $svc = app(GpsMobileService::class);
        $this->assertNull($svc->getAllBuses());
        $this->assertNull($svc->getAllBuses());
        $this->assertNull($svc->getAllBuses());

        Http::assertSentCount(1);
    }

    public function test_empty_env_values_fall_back_to_safe_defaults(): void
    {
        config(['gpsmobile.base_url' => '', 'gpsmobile.timeout' => '', 'gpsmobile.cod_user_inc' => '']);
        Http::fake(['*' => Http::response([], 500)]);

        app(GpsMobileService::class)->getAllBuses();

        Http::assertSent(fn ($r) => str_starts_with((string) $r->url(), 'http://gpsmobile.co:4000/api/Home/110571/'));
    }

    public function test_stops_and_buses_endpoints_use_a_constant_number_of_queries(): void
    {
        config(['gpsmobile.base_url' => 'http://gps.test']);
        Http::fake(['*' => Http::response(['sucess' => true, 'response' => ['veh' => []]])]);
        $t = $this->tenant('perf');
        $user = User::factory()->create(['role' => 'pasajero', 'transportadora_id' => $t->id]);

        $count = function (int $stops): int {
            for ($i = 0; $i < $stops; $i++) {
                Stop::create(['name' => "S{$stops}-{$i}", 'latitude' => 7.1, 'longitude' => -73.1, 'radius_meters' => 50, 'is_active' => true]);
            }
            DB::enableQueryLog();
            DB::flushQueryLog();
            $this->actingAs(User::first(), 'sanctum')->getJson('/api/v1/stops')->assertOk();
            $this->actingAs(User::first(), 'sanctum')->getJson('/api/v1/buses')->assertOk();

            return count(DB::getQueryLog());
        };

        $small = $count(2);
        $big = $count(30);

        $this->assertSame($small, $big, 'las consultas no deben crecer con el número de filas (N+1)');
    }
}
