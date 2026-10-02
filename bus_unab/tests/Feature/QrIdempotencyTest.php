<?php

namespace Tests\Feature;

use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\IdempotencyKey;
use App\Models\QrPaymentToken;
use App\Models\Stop;
use App\Models\User;
use App\Models\Wallet;
use App\Services\QrPaymentService;
use App\Services\WalletService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * POST /qr/pay con Idempotency-Key: el reintento tras un corte devuelve el
 * resultado original sin segundo débito.
 */
class QrIdempotencyTest extends TestCase
{
    use RefreshDatabase;

    private User $passenger;

    private User $driver;

    protected function setUp(): void
    {
        parent::setUp();
        $this->passenger = User::factory()->create(['role' => 'pasajero']);
        $this->driver = User::factory()->create(['role' => 'driver']);
        app(WalletService::class)->credit(Wallet::para($this->passenger), 1_000_000, 'seed', 'mock');
    }

    private function qr(): string
    {
        return app(QrPaymentService::class)->issueToken($this->passenger)[1];
    }

    private function pay(User $driver, string $qr, ?string $key)
    {
        return $this->actingAs($driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr], $key ? ['Idempotency-Key' => $key] : []);
    }

    private function balance(): int
    {
        return Wallet::para($this->passenger)->fresh()->balance_centavos;
    }

    public function test_retry_after_network_cut_returns_original_result_without_second_debit(): void
    {
        $qr = $this->qr();
        $key = 'qr-pay-'.explode('.', $qr)[0];

        $first = $this->pay($this->driver, $qr, $key)->assertOk();
        $retry = $this->pay($this->driver, $qr, $key)->assertOk();

        $this->assertSame($first->json('data'), $retry->json('data'));
        $this->assertSame('true', $retry->headers->get('Idempotent-Replayed'));
        $this->assertNull($first->headers->get('Idempotent-Replayed'));
        $this->assertSame(1_000_000 - QrPaymentService::FALLBACK_FARE_CENTAVOS, $this->balance());
        $this->assertDatabaseCount('wallet_transactions', 2); // seed + un único débito
        $this->assertSame(1, IdempotencyKey::count());
    }

    public function test_without_key_replay_is_still_rejected(): void
    {
        $qr = $this->qr();

        $this->pay($this->driver, $qr, null)->assertOk();
        $this->pay($this->driver, $qr, null)->assertStatus(422)->assertJsonPath('message', 'Este QR ya fue utilizado.');
    }

    public function test_another_driver_with_same_key_gets_an_error_not_the_result(): void
    {
        $qr = $this->qr();
        $key = 'qr-pay-'.explode('.', $qr)[0];
        $other = User::factory()->create(['role' => 'driver']);

        $this->pay($this->driver, $qr, $key)->assertOk();

        $this->pay($other, $qr, $key)->assertStatus(422)->assertJsonPath('message', 'Este QR ya fue utilizado.');
        $this->assertSame(1_000_000 - QrPaymentService::FALLBACK_FARE_CENTAVOS, $this->balance());
    }

    public function test_same_key_with_different_payload_is_a_conflict(): void
    {
        $qrA = $this->qr();
        $qrB = $this->qr();
        $key = 'qr-pay-compartida';

        $this->pay($this->driver, $qrA, $key)->assertOk();
        $this->pay($this->driver, $qrB, $key)->assertStatus(422);

        // El segundo QR NO se cobró.
        $this->assertSame(1_000_000 - QrPaymentService::FALLBACK_FARE_CENTAVOS, $this->balance());
        $this->assertNull(QrPaymentToken::orderByDesc('id')->first()->used_at);
    }

    public function test_failed_charge_is_not_stored_so_driver_can_retry(): void
    {
        $poor = User::factory()->create(['role' => 'pasajero']);
        [, $qr] = app(QrPaymentService::class)->issueToken($poor);
        $key = 'qr-pay-poor';

        $this->pay($this->driver, $qr, $key)->assertStatus(422);
        $this->assertSame(0, IdempotencyKey::count());

        app(WalletService::class)->credit(Wallet::para($poor), 500_000, 'topup', 'mock');

        $this->pay($this->driver, $qr, $key)->assertOk();
    }

    public function test_sibling_request_that_lost_the_race_gets_the_stored_result(): void
    {
        // Simula la petición hermana: el primer cobro ya confirmó su fila de idempotencia
        // cuando el reintento concurrente falla con "ya utilizado".
        $qr = $this->qr();
        $key = 'qr-pay-carrera';

        $first = $this->pay($this->driver, $qr, $key)->assertOk();
        $row = IdempotencyKey::first();
        $this->assertSame($first->json(), $row->response);

        $this->pay($this->driver, $qr, $key)->assertOk()->assertJsonPath('data.token_id', $first->json('data.token_id'));
    }

    public function test_expired_key_is_discarded(): void
    {
        $qr = $this->qr();
        $key = 'qr-pay-vieja';

        $this->pay($this->driver, $qr, $key)->assertOk();
        IdempotencyKey::query()->update(['expires_at' => now()->subMinute()]);

        // Vencida: ya no hay replay y el token real sigue usado.
        $this->pay($this->driver, $qr, $key)->assertStatus(422);
        $this->assertSame(0, IdempotencyKey::count());
    }

    public function test_invalid_key_format_is_rejected(): void
    {
        $qr = $this->qr();

        $this->pay($this->driver, $qr, 'mala clave con espacios')->assertStatus(422);
        $this->pay($this->driver, $qr, str_repeat('a', 191))->assertStatus(422);
        $this->assertSame(1_000_000, $this->balance());
    }

    public function test_validate_endpoint_is_idempotent_too(): void
    {
        $bus = Bus::create(['name' => 'B', 'plate' => 'B1', 'external_vehicle_id' => 5, 'capacity' => 40, 'is_active' => true]);
        $stop = Stop::create(['name' => 'S', 'latitude' => 7.1, 'longitude' => -73.1, 'radius_meters' => 50, 'is_active' => true]);
        $req = BusRequest::create(['user_id' => $this->passenger->id, 'bus_id' => $bus->id, 'stop_id' => $stop->id, 'status' => 'pending']);

        $payload = [
            'request_id' => $req->id, 'user_id' => $this->passenger->id, 'bus_id' => $bus->id,
            'stop_id' => $stop->id, 'ts' => (int) (microtime(true) * 1000),
        ];
        $headers = ['Idempotency-Key' => 'qr-validate-1'];

        $this->actingAs($this->driver, 'sanctum')->postJson('/api/v1/qr/validate', $payload, $headers)->assertOk();
        $this->actingAs($this->driver, 'sanctum')->postJson('/api/v1/qr/validate', $payload, $headers)
            ->assertOk()->assertHeader('Idempotent-Replayed', 'true');

        $this->actingAs($this->driver, 'sanctum')->postJson('/api/v1/qr/validate', $payload)
            ->assertStatus(422); // sin clave: sigue el error de "ya utilizado"
    }
}
