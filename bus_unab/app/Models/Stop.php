<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Stop extends Model
{
    use SoftDeletes;

    protected $fillable = [
        'name',
        'address',
        'latitude',
        'longitude',
        'radius_meters',
        'is_active',
    ];

    protected function casts(): array
    {
        return [
            'latitude'      => 'float',
            'longitude'     => 'float',
            'radius_meters' => 'integer',
            'is_active'     => 'boolean',
        ];
    }

    public function buses()
    {
        return $this->belongsToMany(Bus::class, 'route_stops')
            ->withPivot(['order', 'estimated_minutes']);
    }

    public function pendingRequests()
    {
        return $this->hasMany(BusRequest::class)->where('status', 'pending');
    }

    /**
     * Calcula la distancia en metros entre esta parada y unas coordenadas dadas.
     * Fórmula Haversine.
     */
    public function distanceTo(float $lat, float $lng): float
    {
        $earthRadius = 6371000; // metros

        $latFrom = deg2rad($this->latitude);
        $latTo   = deg2rad($lat);
        $dLat    = deg2rad($lat - $this->latitude);
        $dLng    = deg2rad($lng - $this->longitude);

        $a = sin($dLat / 2) ** 2
            + cos($latFrom) * cos($latTo) * sin($dLng / 2) ** 2;

        return $earthRadius * 2 * atan2(sqrt($a), sqrt(1 - $a));
    }

    /**
     * Verifica si unas coordenadas están dentro del radio de la parada.
     */
    public function isNearby(float $lat, float $lng): bool
    {
        return $this->distanceTo($lat, $lng) <= $this->radius_meters;
    }
}
