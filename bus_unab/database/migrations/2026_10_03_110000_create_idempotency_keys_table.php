<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Idempotencia de cobros: (conductor, scope, key) único. Guarda el resultado
 * original para reenviarlo ante reintentos tras un corte de red.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('idempotency_keys', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('user_id');
            $table->string('scope', 40);
            $table->string('key', 190);
            $table->char('request_hash', 64);
            $table->unsignedSmallInteger('status_code');
            $table->json('response');
            $table->timestamp('expires_at');
            $table->timestamps();

            $table->unique(['user_id', 'scope', 'key']);
            $table->index('expires_at');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('idempotency_keys');
    }
};
