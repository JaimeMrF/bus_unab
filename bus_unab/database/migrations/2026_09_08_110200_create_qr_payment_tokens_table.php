<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * M3 · S3.2.4 — Tokens de pago QR efímeros (DISEÑO_DB.md §7, adaptado).
 *
 * El QR que ve el pasajero es "selector.firma": el selector (16 bytes hex)
 * NUNCA se almacena en claro; se guarda su SHA-256 (token_hash, 64 hex).
 * La firma HMAC-SHA256(app.key, selector|user_id|exp) se recalcula al pagar.
 *
 * `transportadora_id`/`bus_id`: BIGINT sin FK (consolidación posterior).
 * `fares`/`users`/`wallets`: FK reales — tablas propias de esta onda o base.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('qr_payment_tokens', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('wallet_id')->constrained()->cascadeOnDelete();
            $table->foreignId('fare_id')->nullable()->constrained('fares')->nullOnDelete();
            $table->unsignedBigInteger('transportadora_id')->nullable()->index();
            $table->unsignedBigInteger('bus_id')->nullable()->index(); // snapshot del cobro
            // SHA-256 del selector: 64 hex, unique. Nunca token en claro.
            $table->char('token_hash', 64)->unique();
            // Monto congelado al emitir (la tarifa puede cambiar después).
            $table->unsignedBigInteger('monto_snapshot_centavos');
            $table->timestamp('issued_at')->useCurrent();
            $table->timestamp('expires_at')->index();      // issued_at + 60 s
            $table->timestamp('used_at')->nullable()->index(); // ONE-TIME: primera marca gana
            $table->foreignId('used_by_driver_id')->nullable()->constrained('users')->nullOnDelete();
            $table->string('reference', 190)->nullable();  // 'qrpay:{token_id}' del asiento cobrado
            $table->timestamps();

            $table->index(['user_id', 'issued_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('qr_payment_tokens');
    }
};
