<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class RouteStop extends Model
{
    protected $fillable = [
        'bus_id',
        'stop_id',
        'order',
        'estimated_minutes',
    ];

    protected function casts(): array
    {
        return [
            'order'              => 'integer',
            'estimated_minutes'  => 'integer',
        ];
    }

    public function bus()
    {
        return $this->belongsTo(Bus::class);
    }

    public function stop()
    {
        return $this->belongsTo(Stop::class);
    }
}
