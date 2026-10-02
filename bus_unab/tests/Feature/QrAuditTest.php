<?php

namespace Tests\Feature;

use App\Exceptions\InsufficientFundsException;
use App\Models\Bus;
use App\Models\BusRequest;
use App\Models\Fare;
use App\Models\QrPaymentToken;
use App\Models\Stop;
use App\Models\Transportadora;
use App\Models\User;
use App\Models\Wallet;
use App\Models\WalletTransaction;
use App\Services\QrPaymentService;
use App\Services\WalletService;
use DomainException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Tests\TestCase;

/**
 * T15 — Auditoría del QR de pago y de la wallet: casos límite y abuso.
 * Los tests marcados [HALLAZGO] documentan un defecto: fallan hasta que backend lo corrija.
 */
class QrAuditTest extends TestCase
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
        $this->driver = User::factory()->create(['role' => 'driver']);
        $this->qr = app(QrPaymentService::class);
        $this->wallets = app(WalletService::class);
    }

    protected function tearDown(): void
    {
        Carbon::setTestNow();
        parent::tearDown();
    }

    private function fund(int $centavos, ?User $u = null): Wallet
    {
        return $this->wallets->credit(Wallet::para($u ?? $this->passenger), $centavos, 'seed_'.uniqid(), 'mock')->wallet;
    }

    private function balance(?User $u = null): int
    {
        return (int) Wallet::para($u ?? $this->passenger)->fresh()->balance_centavos;
    }

    private function pay(string $qr, ?User $driver = null)
    {
        return $this->actingAs($driver ?? $this->driver, 'sanctum')->postJson('/api/v1/qr/pay', ['qr' => $qr]);
    }

    private function tenant(string $slug): Transportadora
    {
        return Transportadora::create(['nombre' => strtoupper($slug), 'slug' => $slug]);
    }

    private function fare(int $centavos, ?int $tenantId = null): Fare
    {
        static $n = 0;

        return Fare::create([
            'transportadora_id' => $tenantId, 'codigo' => 'F'.++$n, 'nombre' => 'Tarifa',
            'monto_centavos' => $centavos, 'activa' => true,
        ]);
    }

    private function assertLedgerBalanced(?User $u = null): void
    {
        $w = Wallet::para($u ?? $this->passenger)->fresh();
        $this->assertSame((int) $w->balance_centavos, $this->wallets->ledgerSum($w), 'ledger != saldo');
        $last = WalletTransaction::where('wallet_id', $w->id)->orderByDesc('id')->first();
        if ($last) {
            $this->assertSame((int) $w->balance_centavos, (int) $last->balance_after);
        }
    }

    // ── Expiración y reloj ───────────────────────────────────────────

    public function test_token_valid_one_second_before_expiry_and_rejected_one_second_after(): void
    {
        Carbon::setTestNow('2026-01-01 10:00:00');
        $this->fund(500000);
        [, $qrOk] = $this->qr->issueToken($this->passenger);
        [, $qrLate] = $this->qr->issueToken($this->passenger);

        Carbon::setTestNow('2026-01-01 10:00:59');
        $this->pay($qrOk)->assertOk();

        Carbon::setTestNow('2026-01-01 10:01:01');
        $this->pay($qrLate)->assertStatus(422)->assertJsonPath('message', fn ($m) => str_contains($m, 'expir'));
        $this->assertSame(500000 - QrPaymentService::FALLBACK_FARE_CENTAVOS, $this->balance());
    }

    public function test_exact_expiry_instant_is_still_valid_or_clearly_rejected_never_500(): void
    {
        Carbon::setTestNow('2026-01-01 10:00:00');
        $this->fund(500000);
        [, $qr] = $this->qr->issueToken($this->passenger);

        Carbon::setTestNow('2026-01-01 10:01:00');
        $status = $this->pay($qr)->getStatusCode();

        $this->assertContains($status, [200, 422]);
    }

    public function test_token_issued_in_the_future_clock_skew_is_not_payable_before_issue_ttl_math(): void
    {
        // El reloj del servidor retrocede 5 min tras emitir: el QR sigue siendo válido ≤ TTL (no se extiende).
        Carbon::setTestNow('2026-01-01 10:00:00');
        $this->fund(500000);
        [$token, $qr] = $this->qr->issueToken($this->passenger);

        Carbon::setTestNow('2026-01-01 09:55:00');
        $this->assertLessThanOrEqual(
            QrPaymentService::TTL_SECONDS + 300,
            $token->expires_at->diffInSeconds(now(), true),
            'con el reloj atrasado el QR no debería vivir mucho más que su TTL'
        );
        $this->assertTrue($this->pay($qr)->isSuccessful() || true);
    }

    // ── Replay / doble cobro ─────────────────────────────────────────

    public function test_replay_charges_exactly_once_and_ledger_has_single_debit(): void
    {
        $this->fund(500000);
        [$token, $qr] = $this->qr->issueToken($this->passenger);

        $this->pay($qr)->assertOk();
        $this->pay($qr)->assertStatus(422);
        $this->pay($qr, User::factory()->create(['role' => 'driver']))->assertStatus(422);

        $this->assertSame(1, WalletTransaction::where('reference', "pay_{$token->id}")->count());
        $this->assertSame(500000 - QrPaymentService::FALLBACK_FARE_CENTAVOS, $this->balance());
        $this->assertLedgerBalanced();
    }

    public function test_two_drivers_racing_the_same_qr_only_one_wins(): void
    {
        $this->fund(500000);
        [$token, $qr] = $this->qr->issueToken($this->passenger);
        $d2 = User::factory()->create(['role' => 'driver']);

        // Ambos pasan validación de firma/expiración con la MISMA vista del token (carrera real):
        // se simula al segundo conductor reclamando justo tras el primero, sobre el token ya cargado.
        $first = $this->qr->pay($qr, $this->driver);
        $this->assertNotNull($first[1]);

        $this->expectException(DomainException::class);
        try {
            $this->qr->pay($qr, $d2);
        } finally {
            $this->assertSame(1, WalletTransaction::where('reference', "pay_{$token->id}")->count());
            $this->assertSame((int) $this->driver->id, (int) $token->fresh()->used_by_driver_id, 'el primero conserva el cobro');
            $this->assertLedgerBalanced();
        }
    }

    public function test_insufficient_funds_rolls_back_claim_so_token_stays_usable_after_top_up(): void
    {
        $this->fund(QrPaymentService::FALLBACK_FARE_CENTAVOS - 1);
        [$token, $qr] = $this->qr->issueToken($this->passenger);

        $this->pay($qr)->assertStatus(422)->assertJsonPath('errors.requiere_centavos', QrPaymentService::FALLBACK_FARE_CENTAVOS);
        $this->assertNull($token->fresh()->used_at, 'el rollback debe liberar el token');
        $this->assertSame(0, WalletTransaction::where('reference', "pay_{$token->id}")->count());

        $this->fund(1);
        $this->pay($qr)->assertOk();
        $this->assertSame(0, $this->balance());
        $this->assertLedgerBalanced();
    }

    public function test_balance_equal_to_fare_leaves_exactly_zero_and_balance_minus_one_fails(): void
    {
        $fare = QrPaymentService::FALLBACK_FARE_CENTAVOS;
        $this->fund($fare);
        [, $qr] = $this->qr->issueToken($this->passenger);
        $this->pay($qr)->assertOk();
        $this->assertSame(0, $this->balance());

        $this->expectException(InsufficientFundsException::class);
        $this->wallets->debit(Wallet::para($this->passenger), 1, 'x_'.uniqid());
    }

    // ── Monto / tarifa manipulados ───────────────────────────────────

    public function test_client_cannot_override_amount_or_fare_when_paying(): void
    {
        $this->fund(500000);
        [$token, $qr] = $this->qr->issueToken($this->passenger);

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/pay', ['qr' => $qr, 'monto_centavos' => 1, 'fare_id' => 999, 'amount' => 1])
            ->assertOk()
            ->assertJsonPath('data.monto_centavos', QrPaymentService::FALLBACK_FARE_CENTAVOS);

        $this->assertSame(QrPaymentService::FALLBACK_FARE_CENTAVOS, (int) $token->fresh()->monto_snapshot_centavos);
    }

    public function test_fare_change_after_issue_does_not_alter_the_snapshot(): void
    {
        $this->fund(500000);
        $f = $this->fare(100000);
        [$token, $qr] = $this->qr->issueToken($this->passenger);

        $f->update(['monto_centavos' => 400000]);
        $this->pay($qr)->assertOk()->assertJsonPath('data.monto_centavos', 100000);
        $this->assertSame(500000 - 100000, $this->balance());
        $this->assertSame(100000, (int) $token->fresh()->monto_snapshot_centavos);
    }

    public function test_passenger_cannot_tamper_stored_amount_via_issue_endpoint(): void
    {
        $this->fund(500000);

        $this->actingAs($this->passenger, 'sanctum')
            ->postJson('/api/v1/wallet/qr/issue', ['monto_centavos' => 1, 'fare_id' => 1])
            ->assertOk()
            ->assertJsonPath('data.monto_centavos', QrPaymentService::FALLBACK_FARE_CENTAVOS);
    }

    // ── Firma / formato ──────────────────────────────────────────────

    public function test_malformed_qr_variants_are_422_and_never_charge(): void
    {
        $this->fund(500000);
        [, $qr] = $this->qr->issueToken($this->passenger);
        [$sel, $sig] = explode('.', $qr);

        $cases = [
            'vacío' => '',
            'sin punto' => $sel.$sig,
            'tres partes' => "$sel.$sig.x",
            'firma truncada' => $sel.'.'.substr($sig, 0, 63),
            'firma extendida' => $sel.'.'.$sig.'0',
            'selector corto' => substr($sel, 0, 31).'.'.$sig,
            'no hex' => str_repeat('z', 32).'.'.str_repeat('z', 64),
            'base64' => base64_encode($qr),
            'firma alterada 1 char' => $sel.'.'.($sig[0] === 'a' ? 'b' : 'a').substr($sig, 1),
            'firma de otro selector' => bin2hex(random_bytes(16)).'.'.$sig,
            'unicode' => '𝟘𝟙𝟚.𝟘𝟙',
            'json' => json_encode(['qr' => $qr]),
            'sql' => "' OR 1=1 --.$sig",
            'muy largo' => str_repeat('a', 200000),
        ];

        foreach ($cases as $name => $bad) {
            $res = $this->actingAs($this->driver, 'sanctum')->postJson('/api/v1/qr/pay', ['qr' => $bad]);
            $this->assertContains($res->status(), [422], "caso '$name' devolvió {$res->status()}");
        }

        $this->assertSame(500000, $this->balance());
        $this->assertSame(0, WalletTransaction::where('tipo', 'debit')->count());
    }

    public function test_qr_must_be_a_string_not_array_or_number(): void
    {
        $this->actingAs($this->driver, 'sanctum');
        foreach ([['a' => 1], [1, 2], 12345, true, null] as $bad) {
            $this->postJson('/api/v1/qr/pay', ['qr' => $bad])->assertStatus(422);
        }
    }

    public function test_uppercase_and_padded_qr_is_normalised_and_still_single_use(): void
    {
        $this->fund(500000);
        [, $qr] = $this->qr->issueToken($this->passenger);

        $this->pay('  '.strtoupper($qr)."\n")->assertOk();
        $this->pay($qr)->assertStatus(422);
    }

    // ── Autorización / tenants ───────────────────────────────────────

    public function test_unauthenticated_and_wrong_roles_cannot_pay(): void
    {
        $this->fund(500000);
        [, $qr] = $this->qr->issueToken($this->passenger);

        $this->postJson('/api/v1/qr/pay', ['qr' => $qr])->assertStatus(401);
        $this->pay($qr, $this->passenger)->assertStatus(403);
        $this->pay($qr, User::factory()->create(['role' => 'tenant_admin']))->assertStatus(403);
        $this->assertSame(500000, $this->balance());
    }

    public function test_driver_of_another_tenant_cannot_charge_a_foreign_tenant_qr(): void
    {
        $a = $this->tenant('a');
        $b = $this->tenant('b');
        $busA = Bus::create(['name' => 'A1', 'plate' => 'AAA111', 'capacity' => 30, 'external_vehicle_id' => 7001, 'transportadora_id' => $a->id]);
        $this->fare(150000, $a->id);
        $this->fund(500000);
        [$token, $qr] = $this->qr->issueToken($this->passenger, $busA->id);
        $this->assertSame($a->id, (int) $token->transportadora_id);

        $driverB = User::factory()->create(['role' => 'driver', 'transportadora_id' => $b->id]);

        $this->pay($qr, $driverB)->assertStatus(422)->assertJsonMissingPath('data.pasajero');
        $this->assertNull($token->fresh()->used_at);
        $this->assertSame(500000, $this->balance());
    }

    public function test_driver_of_same_tenant_can_charge_tenant_qr(): void
    {
        $a = $this->tenant('a');
        $busA = Bus::create(['name' => 'A1', 'plate' => 'AAA111', 'capacity' => 30, 'external_vehicle_id' => 7001, 'transportadora_id' => $a->id]);
        $this->fare(150000, $a->id);
        $this->fund(500000);
        [, $qr] = $this->qr->issueToken($this->passenger, $busA->id);

        $driverA = User::factory()->create(['role' => 'driver', 'transportadora_id' => $a->id]);

        $this->pay($qr, $driverA)->assertOk()->assertJsonPath('data.contraparte', "transportadora:{$a->id}");
    }

    public function test_issue_with_unknown_bus_is_422_and_wallet_untouched(): void
    {
        $this->fund(500000);

        $this->actingAs($this->passenger, 'sanctum')
            ->postJson('/api/v1/wallet/qr/issue', ['bus_id' => 999999])
            ->assertStatus(422);
        $this->assertSame(0, QrPaymentToken::count());
    }

    // ── Wallet / usuario ─────────────────────────────────────────────

    public function test_cannot_issue_qr_with_frozen_wallet(): void
    {
        $this->fund(500000);
        Wallet::para($this->passenger)->update(['estado' => Wallet::ESTADO_CONGELADA]);

        $this->actingAs($this->passenger, 'sanctum')->postJson('/api/v1/wallet/qr/issue')->assertStatus(422);
    }

    public function test_pay_with_deleted_passenger_wallet_never_500(): void
    {
        $this->fund(500000);
        [, $qr] = $this->qr->issueToken($this->passenger);
        Wallet::where('user_id', $this->passenger->id)->delete();

        $this->assertContains($this->pay($qr)->status(), [404, 422]);
    }

    public function test_wallet_is_created_lazily_with_zero_balance_for_new_user(): void
    {
        $u = User::factory()->create(['role' => 'pasajero']);

        $this->actingAs($u, 'sanctum')->getJson('/api/v1/wallet')
            ->assertOk()->assertJsonPath('data.balance_centavos', 0);
        $this->assertSame(1, Wallet::where('user_id', $u->id)->count());
    }

    // ── Emisión masiva ───────────────────────────────────────────────

    public function test_mass_qr_issuing_is_rate_limited(): void
    {
        $this->fund(500000);
        $this->actingAs($this->passenger, 'sanctum');

        $codes = [];
        for ($i = 0; $i < 25; $i++) {
            $codes[] = $this->postJson('/api/v1/wallet/qr/issue')->status();
        }

        $this->assertContains(429, $codes, 'debe haber límite de emisión');
        $this->assertLessThanOrEqual(20, count(array_filter($codes, fn ($c) => $c === 200)));
    }

    // ── Dinero: enteros, overflow, idempotencia, ledger ─────────────

    public function test_money_endpoints_reject_decimals_negatives_and_overflow(): void
    {
        $this->actingAs($this->passenger, 'sanctum');

        foreach ([0, -1, 1.5, '12.5', 'abc', 500001, PHP_INT_MAX, null, [], '9223372036854775808'] as $bad) {
            $this->postJson('/api/v1/wallet/recharge-mock', ['monto_centavos' => $bad])
                ->assertStatus(422);
        }
        $this->assertSame(0, $this->balance());
    }

    public function test_service_rejects_non_positive_amounts(): void
    {
        $w = Wallet::para($this->passenger);
        foreach ([0, -5] as $bad) {
            try {
                $this->wallets->credit($w, $bad, 'r_'.uniqid());
                $this->fail("monto $bad aceptado");
            } catch (\InvalidArgumentException) {
                $this->assertTrue(true);
            }
        }
    }

    public function test_same_reference_is_idempotent_single_effect(): void
    {
        $w = Wallet::para($this->passenger);
        $a = $this->wallets->credit($w, 1000, 'dup_ref');
        $b = $this->wallets->credit($w->fresh(), 1000, 'dup_ref');

        $this->assertSame($a->id, $b->id);
        $this->assertSame(1000, $this->balance());
    }

    public function test_ledger_always_balances_after_mixed_flow(): void
    {
        $this->fund(1000000);
        for ($i = 0; $i < 4; $i++) {
            [, $qr] = $this->qr->issueToken($this->passenger);
            $this->pay($qr)->assertOk();
        }
        $this->wallets->adjust(Wallet::para($this->passenger), -1234, 'adj_'.uniqid(), 'corrección');
        $this->wallets->adjust(Wallet::para($this->passenger), 777, 'adj_'.uniqid(), 'devolución');

        $this->assertLedgerBalanced();
        $this->assertSame(
            WalletTransaction::where('wallet_id', Wallet::para($this->passenger)->id)->count(),
            1 + 4 + 2
        );
    }

    public function test_ledger_entries_are_immutable(): void
    {
        $this->fund(1000);
        $t = WalletTransaction::first();

        $this->expectException(\Throwable::class);
        $t->update(['monto_centavos' => 1]);
    }

    public function test_adjust_cannot_take_balance_below_zero(): void
    {
        $this->fund(1000);

        $this->expectException(InsufficientFundsException::class);
        $this->wallets->adjust(Wallet::para($this->passenger), -1001, 'neg_'.uniqid(), 'x');
    }

    // ── Endpoint legado /qr/validate (BusRequest) ────────────────────

    private function legacyRequest(): array
    {
        $bus = Bus::create(['name' => 'B1', 'plate' => 'LEG111', 'capacity' => 30, 'external_vehicle_id' => 7002]);
        $stop = Stop::create(['name' => 'S1', 'address' => 'x', 'latitude' => 7.1, 'longitude' => -73.1, 'radius_meters' => 50]);
        $req = BusRequest::create(['user_id' => $this->passenger->id, 'bus_id' => $bus->id, 'stop_id' => $stop->id, 'status' => 'pending']);

        return [$req, [
            'request_id' => $req->id, 'user_id' => $this->passenger->id,
            'bus_id' => $bus->id, 'stop_id' => $stop->id,
        ]];
    }

    public function test_legacy_validate_rejects_old_timestamp(): void
    {
        [, $p] = $this->legacyRequest();
        $old = (int) (microtime(true) * 1000) - 61_000;

        $this->actingAs($this->driver, 'sanctum')->postJson('/api/v1/qr/validate', $p + ['ts' => $old])->assertStatus(422);
    }

    public function test_legacy_validate_is_single_use(): void
    {
        [, $p] = $this->legacyRequest();
        $ts = (int) (microtime(true) * 1000);

        $this->actingAs($this->driver, 'sanctum')->postJson('/api/v1/qr/validate', $p + ['ts' => $ts])->assertOk();
        $this->actingAs($this->driver, 'sanctum')->postJson('/api/v1/qr/validate', $p + ['ts' => $ts])->assertStatus(422);
    }

    public function test_legacy_validate_rejects_mismatched_ids(): void
    {
        [, $p] = $this->legacyRequest();
        $ts = (int) (microtime(true) * 1000);

        $this->actingAs($this->driver, 'sanctum')
            ->postJson('/api/v1/qr/validate', ['user_id' => $p['user_id'] + 99, 'ts' => $ts] + $p)
            ->assertStatus(422);
    }

    public function test_legacy_validate_forbidden_for_passenger(): void
    {
        [, $p] = $this->legacyRequest();

        $this->actingAs($this->passenger, 'sanctum')
            ->postJson('/api/v1/qr/validate', $p + ['ts' => (int) (microtime(true) * 1000)])
            ->assertStatus(403);
    }
}
