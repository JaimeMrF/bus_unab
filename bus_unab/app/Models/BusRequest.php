<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class BusRequest extends Model
{
    protected $fillable = [
        'user_id',
        'bus_id',
        'stop_id',
        'status',
        'boarded_at',
    ];

    protected function casts(): array
    {
        return [
            'boarded_at' => 'datetime',
        ];
    }

    public function user()
    {
        return $this->belongsTo(User::class);
    }

    public function bus()
    {
        return $this->belongsTo(Bus::class);
    }

    public function stop()
    {
        return $this->belongsTo(Stop::class);
    }

    public function scopePending($query)
    {
        return $query->where('status', 'pending');
    }
}
