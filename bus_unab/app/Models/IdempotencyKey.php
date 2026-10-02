<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Prunable;

class IdempotencyKey extends Model
{
    use Prunable;

    protected $fillable = ['user_id', 'scope', 'key', 'request_hash', 'status_code', 'response', 'expires_at'];

    protected function casts(): array
    {
        return [
            'response' => 'array',
            'status_code' => 'integer',
            'expires_at' => 'datetime',
        ];
    }

    public function prunable(): Builder
    {
        return static::where('expires_at', '<', now());
    }
}
