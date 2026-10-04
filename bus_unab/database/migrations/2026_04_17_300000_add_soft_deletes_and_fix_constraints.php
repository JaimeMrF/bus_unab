<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Soft deletes en buses
        Schema::table('buses', function (Blueprint $table) {
            if (! Schema::hasColumn('buses', 'deleted_at')) {
                $table->softDeletes()->after('is_active');
            }
        });

        // Soft deletes en stops
        Schema::table('stops', function (Blueprint $table) {
            if (! Schema::hasColumn('stops', 'deleted_at')) {
                $table->softDeletes()->after('is_active');
            }
        });

        // Corregir el unique constraint en bus_requests:
        // El constraint original unique(user_id, bus_id, status) es incorrecto porque
        // un usuario puede tener múltiples registros con status=cancelled/expired/boarded.
        // La restricción correcta es: solo 1 solicitud PENDING por usuario por bus.
        $uniqueName = 'bus_requests_user_id_bus_id_status_unique';
        $plainName = 'bus_requests_user_id_bus_id_status_index';
        $driver = DB::getDriverName();

        if ($driver === 'mysql') {
            $hasUnique = collect(DB::select('SHOW INDEX FROM bus_requests WHERE Key_name = ?', [$uniqueName]))->isNotEmpty();
            $hasPlain = collect(DB::select('SHOW INDEX FROM bus_requests WHERE Key_name = ?', [$plainName]))->isNotEmpty();
        } elseif ($driver === 'sqlite') {
            // SHOW INDEX no existe en SQLite: los índices (incl. unique) viven en sqlite_master.
            $hasUnique = collect(DB::select(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'bus_requests' AND name = ?",
                [$uniqueName]
            ))->isNotEmpty();
            $hasPlain = collect(DB::select(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'bus_requests' AND name = ?",
                [$plainName]
            ))->isNotEmpty();
        } else {
            // Otros drivers: no tocar nada.
            $hasUnique = false;
            $hasPlain = true;
        }

        // Orden seguro en MySQL/MariaDB: primero el índice plano (user_id es su prefijo
        // izquierdo, así que cubre la FK de user_id) y solo entonces se borra el único.
        // Con el orden inverso (índice temporal + drop) MySQL se niega a borrar el
        // temporal porque la FK ya lo adoptó ("needed in a foreign key constraint").
        if (! $hasPlain) {
            Schema::table('bus_requests', function (Blueprint $table) {
                $table->index(['user_id', 'bus_id', 'status']);
            });
        }

        if ($hasUnique) {
            Schema::table('bus_requests', function (Blueprint $table) use ($uniqueName) {
                $table->dropUnique($uniqueName);
            });
        }

        // Restos de un intento anterior fallido de esta migración (índice temporal).
        if ($driver === 'mysql'
            && collect(DB::select('SHOW INDEX FROM bus_requests WHERE Key_name = ?', ['bus_requests_user_id_fk_cover']))->isNotEmpty()) {
            Schema::table('bus_requests', function (Blueprint $table) {
                $table->dropIndex('bus_requests_user_id_fk_cover');
            });
        }
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
