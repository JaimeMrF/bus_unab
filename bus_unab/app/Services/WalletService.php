<?php

namespace App\Services;

use App\Exceptions\InsufficientFundsException;
use App\Models\Wallet;
use App\Models\WalletTransaction;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use InvalidArgumentException;
use RuntimeException;

/**
 * Único escriba permitido de saldos y del ledger (M3 · S3.2.2).
 *
 * Reglas (DISEÑO_DB.md §5):
 *  1. Todo movimiento vive en la MISMA transacción DB: UPDATE del saldo
 *     cacheado (con lockForUpdate) + INSERT del asiento con balance_after.
 *  2. `reference` es UNIQUE → idempotencia: reintento con la misma
 *     reference devuelve el asiento existente sin duplicar el efecto.
 *  3. Débito con saldo insuficiente → InsufficientFundsException (rollback).
 *  4. Asientos nunca se mutan: el modelo WalletTransaction bloquea
 *     update/delete a nivel de aplicación.
 *
 * Nota SQLite: `SELECT … FOR UPDATE` no existe; Laravel lo omite en silencio
 * (DISEÑO_DB §12). La guarda real es la relectura dentro de la transacción
 * (`balance >= monto`), que es correcta también en MySQL/InnoDB.
 */
class WalletService
{
    /**
     * Acreencia: suma al saldo. `$reference` null → se genera única.
     */
    public function credit(Wallet $wallet, int $montoCentavos, ?string $reference = null, ?string $contraparte = null, array $metadata = []): WalletTransaction
    {
        return $this->move($wallet, $montoCentavos, WalletTransaction::TIPO_CREDITO, $reference, $contraparte, $metadata);
    }

    /**
     * Debito: resta del saldo. Lanza InsufficientFundsException si no alcanza.
     */
    public function debit(Wallet $wallet, int $montoCentavos, ?string $reference = null, ?string $contraparte = null, array $metadata = []): WalletTransaction
    {
        return $this->move($wallet, $montoCentavos, WalletTransaction::TIPO_DEBITO, $reference, $contraparte, $metadata);
    }

    /**
     * Corrección manual con signo (Super Admin). `$montoCentavos` admite
     * negativo; el asiento guarda monto absoluto + metadata['signo'].
     */
    public function adjust(Wallet $wallet, int $montoCentavos, string $reference, string $motivo, ?string $createdBy = null): WalletTransaction
    {
        if ($montoCentavos === 0) {
            throw new InvalidArgumentException('El ajuste no puede ser 0; use un asiento inverso explícito.');
        }

        return $this->move(
            $wallet,
            abs($montoCentavos),
            WalletTransaction::TIPO_AJUSTE,
            $reference,
            'ajuste:' . $motivo,
            ['signo' => $montoCentavos > 0 ? 1 : -1, 'created_by' => $createdBy]
        );
    }

    /**
     * Núcleo transaccional. Public por conveniencia de tests; mejor usar
     * credit()/debit()/adjust().
     */
    public function move(Wallet $wallet, int $montoCentavos, string $tipo, ?string $reference, ?string $contraparte = null, array $metadata = []): WalletTransaction
    {
        if ($montoCentavos <= 0 && $tipo !== WalletTransaction::TIPO_AJUSTE) {
            throw new InvalidArgumentException('El monto debe ser positivo en centavos.');
        }

        if (! in_array($tipo, [WalletTransaction::TIPO_CREDITO, WalletTransaction::TIPO_DEBITO, WalletTransaction::TIPO_AJUSTE], true)) {
            throw new InvalidArgumentException("Tipo de asiento desconocido: {$tipo}");
        }

        $reference ??= Str::uuid()->toString();
        $signo = match ($tipo) {
            WalletTransaction::TIPO_DEBITO => -1,
            WalletTransaction::TIPO_CREDITO => 1,
            default => (int) ($metadata['signo'] ?? 1),
        };

        return DB::transaction(function () use ($wallet, $montoCentavos, $tipo, $reference, $contraparte, $metadata, $signo) {
            // Relectura con lock: trabajamos sobre el saldo vigente, no el cache del request.
            $locked = Wallet::whereKey($wallet->getKey())->lockForUpdate()->firstOrFail();

            // Idempotencia por reference: el reintento devuelve el asiento previo, cero efecto extra.
            $existe = WalletTransaction::where('reference', $reference)->first();
            if ($existe !== null) {
                return $existe;
            }

            if (! $locked->estaActiva()) {
                throw new RuntimeException('Wallet congelada: no se admiten movimientos.');
            }

            $nuevoSaldo = $locked->balance_centavos + ($signo * $montoCentavos);

            if ($nuevoSaldo < 0) {
                throw new InsufficientFundsException($locked->balance_centavos, $montoCentavos);
            }

            // El modelo Wallet sí es mutable (es proyección); el ledger no.
            $locked->balance_centavos = $nuevoSaldo;
            $locked->version = $locked->version + 1;
            $locked->save();

            // El modelo Wallet se toca por property+save, no por update(),
            // para no disparar eventos que otro GlobalScope interprete mal.

            return WalletTransaction::create([
                'wallet_id'      => $locked->getKey(),
                'tipo'           => $tipo,
                'monto_centavos' => $montoCentavos,
                'balance_after'  => $nuevoSaldo,
                'reference'      => $reference,
                'contraparte'    => $contraparte,
                'metadata'       => $metadata ?: null,
            ]);
        });
    }

    /**
     * Suma del ledger para una wallet (créditos − débitos ± ajustes).
     * Debe coincidir SIEMPRE con balance_centavos (prueba de reconexión).
     */
    public function ledgerSum(Wallet $wallet): int
    {
        $sum = 0;
        WalletTransaction::where('wallet_id', $wallet->getKey())->orderBy('id')->each(function (WalletTransaction $t) use (&$sum) {
            $signo = match ($t->tipo) {
                WalletTransaction::TIPO_CREDITO => 1,
                WalletTransaction::TIPO_DEBITO => -1,
                default => (int) ($t->metadata['signo'] ?? 1),
            };
            $sum += $signo * $t->monto_centavos;
        });

        return $sum;
    }
}
