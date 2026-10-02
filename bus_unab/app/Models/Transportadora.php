<?php

namespace App\Models;

use App\Services\BrandingService;
use Database\Factories\TransportadoraFactory;
use Illuminate\Database\Eloquent\Casts\Attribute;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * Transportadora = TENANT raíz del SaaS "BUCARATRANSIT" (M3 · S3.1.1).
 *
 * Dueña de su flota (buses), sus paradas asignadas (stops), su operación de
 * rutas (route_stops/waypoints vía buses) y su equipo (users con rol
 * admin/driver). Los pasajeros pueden quedar con transportadora_id NULL
 * (pasajero genérico de ciudad) y los Super Admin también NULL
 * (ver DISEÑO_DB.md §3).
 */
class Transportadora extends Model
{
    /** @use HasFactory<TransportadoraFactory> */
    use HasFactory, SoftDeletes;

    protected $fillable = [
        'nombre',
        'slug',
        'razon_social',
        'nit',
        'logo_path',
        'logo_dark_path',
        'icon_path',
        'mascot_path',
        'branding',
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
        'branding_version' => 1,
        'activo' => true,
    ];

    protected function casts(): array
    {
        return [
            'activo' => 'boolean',
            'branding' => 'array',
            'branding_version' => 'integer',
        ];
    }

    /** Cada cambio visual incrementa la versión que el cliente usa para cachear. */
    protected static function booted(): void
    {
        static::updating(function (self $t): void {
            if ($t->isDirty(['branding', 'logo_path', 'logo_dark_path', 'icon_path', 'mascot_path'])) {
                $t->branding_version = ((int) ($t->getOriginal('branding_version') ?? 1)) + 1;
            }
        });

        // Invalida el payload cacheado (slug actual y, si cambió, el anterior).
        $forget = function (self $t): void {
            BrandingService::forget($t->slug);
            BrandingService::forget($t->getOriginal('slug'));
        };
        static::saved($forget);
        static::deleted($forget);
        static::restored($forget);
    }

    /** Slug siempre en minúsculas: búsqueda consistente entre MySQL y SQLite. */
    protected function slug(): Attribute
    {
        return Attribute::set(fn (?string $value) => $value === null ? null : strtolower(trim($value)));
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
