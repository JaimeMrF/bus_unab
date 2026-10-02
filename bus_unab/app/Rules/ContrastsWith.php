<?php

namespace App\Rules;

use App\Support\ColorContrast;
use Closure;
use Illuminate\Contracts\Validation\ValidationRule;

/**
 * El color validado debe tener contraste WCAG >= $min contra $against.
 */
class ContrastsWith implements ValidationRule
{
    public function __construct(
        private readonly ?string $against,
        private readonly float $min = 4.5,
    ) {}

    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if (! ColorContrast::isHex($value) || ! ColorContrast::isHex($this->against)) {
            return; // HexColor reporta el formato
        }

        $ratio = ColorContrast::ratio($value, $this->against);
        if ($ratio < $this->min) {
            $fail(sprintf('El contraste con %s es %.2f:1; se requiere al menos %.1f:1.', $this->against, $ratio, $this->min));
        }
    }
}
