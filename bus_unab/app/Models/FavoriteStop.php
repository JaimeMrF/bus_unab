<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class FavoriteStop extends Model
{
    protected $fillable = ['user_id', 'stop_id'];

    public function stop(): BelongsTo
    {
        return $this->belongsTo(Stop::class);
    }
}
