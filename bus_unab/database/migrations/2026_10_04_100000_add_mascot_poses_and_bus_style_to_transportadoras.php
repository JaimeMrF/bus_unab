<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * White-label — mascota por poses (JSON {pose: path}) y estilo del bus
 * (JSON {body, accent, icon, icon_path}). Ambos opcionales.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('transportadoras', function (Blueprint $table) {
            $table->json('mascot_poses')->nullable()->after('mascot_path');
            $table->json('bus_style')->nullable()->after('mascot_poses');
        });
    }

    public function down(): void
    {
        Schema::table('transportadoras', function (Blueprint $table) {
            $table->dropColumn(['mascot_poses', 'bus_style']);
        });
    }
};
