<?php

namespace Tests\Feature;

use App\Models\QrPaymentToken;
use App\Models\User;
use App\Models\Wallet;
use App\Models\WalletTransaction;
use App\Services\QrPaymentService;
use App\Services\WalletService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * M3 · S3.2.4 — QR dinámico: issue/pay por HTTP + reglas de seguridad
 * (expiración, replay, firma manipulada, saldo insuficiente).
 */
class QrPaymentTest extends TestCase
{
    use RefreshDatabase;

    private User $passenger;
    private User $driver;
    private QrPaymentService $qr;
    private WalletService $wallets;

    protected function setUp(): void
    {
        parent::setUp();
        $this->passenger = User::factory()->create(['role' => 'pasajero']);
        $this->driver    = User::factory()->create(['role' => 'driver']);
        $this->qr        = app(QrPaymentService::class);
        $this->wallets   = app(WalletService::class);
    }

    private function fundWallet(int $centavos): Wallet
    {
        $w = Wallet::para($this->passenger);

        return $this->wallets->credit($w, $centavos, 'test_seed_' . $centavos, 'mock')->wallet;
    }

    /** @return array{0:QrPaymentToken,1:string} */
    private function issue(?int $ttl = null): array
    {
        return $ttl === null
            ? $this->qr->issueToken($this->passenger)
            : $this->qr->issueToken($this->passenger, null, $ttl);
    }

    public function test_issue_and_pay_happy_path(): void
    {
        $this->fundWallet(500000);

        $resp = $this->actingAs($this->passenger, 'sanctum')
            ->postJson('/api/v1/wallet/qr/issue')
            ->assertStatus(200)
            ->assertJson(['success' => true]);

        $qr = $resp->json('data.qr');
        $this->assertMatchesRegularExpression('/^[0-9a-f]{32}\.[0-9a-f]{64}$/', $qr);
        // Sin fares sembrados → tarifa fallback
        $this->assertSame(QrPaymentService::FALLBACK_FARE_CENTAVOS, $resp->json('data.monto_centavos'));

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr])
            ->assertStatus(200)
            ->assertJsonPath('data.monto_centavos', QrPaymentService::FALLBACK_FARE_CENTAVOS)
            ->assertJsonPath('data.contraparte', 'plataforma');

        $this->assertSame(
            500000 - QrPaymentService::FALLBACK_FARE_CENTAVOS,
            (int) Wallet::para($this->passenger)->fresh()->balance_centavos
        );

        $token = QrPaymentToken::firstWhere('token_hash', hash('sha256', explode('.', $qr)[0]));
        $this->assertNotNull($token->used_at);
        $this->assertSame($this->driver->id, (int) $token->used_by_driver_id);
        $this->assertDatabaseHas('wallet_transactions', ['reference' => "pay_{$token->id}", 'tipo' => 'debit']);
    }

    public function test_passenger_cannot_call_pay_endpoint(): void
    {
        $this->fundWallet(500000);
        [, $qr] = $this->issue();

        $this->actingAs($this->passenger, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr])
            ->assertStatus(403);
    }

    public function test_expired_token_rejected(): void
    {
        $this->fundWallet(500000);
        [, $qr] = $this->issue(-10); // ya nacido expirado

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr])
            ->assertStatus(422)
            ->assertJson(['success' => false]);

        $this->assertSame(500000, (int) Wallet::para($this->passenger)->fresh()->balance_centavos);
    }

    public function test_replay_rejected(): void
    {
        $this->fundWallet(500000);
        [, $qr] = $this->issue();

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr])->assertStatus(200);

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr])
            ->assertStatus(422)
            ->assertJson(['success' => false]);

        // Un solo débito pese a dos escaneos
        $this->assertSame(
            1,
            WalletTransaction::where('reference', 'like', 'pay_%')->count()
        );
    }

    public function test_tampered_signature_rejected(): void
    {
        $this->fundWallet(500000);
        [, $qr] = $this->issue();
        [$selector, $firma] = explode('.', $qr);
        $mala = $firma[0] === 'a' ? 'b' . substr($firma, 1) : 'a' . substr($firma, 1);

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => "{$selector}.{$mala}"])
            ->assertStatus(422);

        $this->assertSame(500000, (int) Wallet::para($this->passenger)->fresh()->balance_centavos);
        $this->assertNull(QrPaymentToken::first()->used_at);
    }

    public function test_garbage_qr_rejected(): void
    {
        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => 'no-es-un-qr'])
            ->assertStatus(422);
    }

    public function test_insufficient_balance_fails_and_token_not_consumed(): void
    {
        $this->fundWallet(1000); // menos que la tarifa
        [, $qr] = $this->issue();

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr])
            ->assertStatus(422);

        // Rollback total: used_at NO queda marcado (segunda intentada tras recargar sí puede pagar)
        $this->assertNull(QrPaymentToken::first()->fresh()->used_at);

        $this->wallets->credit(Wallet::para($this->passenger), 499000, 'recarga_extra', 'mock');
        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr])
            ->assertStatus(200);
    }

    public function test_wallet_endpoint_reports_balance_and_transactions(): void
    {
        $this->fundWallet(123456);

        $this->actingAs($this->passenger, 'sanctum')
            ->getJson('/api/v1/wallet')
            ->assertStatus(200)
            ->assertJsonPath('data.balance_centavos', 123456)
            ->assertJsonCount(1, 'data.transactions');
    }

    public function test_recharge_mock_applies_and_respects_cap(): void
    {
        // Env "testing" → permitido (phpunit.xml). Tope: 500000 centavos.
        $this->actingAs($this->passenger, 'sanctum')
            ->postJson('/api/v1/wallet/recharge-mock', ['monto_centavos' => 250000])
            ->assertStatus(200)
            ->assertJsonPath('data.balance_centavos', 250000);

        $this->actingAs($this->passenger, 'sanctum')
            ->postJson('/api/v1/wallet/recharge-mock', ['monto_centavos' => 500001])
            ->assertStatus(422);
    }
}
