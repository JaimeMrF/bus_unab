<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('route_stops', function (Blueprint $table) {
            $table->id();
            $table->foreignId('bus_id')->constrained()->cascadeOnDelete();
            $table->foreignId('stop_id')->constrained()->cascadeOnDelete();
            $table->unsignedSmallInteger('order');                              // orden de la parada en la ruta
            $table->unsignedSmallInteger('estimated_minutes')->nullable();      // minutos desde el inicio de la ruta
            $table->timestamps();

            $table->unique(['bus_id', 'stop_id']);
            $table->index(['bus_id', 'order']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('route_stops');
    }
};
