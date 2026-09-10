<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * Transportadora = TENANT raíz del SaaS "Bucaramanga Mobility" (M3 · S3.1.1).
 *
 * Dueña de su flota (buses), sus paradas asignadas (stops), su operación de
 * rutas (route_stops/waypoints vía buses) y su equipo (users con rol
 * admin/driver). Los pasajeros pueden quedar con transportadora_id NULL
 * (pasajero genérico de ciudad) y los Super Admin también NULL
 * (ver DISEÑO_DB.md §3).
 */
class Transportadora extends Model
{
    /** @use HasFactory<\Database\Factories\TransportadoraFactory> */
    use HasFactory, SoftDeletes;

    protected $fillable = [
        'nombre',
        'slug',
        'razon_social',
        'nit',
        'logo_path',
        'contacto_nombre',
        'contacto_email',
        'contacto_telefono',
        'plan',
        'activo',
    ];

    /**
     * Defaults de instancia (TACHE-CAC5-3): el default de BD no se refleja en
     * el modelo recién creado sin refresh(), así que viven también aquí.
     */
    protected $attributes = [
        'plan' => 'basico',
        'activo' => true,
    ];

    protected function casts(): array
    {
        return [
            'activo' => 'boolean',
        ];
    }

    public function buses(): HasMany
    {
        return $this->hasMany(Bus::class);
    }

    public function stops(): HasMany
    {
        return $this->hasMany(Stop::class);
    }

    public function users(): HasMany
    {
        return $this->hasMany(User::class);
    }
}
