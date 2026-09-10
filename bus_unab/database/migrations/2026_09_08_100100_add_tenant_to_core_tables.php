<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * M3 · S3.1.2 — Columna tenant `transportadora_id` en tablas núcleo
 * (DISEÑO_DB.md §3).
 *
 * Tablas: buses, stops, users, points_of_interest (lista de todo.md S3.1.2)
 * + las rutas-related que REALMENTE existen verificadas en migraciones:
 *   route_stops (pivot bus_id/stop_id — no tiene route_id, verificada),
 *   bus_route_waypoints (referencia bus), y bus_requests (hereda del bus).
 *
 * Siempre NULLABLE + índice: stops/POI son datos compartidos de ciudad hoy;
 * NOT NULL solo tras backfill verificado (fase siguiente, DISEÑO_DB §3).
 * Compatible MySQL (prod) y SQLite (dev/tests): SQLite acepta ADD COLUMN con
 * REFERENCES (default NULL), así que la FK se crea en ambos motores.
 */
return new class extends Migration
{
    /** @var array<int, string> */
    private const TABLES = [
        'buses',
        'stops',
        'users',
        'points_of_interest',
        'route_stops',
        'bus_route_waypoints',
        'bus_requests',
    ];

    public function up(): void
    {
        foreach (self::TABLES as $tableName) {
            if (! Schema::hasTable($tableName)) {
                continue;
            }

            Schema::table($tableName, function (Blueprint $table) use ($tableName) {
                if (Schema::hasColumn($tableName, 'transportadora_id')) {
                    return;
                }

                $table->unsignedBigInteger('transportadora_id')->nullable();
                $table->index('transportadora_id', "{$tableName}_transportadora_id_index");
            });

            // FK separada para poder nombrarla y poder bajarla en down() en ambos motores.
            Schema::table($tableName, function (Blueprint $table) use ($tableName) {
                $table->foreign('transportadora_id', "{$tableName}_transportadora_id_fk")
                    ->references('id')
                    ->on('transportadoras')
                    ->nullOnDelete();
            });
        }
    }

    public function down(): void
    {
        foreach (array_reverse(self::TABLES) as $tableName) {
            if (! Schema::hasTable($tableName) || ! Schema::hasColumn($tableName, 'transportadora_id')) {
                continue;
            }

            if (DB::getDriverName() === 'sqlite') {
                // SQLite no soporta DROP CONSTRAINT; Laravel 12 reconstruye la tabla
                // al soltar el índice/auto-FK: basta con drop index + drop column.
                Schema::table($tableName, function (Blueprint $table) use ($tableName) {
                    $table->dropIndex("{$tableName}_transportadora_id_index");
                });
                Schema::table($tableName, function (Blueprint $table) {
                    $table->dropColumn('transportadora_id');
                });

                return;
            }

            Schema::table($tableName, function (Blueprint $table) use ($tableName) {
                $table->dropForeign("{$tableName}_transportadora_id_fk");
                $table->dropIndex("{$tableName}_transportadora_id_index");
                $table->dropColumn('transportadora_id');
            });
        }
    }
};
