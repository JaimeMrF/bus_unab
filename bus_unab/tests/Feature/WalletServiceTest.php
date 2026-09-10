<?php

namespace Tests\Feature;

use App\Exceptions\InsufficientFundsException;
use App\Models\User;
use App\Models\Wallet;
use App\Models\WalletTransaction;
use App\Services\WalletService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * M3 · S3.2.2 — WalletService: crédito/débito transaccional, idempotencia
 * por `reference` e invariante del ledger (DISEÑO_DB.md §5).
 */
class WalletServiceTest extends TestCase
{
    use RefreshDatabase;

    private WalletService $service;
    private Wallet $wallet;

    protected function setUp(): void
    {
        parent::setUp();
        $user           = User::factory()->create();
        $this->wallet   = Wallet::para($user);
        $this->service  = app(WalletService::class);
    }

    public function test_credit_increments_balance_and_writes_ledger(): void
    {
        $tx = $this->service->credit($this->wallet, 50000, 'rec_1', 'mock');

        $this->wallet->refresh();
        $this->assertSame(50000, (int) $this->wallet->balance_centavos);
        $this->assertSame(1, (int) $this->wallet->version);
        $this->assertSame(WalletTransaction::TIPO_CREDITO, $tx->tipo);
        $this->assertSame(50000, (int) $tx->balance_after);
        $this->assertSame('rec_1', $tx->reference);
        $this->assertSame('mock', $tx->contraparte);
    }

    public function test_debit_reduces_balance_and_writes_ledger(): void
    {
        $this->service->credit($this->wallet, 100000, 'rec_2', 'mock');
        $tx = $this->service->debit($this->wallet, 18500, 'pay_2', 'plataforma');

        $this->wallet->refresh();
        $this->assertSame(81500, (int) $this->wallet->balance_centavos);
        $this->assertSame(2, (int) $this->wallet->version);
        $this->assertSame(WalletTransaction::TIPO_DEBITO, $tx->tipo);
        $this->assertSame(81500, (int) $tx->balance_after);
    }

    public function test_debit_with_insufficient_funds_throws_and_keeps_state(): void
    {
        $this->service->credit($this->wallet, 1000, 'rec_3', 'mock');

        try {
            $this->service->debit($this->wallet, 2000, 'pay_3', 'plataforma');
            $this->fail('Expected InsufficientFundsException');
        } catch (InsufficientFundsException $e) {
            $this->assertSame(1000, $e->saldoCentavos);
            $this->assertSame(2000, $e->montoCentavos);
        }

        $this->wallet->refresh();
        $this->assertSame(1000, (int) $this->wallet->balance_centavos);
        $this->assertSame(1, (int) $this->wallet->version);
        // El asiento del intento fallido NO existe (rollback).
        $this->assertDatabaseMissing('wallet_transactions', ['reference' => 'pay_3']);
    }

    public function test_same_reference_twice_has_one_effect(): void
    {
        $a = $this->service->credit($this->wallet, 20000, 'dup_ref', 'mock');
        $b = $this->service->credit($this->wallet, 20000, 'dup_ref', 'mock');

        $this->wallet->refresh();
        $this->assertSame($a->id, $b->id);                    // devuelve el asiento previo
        $this->assertSame(20000, (int) $this->wallet->balance_centavos); // cero efecto extra
        $this->assertSame(1, WalletTransaction::where('reference', 'dup_ref')->count());
    }

    public function test_ledger_sum_invariant_after_mixed_movements(): void
    {
        $this->service->credit($this->wallet, 100000, 'rec_a', 'mock');
        $this->service->debit($this->wallet, 18500, 'pay_a', 'plataforma');
        $this->service->credit($this->wallet, 50000, 'rec_b', 'mock');
        $this->service->debit($this->wallet, 31000, 'pay_b', 'plataforma');

        $sum = (int) WalletTransaction::where('wallet_id', $this->wallet->id)
            ->selectRaw("COALESCE(SUM(CASE WHEN tipo = 'credit' THEN monto_centavos ELSE 0 END), 0)
                        - COALESCE(SUM(CASE WHEN tipo = 'debit' THEN monto_centavos ELSE 0 END), 0) AS saldo")
            ->value('saldo');

        $this->wallet->refresh();
        $this->assertSame($sum, (int) $this->wallet->balance_centavos);
        $this->assertSame(100500, $sum);
    }

    public function test_sequential_double_debit_only_pays_what_balance_covers(): void
    {
        $this->service->credit($this->wallet, 200000, 'rec_c', 'mock');

        $first = $this->service->debit($this->wallet, 185000, 'pay_c1', 'plataforma');
        $this->assertSame(15000, (int) $first->balance_after);

        $this->expectException(InsufficientFundsException::class);
        $this->service->debit($this->wallet, 185000, 'pay_c2', 'plataforma');
    }
}
