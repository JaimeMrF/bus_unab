<?php

namespace App\Models;

use App\Models\Concerns\BelongsToTenant;
use Database\Factories\UserFactory;
use Filament\Models\Contracts\FilamentUser;
use Filament\Panel;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable implements FilamentUser
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, Notifiable;
    use BelongsToTenant;

    protected $fillable = [
        'name',
        'email',
        'password',
        'google_id',
        'avatar',
        'role',
        'transportadora_id',
    ];

    protected $hidden = [
        'password',
        'remember_token',
    ];

    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password'          => 'hashed',
        ];
    }

    public function isAdmin(): bool
    {
        return $this->role === 'admin';
    }

    public function isDriver(): bool
    {
        return $this->role === 'driver';
    }

    /**
     * H1 · 'student' fue ELIMINADO como rol del producto (pivote): los usuarios
     * de app son 'pasajero'. Se conserva como ALIAS de lectura para no romper
     * llamadas antiguas ni datos pre-migración (ver migration 130000).
     */
    public function isStudent(): bool
    {
        return $this->isPasajero();
    }

    /** Pasajero de a pie: rol oficial 'pasajero' o legacy 'student'. */
    public function isPasajero(): bool
    {
        return in_array($this->role, ['pasajero', 'student'], true);
    }

    /**
     * H1/H2 · Único lugar del codebase que decide qué roles se pueden ASIGNAR
     * desde cada panel Filament. El Select del UserResource consume esto:
     * Filament valida contra las opciones, así que la lista ES la guard
     * (un gerente de empresa no puede crearse admin — ni por UI, ni por payload).
     *
     * @return array<string,string> value => label
     */
    public static function assignableRoles(?string $panelId): array
    {
        return match ($panelId) {
            'empresa' => [
                'driver'   => 'Conductor',
                'pasajero' => 'Pasajero',
            ],
            default => [
                'super_admin'  => 'Super Admin',
                'tenant_admin' => 'Admin de Empresa',
                'driver'       => 'Conductor',
                'pasajero'     => 'Pasajero',
                'admin'        => 'Administrador (legado)',
            ],
        };
    }

    /** Rol sugerido por defecto al crear: gerente crea conductores; admin, pasajeros. */
    public static function defaultRoleForPanel(?string $panelId): string
    {
        return $panelId === 'empresa' ? 'driver' : 'pasajero';
    }

    /** Super Admin: role histórico 'admin' ≡ nuevo 'super_admin' (S3.3.1, alias sin rompe). */
    public function isSuperAdmin(): bool
    {
        return in_array($this->role, ['admin', 'super_admin'], true);
    }

    /** Admin de una transportadora (panel /empresa). Requiere transportadora_id no nulo. */
    public function isTenantAdmin(): bool
    {
        return $this->role === 'tenant_admin' && $this->transportadora_id !== null;
    }

    /**
     * Acceso por panel (M3 · S3.3.1):
     *  - 'admin'   → Super Admins (compat con role 'admin' histórico).
     *  - 'empresa' → tenant_admins con transportadora asignada.
     * isAdmin()/isDriver()/isStudent() NO cambian: dependen de ellas tests y API.
     */
    public function canAccessPanel(Panel $panel): bool
    {
        return match ($panel->getId()) {
            'admin'   => $this->isSuperAdmin(),
            'empresa' => $this->isTenantAdmin(),
            default   => false,
        };
    }

    public function deviceTokens()
    {
        return $this->hasMany(DeviceToken::class);
    }

    public function busRequests()
    {
        return $this->hasMany(BusRequest::class);
    }
}
