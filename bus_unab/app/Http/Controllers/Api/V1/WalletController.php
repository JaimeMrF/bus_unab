<?php

namespace App\Http\Controllers\Api\V1;

use App\Exceptions\InsufficientFundsException;
use App\Models\Concerns\GlobalTenantScope;
use App\Models\User;
use App\Models\Wallet;
use App\Services\QrPaymentService;
use App\Services\WalletService;
use DomainException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

/**
 * Wallet prepago + emisión/cobro de QR dinámico (M3 · S3.2.2 y S3.2.4).
 *
 * Único escriba del saldo: WalletService (nunca tocar balance_centavos a mano).
 * El endpoint pay() vive aquí (no en QrController) para no tocar la ruta
 * existente /qr/validate, que sigue operativa e intacta.
 */
class WalletController extends BaseController
{
    public function __construct(
        private readonly WalletService $wallets,
        private readonly QrPaymentService $qrService,
    ) {
    }

    /**
     * GET /api/v1/wallet — saldo actual + últimos 20 asientos del ledger.
     */
    public function show(Request $request): JsonResponse
    {
        $wallet = Wallet::para($request->user());

        $transactions = $wallet->transactions()
            ->latest('id')
            ->limit(20)
            ->get(['id', 'tipo', 'monto_centavos', 'balance_after', 'reference', 'contraparte', 'created_at']);

        return $this->success([
            'wallet_id'        => $wallet->id,
            'balance_centavos' => (int) $wallet->balance_centavos,
            'estado'           => $wallet->estado,
            'version'          => (int) $wallet->version,
            'transactions'     => $transactions,
        ]);
    }

    /**
     * POST /api/v1/wallet/recharge-mock — recarga de prueba (SOLO local/testing).
     * Tope 500000 centavos; reference 'mock_{uuid}' para idempotencia/auditoría.
     */
    public function rechargeMock(Request $request): JsonResponse
    {
        if (! app()->environment('local', 'testing')) {
            return $this->forbidden('Recarga mock deshabilitada fuera de local/testing.');
        }

        $data = $request->validate([
            'monto_centavos' => 'required|integer|min:1|max:500000',
        ]);

        $wallet = Wallet::para($request->user());
        $ref    = 'mock_' . Str::uuid()->toString();

        try {
            $asiento = $this->wallets->credit($wallet, (int) $data['monto_centavos'], $ref, 'mock');
        } catch (InsufficientFundsException $e) {
            return $this->error($e->getMessage(), 422);
        }

        return $this->success([
            'balance_centavos' => (int) $wallet->fresh()->balance_centavos,
            'transaction_id'   => $asiento->id,
            'reference'        => $asiento->reference,
        ], 'Recarga mock aplicada');
    }

    /**
     * POST /api/v1/wallet/qr/issue — pasajero genera su QR dinámico.
     * Body opcional: { bus_id }.
     */
    public function issueQr(Request $request): JsonResponse
    {
        $data = $request->validate([
            'bus_id' => 'nullable|integer|exists:buses,id',
        ]);

        try {
            [$token, $qr] = $this->qrService->issueToken($request->user(), $data['bus_id'] ?? null);
        } catch (DomainException $e) {
            return $this->error($e->getMessage(), 422);
        }

        return $this->success([
            'qr'             => $qr,               // "selector.firma" — rota cada TTL
            'token_id'       => $token->id,
            'monto_centavos' => (int) $token->monto_snapshot_centavos,
            'expires_at'     => $token->expires_at->toIso8601String(),
            'ttl_seconds'    => QrPaymentService::TTL_SECONDS,
        ], 'QR generado');
    }

    /**
     * POST /api/v1/qr/pay — conductor/admin escanea y cobra (role:admin,driver).
     * Body: { qr }. Errores expired/replay/tampered/saldo → 422 con causa clara.
     */
    public function pay(Request $request): JsonResponse
    {
        $data = $request->validate([
            'qr' => 'required|string',
        ]);

        try {
            [$token, $asiento] = $this->qrService->pay($data['qr'], $request->user());
        } catch (InsufficientFundsException $e) {
            return $this->error('Saldo insuficiente del pasajero para este viaje.', 422, [
                'saldo_centavos'    => $e->saldoCentavos,
                'requiere_centavos' => $e->montoCentavos,
            ]);
        } catch (DomainException $e) {
            return $this->error($e->getMessage(), 422);
        }

        return $this->success([
            'token_id'       => $token->id,
            'monto_centavos' => (int) $token->monto_snapshot_centavos,
            'contraparte'    => $asiento->contraparte,
            'reference'      => $asiento->reference,
            'saldo_restante' => (int) $token->wallet->fresh()->balance_centavos,
            // El pasajero suele ser dato compartido (transportadora_id NULL):
            // se resuelve SIN el GlobalScope de tenant, que en esta ruta está
            // puesto por tenant.scope con el tenant del CONDUCTOR.
            'pasajero'       => User::withoutGlobalScope(GlobalTenantScope::class)
                ->find($token->user_id)?->name,
        ], 'Abordaje cobrado');
    }
}
