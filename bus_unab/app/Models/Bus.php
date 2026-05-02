<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Bus extends Model
{
    use SoftDeletes;

    protected $fillable = [
        'name',
        'plate',
        'capacity',
        'external_vehicle_id',
        'is_active',
    ];

    protected function casts(): array
    {
        return [
            'is_active'           => 'boolean',
            'external_vehicle_id' => 'integer',
            'capacity'            => 'integer',
        ];
    }

    public function scopeActive($query)
    {
        return $query->where('is_active', true);
    }

    public function stops()
    {
        return $this->belongsToMany(Stop::class, 'route_stops')
            ->withPivot(['order', 'estimated_minutes'])
            ->orderByPivot('order');
    }

    public function routeStops()
    {
        return $this->hasMany(RouteStop::class)->orderBy('order');
    }

    public function routeWaypoints()
    {
        return $this->hasMany(BusRouteWaypoint::class)->orderBy('order');
    }

    public function requests()
    {
        return $this->hasMany(BusRequest::class);
    }

    /**
     * Cantidad de usuarios esperando actualmente este bus (aforo estimado).
     */
    public function pendingRequestsCount(): int
    {
        return $this->requests()->where('status', 'pending')->count();
    }

    /**
     * Porcentaje de ocupación estimado basado en solicitudes pendientes.
     */
    public function occupancyPercentage(): float
    {
        if ($this->capacity === 0) return 0.0;
        return round(min(($this->pendingRequestsCount() / $this->capacity) * 100, 100), 1);
    }

    /**
     * Nivel de ocupación: low / medium / high / full
     */
    public function occupancyLevel(): string
    {
        $pct = $this->occupancyPercentage();
        return match(true) {
            $pct >= 90 => 'full',
            $pct >= 60 => 'high',
            $pct >= 30 => 'medium',
            default    => 'low',
        };
    }
}
