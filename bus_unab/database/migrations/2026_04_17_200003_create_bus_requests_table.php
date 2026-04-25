<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('bus_requests', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('bus_id')->constrained()->cascadeOnDelete();
            $table->foreignId('stop_id')->constrained()->cascadeOnDelete();
            $table->enum('status', [
                'pending',    // usuario esperando el bus
                'boarded',    // bus llegó a la parada, usuario subió
                'cancelled',  // usuario canceló
                'expired',    // el bus pasó y el usuario no abordó
            ])->default('pending');
            $table->timestamp('boarded_at')->nullable();   // cuándo se marcó como abordado
            $table->timestamps();

            // Un usuario solo puede tener una solicitud activa por bus
            $table->unique(['user_id', 'bus_id', 'status']);
            $table->index(['bus_id', 'stop_id', 'status']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('bus_requests');
    }
};
