<?php

namespace App\Services;

use App\Models\IdempotencyKey;
use App\Models\User;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Facades\DB;

/**
 * Ejecuta una acción una sola vez por (conductor, scope, Idempotency-Key).
 *
 * - Misma clave + mismo payload: devuelve el resultado ORIGINAL sin reejecutar.
 * - Misma clave + otro payload: conflicto (IdempotencyConflict).
 * - La clave se guarda en la MISMA transacción que la acción; solo se guardan
 *   resultados 2xx (un error, p. ej. saldo insuficiente, permite reintentar).
 * - Otro conductor con la misma clave no ve el resultado: tiene su propia fila.
 */
class IdempotencyService
{
    public const TTL_HOURS = 24;

    public const KEY_PATTERN = '/^[A-Za-z0-9._:\-]{1,190}$/';

    /**
     * @param  callable(): array{0:int,1:array}  $action  devuelve [status, body]
     * @return array{0:int,1:array,2:bool} [status, body, replayed]
     *
     * @throws IdempotencyConflict
     */
    public function run(User $driver, string $scope, ?string $key, string $fingerprint, callable $action): array
    {
        if ($key === null) {
            return [...$action(), false];
        }

        if ($stored = $this->find($driver, $scope, $key, $fingerprint)) {
            return $stored;
        }

        try {
            [$status, $body] = DB::transaction(function () use ($driver, $scope, $key, $fingerprint, $action) {
                [$status, $body] = $action();

                if ($status >= 200 && $status < 300) {
                    IdempotencyKey::create([
                        'user_id' => $driver->id,
                        'scope' => $scope,
                        'key' => $key,
                        'request_hash' => $fingerprint,
                        'status_code' => $status,
                        'response' => $body,
                        'expires_at' => now()->addHours(self::TTL_HOURS),
                    ]);
                }

                return [$status, $body];
            });
        } catch (UniqueConstraintViolationException) {
            // Carrera: otra petición con la misma clave confirmó primero.
            return $this->find($driver, $scope, $key, $fingerprint) ?? throw new IdempotencyConflict('Solicitud en curso con la misma Idempotency-Key.');
        }

        // Carrera sin violación de unique (el reintento perdió el reclamo y falló):
        // si la petición hermana ya confirmó, se devuelve su resultado original.
        if ($status >= 400 && ($stored = $this->find($driver, $scope, $key, $fingerprint))) {
            return $stored;
        }

        return [$status, $body, false];
    }

    /** @return array{0:int,1:array,2:bool}|null */
    private function find(User $driver, string $scope, string $key, string $fingerprint): ?array
    {
        $row = IdempotencyKey::where(['user_id' => $driver->id, 'scope' => $scope, 'key' => $key])->first();

        if (! $row) {
            return null;
        }

        if ($row->expires_at->isPast()) {
            $row->delete();

            return null;
        }

        if (! hash_equals($row->request_hash, $fingerprint)) {
            throw new IdempotencyConflict('La Idempotency-Key ya se usó con otro contenido.');
        }

        return [$row->status_code, $row->response, true];
    }
}
