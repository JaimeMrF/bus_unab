<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class BusRouteWaypoint extends Model
{
    protected $fillable = ['bus_id', 'order', 'latitude', 'longitude', 'label'];

    protected function casts(): array
    {
        return [
            'latitude'  => 'float',
            'longitude' => 'float',
            'order'     => 'integer',
        ];
    }

    public function bus()
    {
        return $this->belongsTo(Bus::class);
    }
}
