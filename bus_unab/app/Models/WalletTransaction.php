<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use RuntimeException;

/**
 * Asiento del LEDGER (M3 · S3.2.1 — DISEÑO_DB.md §5): APPEND-ONLY.
 *
 * Correcciones se hacen con asientos inversos (tipo 'ajuste'), nunca
 * UPDATE/DELETE. El modelo lo garantiza a nivel de aplicación: los eventos
 * updating/deleting lanzan excepción (defensa en profundidad; la BD no
 * puede forzarlo sin triggers).
 */
class WalletTransaction extends Model
{
    use HasFactory;

    public const TIPO_CREDITO = 'credit';

    public const TIPO_DEBITO = 'debit';

    public const TIPO_AJUSTE = 'ajuste';

    protected $fillable = [
        'wallet_id',
        'tipo',
        'monto_centavos',
        'balance_after',
        'reference',
        'contraparte',
        'metadata',
    ];

    protected function casts(): array
    {
        return [
            'monto_centavos'  => 'integer',
            'balance_after'   => 'integer',
            'metadata'        => 'array',
        ];
    }

    /** Inmutabilidad: prohibido mutar o borrar asientos ya escritos. */
    protected static function booted(): void
    {
        static::updating(function () {
            throw new RuntimeException('El ledger es inmutable: no se permiten UPDATE sobre wallet_transactions.');
        });

        static::deleting(function () {
            throw new RuntimeException('El ledger es inmutable: no se permiten DELETE sobre wallet_transactions.');
        });
    }

    public function wallet(): BelongsTo
    {
        return $this->belongsTo(Wallet::class);
    }
}
