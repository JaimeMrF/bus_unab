<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * White-label — perfil de branding por tenant.
 *
 * `branding` (JSON) guarda textos, paleta clara/oscura, fuente, radio y
 * features; las imágenes viven en columnas *_path (disco public). `logo_path`
 * ya existe desde create_transportadoras_table. `branding_version` se
 * incrementa en cada cambio visual para que el cliente cachee por versión.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('transportadoras', function (Blueprint $table) {
            $table->json('branding')->nullable()->after('logo_path');
            $table->string('logo_dark_path')->nullable()->after('logo_path');
            $table->string('icon_path')->nullable()->after('logo_dark_path');
            $table->string('mascot_path')->nullable()->after('icon_path');
            $table->unsignedInteger('branding_version')->default(1)->after('branding');
        });
    }

    public function down(): void
    {
        Schema::table('transportadoras', function (Blueprint $table) {
            $table->dropColumn(['branding', 'logo_dark_path', 'icon_path', 'mascot_path', 'branding_version']);
        });
    }
};
