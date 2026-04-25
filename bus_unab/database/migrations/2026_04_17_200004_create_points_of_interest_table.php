<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('points_of_interest', function (Blueprint $table) {
            $table->id();
            $table->string('name', 150);
            $table->text('description')->nullable();
            $table->decimal('latitude', 10, 7);
            $table->decimal('longitude', 10, 7);
            $table->enum('category', [
                'campus',       // edificios UNAB
                'parking',      // parqueaderos
                'food',         // cafeterías y restaurantes
                'health',       // servicios médicos
                'transport',    // puntos de transporte
                'other',
            ])->default('other');
            $table->string('icon', 50)->nullable();   // nombre del ícono en la app
            $table->boolean('is_active')->default(true);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('points_of_interest');
    }
};
