<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * Wallet prepago del pasajero (M3 · S3.2.1 — DISEÑO_DB.md §4).
 *
 * `balance_centavos` es una PROYECCIÓN CACHEADA del ledger: solo se escribe
 * dentro de la misma transacción DB que inserta el asiento correspondiente
 * (ver WalletService). Nunca actualizar el saldo a mano.
 */
class Wallet extends Model
{
    use HasFactory;

    public const ESTADO_ACTIVA = 'activa';

    public const ESTADO_CONGELADA = 'congelada';

    protected $fillable = [
        'user_id',
        'transportadora_id',
        'balance_centavos',
        'version',
        'estado',
    ];

    protected function casts(): array
    {
        return [
            'balance_centavos' => 'integer',
            'version'          => 'integer',
            'transportadora_id' => 'integer',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    /** Ledger append-only de esta wallet. */
    public function transactions(): HasMany
    {
        return $this->hasMany(WalletTransaction::class);
    }

    public function estaActiva(): bool
    {
        return $this->estado === self::ESTADO_ACTIVA;
    }

    /** Devuelve (o crea) la wallet del usuario: 1 wallet por usuario. */
    public static function para(User $user): self
    {
        return self::firstOrCreate(
            ['user_id' => $user->id],
            ['balance_centavos' => 0, 'version' => 0, 'estado' => self::ESTADO_ACTIVA]
        );
    }
}
