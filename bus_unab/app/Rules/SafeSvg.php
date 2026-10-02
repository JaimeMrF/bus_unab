<?php

namespace App\Rules;

use Closure;
use Illuminate\Contracts\Validation\ValidationRule;
use Illuminate\Http\UploadedFile;

/**
 * Rechaza SVG con contenido activo (script, handlers on*, javascript:,
 * foreignObject, entidades, referencias externas). Los archivos que no son
 * SVG pasan sin tocar (los valida la regla de mimes).
 */
class SafeSvg implements ValidationRule
{
    private const FORBIDDEN = [
        '/<\s*script/i',
        '/<\s*foreignObject/i',
        '/<\s*iframe/i',
        '/<\s*embed/i',
        '/<\s*object/i',
        '/<!ENTITY/i',
        '/<!DOCTYPE/i',
        '/\son[a-z]+\s*=/i',
        '/javascript\s*:/i',
        '/data\s*:\s*text\/html/i',
        '/(?:xlink:)?href\s*=\s*["\']\s*(?!#)[^"\']/i',
    ];

    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        if (! $value instanceof UploadedFile) {
            return;
        }

        $head = (string) @file_get_contents($value->getRealPath(), false, null, 0, 512);
        if (! preg_match('/<svg[\s>]/i', $head) && ! str_contains($head, '<?xml')) {
            return; // no es SVG
        }

        $content = (string) @file_get_contents($value->getRealPath());
        foreach (self::FORBIDDEN as $pattern) {
            if (preg_match($pattern, $content)) {
                $fail('El SVG contiene contenido no permitido (scripts, eventos o enlaces externos).');

                return;
            }
        }
    }
}
