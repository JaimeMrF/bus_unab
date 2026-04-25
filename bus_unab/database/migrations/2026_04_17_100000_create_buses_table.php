<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('buses', function (Blueprint $table) {
            $table->id();
            $table->string('name', 50);                      // "Ruta 1", "Ruta 2", "Ruta 3"
            $table->string('plate', 20);                     // "RUTA1", "RUTA2", "RUTA3"
            $table->unsignedBigInteger('external_vehicle_id'); // ID en gpsmobile.co (97141, 190024, etc.)
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->unique('plate');
            $table->unique('external_vehicle_id');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('buses');
    }
};
