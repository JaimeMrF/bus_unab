<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * Token de pago QR efímero y de un solo uso (M3 · S3.2.4 — DISEÑO_DB.md §7).
 *
 * El QR visible para el pasajero es "selector.firmaHMAC". Aquí se guarda
 * SOLO sha256(selector) (token_hash): el selector en claro jamás se persiste.
 */
class QrPaymentToken extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id',
        'wallet_id',
        'fare_id',
        'transportadora_id',
        'bus_id',
        'token_hash',
        'monto_snapshot_centavos',
        'issued_at',
        'expires_at',
        'used_at',
        'used_by_driver_id',
        'reference',
    ];

    protected function casts(): array
    {
        return [
            'monto_snapshot_centavos' => 'integer',
            'issued_at'               => 'datetime',
            'expires_at'              => 'datetime',
            'used_at'                 => 'datetime',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function wallet(): BelongsTo
    {
        return $this->belongsTo(Wallet::class);
    }

    public function fare(): BelongsTo
    {
        return $this->belongsTo(Fare::class);
    }

    /** Conductor/scanner que liquidó el token. */
    public function usedByDriver(): BelongsTo
    {
        return $this->belongsTo(User::class, 'used_by_driver_id');
    }

    public function estaExpirado(): bool
    {
        return $this->expires_at->isPast();
    }

    public function estaUsado(): bool
    {
        return $this->used_at !== null;
    }
}
