<?php

namespace App\Exceptions;

use RuntimeException;

/**
 * Saldo insuficiente para un débito de wallet (M3 · S3.2.2).
 * El servicio SIEMPRE valida antes de escribir el ledger: nunca deja
 * balance_centavos < 0 (CHECK de la BD es segunda línea, §4 DISEÑO_DB).
 */
class InsufficientFundsException extends RuntimeException
{
    public function __construct(
        public readonly int $saldoCentavos,
        public readonly int $montoCentavos,
    ) {
        parent::__construct(sprintf(
            'Saldo insuficiente: tiene %d centavos, se requieren %d.',
            $saldoCentavos,
            $montoCentavos
        ));
    }
}
