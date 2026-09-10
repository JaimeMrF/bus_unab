<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * M3 · S3.2.1 — Wallets + ledger append-only (DISEÑO_DB.md §4–§5).
 *
 * Nota de orden: `transportadora_id` se crea como BIGINT simple + índice,
 * SIN FK, para que esta migración sea independiente del orden de ejecución
 * respecto de `create_transportadoras_table` (la tabla la crea otra onda).
 * La FK real se añade en la migración de consolidación posterior.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('wallets', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->unique()->constrained()->cascadeOnDelete();
            // Tenant patrocinante (NULL = ciudad/plataforma). FK diferida: ver nota arriba.
            $table->unsignedBigInteger('transportadora_id')->nullable()->index();
            // Saldo cacheado en centavos — siempre se escribe junto con su asiento de ledger.
            $table->bigInteger('balance_centavos')->default(0);
            // Optimistic lock (DISEÑO_DB §4): +1 por movimiento.
            $table->bigInteger('version')->default(0);
            $table->enum('estado', ['activa', 'congelada'])->default('activa');
            $table->timestamps();
        });

        Schema::create('wallet_transactions', function (Blueprint $table) {
            $table->id();
            $table->foreignId('wallet_id')->constrained()->cascadeOnDelete();
            $table->enum('tipo', ['credit', 'debit', 'ajuste']);
            // Monto estrictamente positivo (el signo lo da `tipo`); se valida en el servicio.
            $table->unsignedBigInteger('monto_centavos');
            // Saldo de la wallet DESPUÉS de este asiento (inmutable, cache verificable).
            $table->bigInteger('balance_after');
            // Idempotencia: reintentos con la misma reference no duplican efecto.
            $table->string('reference', 190)->unique();
            // Contrapartida del movimiento: 'mock', 'transportadora:{id}', 'plataforma', ...
            $table->string('contraparte')->nullable();
            $table->json('metadata')->nullable();
            $table->timestamps();

            $table->index(['wallet_id', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('wallet_transactions');
        Schema::dropIfExists('wallets');
    }
};
