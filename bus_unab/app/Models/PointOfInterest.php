<?php

namespace App\Models;

use App\Models\Concerns\BelongsToTenant;
use Illuminate\Database\Eloquent\Model;

class PointOfInterest extends Model
{
    use BelongsToTenant;

    protected $table = 'points_of_interest';

    protected $fillable = [
        'name',
        'description',
        'latitude',
        'longitude',
        'category',
        'icon',
        'is_active',
        'transportadora_id',
    ];

    protected function casts(): array
    {
        return [
            'latitude'  => 'float',
            'longitude' => 'float',
            'is_active' => 'boolean',
        ];
    }

    public function scopeActive($query)
    {
        return $query->where('is_active', true);
    }

    public function scopeByCategory($query, string $category)
    {
        return $query->where('category', $category);
    }
}
