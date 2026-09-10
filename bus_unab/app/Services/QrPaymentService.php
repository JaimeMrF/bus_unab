<?php

namespace App\Services;

use App\Models\Bus;
use App\Models\Fare;
use App\Models\QrPaymentToken;
use App\Models\User;
use App\Models\Wallet;
use DomainException;
use Illuminate\Support\Facades\DB;

/**
 * Emisión y cobro de tokens QR dinámicos (M3 · S3.2.4 — DISEÑO_DB.md §7).
 *
 * Flujo:
 *  1. El pasajero solicita un QR → se emite "selector.firma" (NUNCA se
 *     persiste el selector en claro; solo su SHA-256 en `token_hash`).
 *  2. La firma es HMAC-SHA256(app.key, "selector|expires_at_ts"): se
 *     recalcula al pagar; cualquier byte alterado ⇒ rechazo.
 *  3. El conductor escanea → `pay()` valida firma + vigencia y reclama el
 *     token de forma ATÓMICA (UPDATE ... WHERE used_at IS NULL): el primer
 *     escaneo gana, los reintentos reciben "ya utilizado".
 *  4. El débito a la wallet del pasajero ocurre en la MISMA transacción:
 *     si el saldo es insuficiente, todo (incluido el reclamo) se revierte.
 *
 * Adaptación de esquema (documentada): `wallets.user_id` es UNIQUE/NOT NULL,
 * así que NO existe una "wallet de plataforma" sin usuario. El recaudo del
 * pasajero se registra como UN asiento de débito con `contraparte`
 * "transportadora:{id}" (o "plataforma" si el token no tiene tenant). La
 * acreditación al tenant se derivará del ledger en la consolidación (T3.x),
 * sin duplicar asientos.
 */
class QrPaymentService
{
    public const TTL_SECONDS = 60;

    /** Tarifa de rescate en centavos cuando no hay ninguna Fare vigente ($1.850 COP). */
    public const FALLBACK_FARE_CENTAVOS = 185000;

    public function __construct(private readonly WalletService $wallets)
    {
    }

    /**
     * Emite un token efímero para el usuario. Devuelve [QrPaymentToken, qrString].
     * `$ttlSeconds` permite acortar la vigencia en tests; en producción usa TTL_SECONDS.
     */
    public function issueToken(User $user, ?int $busId = null, int $ttlSeconds = self::TTL_SECONDS): array
    {
        $wallet = Wallet::para($user);

        if (! $wallet->estaActiva()) {
            throw new DomainException('Tu wallet está congelada; contacta a soporte.');
        }

        $fare  = $this->resolveFare($busId);
        $monto = $fare?->monto_centavos ?? self::FALLBACK_FARE_CENTAVOS;

        $selector  = bin2hex(random_bytes(16)); // 32 hex chars — jamás se persiste
        $issuedAt  = now();
        $expiresAt = $issuedAt->copy()->addSeconds($ttlSeconds);
        $signature = $this->sign($selector, $expiresAt->getTimestamp());

        $token = QrPaymentToken::create([
            'user_id'                 => $user->id,
            'wallet_id'               => $wallet->id,
            'fare_id'                 => $fare?->id,
            'transportadora_id'       => $fare?->transportadora_id,
            'bus_id'                  => $busId,
            'token_hash'              => hash('sha256', $selector),
            'monto_snapshot_centavos' => $monto,
            'issued_at'               => $issuedAt,
            'expires_at'              => $expiresAt,
        ]);

        return [$token, "{$selector}.{$signature}"];
    }

    /**
     * Cobra un QR en nombre del conductor/scanner. Devuelve [QrPaymentToken, WalletTransaction].
     *
     * @throws DomainException              QR inexistente, manipulado, expirado o reutilizado
     * @throws \App\Exceptions\InsufficientFundsException saldo insuficiente (rollback total)
     */
    public function pay(string $qr, User $driver): array
    {
        [$selector, $signature] = $this->parse($qr);

        $token = QrPaymentToken::where('token_hash', hash('sha256', $selector))->first();

        if (! $token) {
            throw new DomainException('QR inválido o inexistente.');
        }

        // La firma se recalcula contra el expires_at ALMACENADO: cualquier
        // alteración del payload (o intento de reutilizar otra expiración) falla.
        if (! hash_equals($this->sign($selector, $token->expires_at->getTimestamp()), $signature)) {
            throw new DomainException('QR inválido: la firma no coincide (manipulado).');
        }

        if ($token->estaExpirado()) {
            throw new DomainException('El QR expiró; pídele al pasajero que genere uno nuevo.');
        }

        $asiento = DB::transaction(function () use ($token, $driver) {
            // Reclamo one-time atómico: solo el primer escaneo afecta 1 fila.
            $claimed = DB::table('qr_payment_tokens')
                ->where('id', $token->id)
                ->whereNull('used_at')
                ->update([
                    'used_at'           => now(),
                    'used_by_driver_id' => $driver->id,
                    'updated_at'        => now(),
                ]);

            if ($claimed !== 1) {
                throw new DomainException('Este QR ya fue utilizado.');
            }

            $wallet = Wallet::whereKey($token->wallet_id)->lockForUpdate()->firstOrFail();

            // Ver nota de clase: sin wallet de plataforma, la contraparte del
            // débito del pasajero identifica a quién se le acredita el recaudo.
            $contraparte = $token->transportadora_id
                ? "transportadora:{$token->transportadora_id}"
                : 'plataforma';

            $move = $this->wallets->debit(
                $wallet,
                (int) $token->monto_snapshot_centavos,
                "pay_{$token->id}",
                $contraparte,
                ['qr_token_id' => $token->id, 'bus_id' => $token->bus_id],
            );

            DB::table('qr_payment_tokens')
                ->where('id', $token->id)
                ->update(['reference' => $move->reference]);

            return $move;
        });

        $token->refresh();

        return [$token, $asiento];
    }

    /** Descompone "selector.firma" validando formatos hex esperados. */
    private function parse(string $qr): array
    {
        $parts = explode('.', trim($qr));

        if (count($parts) !== 2) {
            throw new DomainException('Formato de QR inválido (se esperaba selector.firma).');
        }

        [$selector, $signature] = array_map(static fn ($p) => strtolower(trim($p)), $parts);

        if (! preg_match('/^[0-9a-f]{32}$/', $selector) || ! preg_match('/^[0-9a-f]{64}$/', $signature)) {
            throw new DomainException('Formato de QR inválido (hex esperados).');
        }

        return [$selector, $signature];
    }

    /** Firma HMAC-SHA256 con la APP_KEY: selector|expiry — coherente issue/pay. */
    private function sign(string $selector, int $expiresAtTs): string
    {
        return hash_hmac('sha256', "{$selector}|{$expiresAtTs}", (string) config('app.key'));
    }

    /**
     * Resuelve la tarifa vigente:
     *  1. Si el bus es asignable a una transportadora (la columna
     *     `buses.transportadora_id` la añade la ola de tenancy T3.1; hoy no
     *     existe y getAttribute() devuelve null → paso 2), usa su tarifa.
     *  2. Tarifa global vigente (transportadora_id NULL).
     *  3. null → el llamador aplica FALLBACK_FARE_CENTAVOS.
     */
    private function resolveFare(?int $busId): ?Fare
    {
        $transportadoraId = null;

        if ($busId !== null) {
            $bus = Bus::find($busId);
            // getAttribute() es seguro aunque la columna aún no exista.
            $transportadoraId = $bus?->getAttribute('transportadora_id');
        }

        if ($transportadoraId !== null) {
            $f = Fare::vigente()
                ->where('transportadora_id', $transportadoraId)
                ->orderBy('monto_centavos')
                ->first();

            if ($f !== null) {
                return $f;
            }
        }

        return Fare::vigente()
            ->whereNull('transportadora_id')
            ->orderBy('monto_centavos')
            ->first();
    }
}
