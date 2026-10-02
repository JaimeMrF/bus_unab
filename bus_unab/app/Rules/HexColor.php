<?php

namespace App\Rules;

use App\Support\ColorContrast;
use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

class HexColor implements ValidationRule
{
    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if (! ColorContrast::isHex($value)) {
            $fail('El color debe tener formato hexadecimal #RRGGBB.');
        }
    }
}
