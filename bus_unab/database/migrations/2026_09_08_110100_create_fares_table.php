<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * M3 · S3.2.3 — Tarifas por transportadora (DISEÑO_DB.md §6).
 *
 * `transportadora_id` y `ruta_id` quedan como BIGINT + índice SIN FK:
 *  - transportadoras: la crea otra onda (orden de migración no garantizado en paralelo).
 *  - ruta: hoy no existe tabla `routes` (las rutas viven en route_stops/bus);
 *    se reservará el vínculo al consolidar.
 * FKs reales → migración de consolidación posterior.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('fares', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('transportadora_id')->nullable()->index();
            $table->unsignedBigInteger('ruta_id')->nullable()->index();
            $table->string('codigo', 30)->nullable();   // 'ORDINARIO', 'ESTUDIANTIL', ...
            $table->string('nombre', 80)->nullable();
            // CENTAVOS, siempre positivo (validado también en el modelo).
            $table->unsignedBigInteger('monto_centavos');
            $table->boolean('activa')->default(true);
            $table->timestamp('vigente_desde')->nullable();
            $table->timestamp('vigente_hasta')->nullable(); // NULL = sin fin
            $table->timestamps();

            // Índice caliente: resolver tarifa vigente del tenant.
            $table->index(['transportadora_id', 'activa', 'vigente_desde', 'vigente_hasta']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('fares');
    }
};
