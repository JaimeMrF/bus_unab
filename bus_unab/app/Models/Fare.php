<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * Tarifa por transportadora (M3 · S3.2.3 — DISEÑO_DB.md §6).
 * NULL en transportadora_id/ruta_id = tarifa general sin restringir.
 */
class Fare extends Model
{
    use HasFactory;

    protected $fillable = [
        'transportadora_id',
        'ruta_id',
        'codigo',
        'nombre',
        'monto_centavos',
        'activa',
        'vigente_desde',
        'vigente_hasta',
    ];

    protected function casts(): array
    {
        return [
            'monto_centavos' => 'integer',
            'activa'         => 'boolean',
            'vigente_desde'  => 'datetime',
            'vigente_hasta'  => 'datetime',
        ];
    }

    /**
     *dueña de la tarifa. La clase Transportadora la crea la otra onda (T3.1);
     * se referencia por nombre para no acoplar el compile-time.
     */
    public function transportadora(): BelongsTo
    {
        return $this->belongsTo(Transportadora::class, 'transportadora_id');
    }

    /** Scope: tarifa activa y dentro de su ventana de vigencia. */
    public function scopeVigente(Builder $q, ?\DateTimeInterface $en = null): Builder
    {
        $en ??= now();

        return $q->where('activa', true)
            ->where(fn (Builder $w) => $w->whereNull('vigente_desde')->orWhere('vigente_desde', '<=', $en))
            ->where(fn (Builder $w) => $w->whereNull('vigente_hasta')->orWhere('vigente_hasta', '>', $en));
    }
}
