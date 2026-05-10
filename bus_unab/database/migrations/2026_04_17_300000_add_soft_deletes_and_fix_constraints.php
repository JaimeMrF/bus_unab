<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Soft deletes en buses
        Schema::table('buses', function (Blueprint $table) {
            if (!Schema::hasColumn('buses', 'deleted_at')) {
                $table->softDeletes()->after('is_active');
            }
        });

        // Soft deletes en stops
        Schema::table('stops', function (Blueprint $table) {
            if (!Schema::hasColumn('stops', 'deleted_at')) {
                $table->softDeletes()->after('is_active');
            }
        });

        // Corregir el unique constraint en bus_requests:
        // El constraint original unique(user_id, bus_id, status) es incorrecto porque
        // un usuario puede tener múltiples registros con status=cancelled/expired/boarded.
        // La restricción correcta es: solo 1 solicitud PENDING por usuario por bus.
        // SQLite no soporta partial unique index nativo, lo manejamos en el service.
        Schema::table('bus_requests', function (Blueprint $table) {
            $table->dropUnique(['user_id', 'bus_id', 'status']);
            // Agregar índice compuesto para performance (sin unique)
            $table->index(['user_id', 'bus_id', 'status']);
        });
    }

    public function down(): void
    {
        Schema::table('buses', function (Blueprint $table) {
            $table->dropSoftDeletes();
        });

        Schema::table('stops', function (Blueprint $table) {
            $table->dropSoftDeletes();
        });

        Schema::table('bus_requests', function (Blueprint $table) {
            $table->dropIndex(['user_id', 'bus_id', 'status']);
            $table->unique(['user_id', 'bus_id', 'status']);
        });
    }
};
