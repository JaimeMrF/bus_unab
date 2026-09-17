<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * M3 · S3.1.1 — Tabla raíz del tenant: `transportadoras` (DISEÑO_DB.md §2).
 *
 * Cada fila es una transportadora de Bucaramanga operando como inquilino
 * (tenant) del SaaS "BUCARATRANSIT". `slug` alimenta el panel
 * /empresa/{slug}; `plan` es solo un campo de trabajo (la suscripción real
 * vivirá en saas_subscriptions, fase posterior — PENDIENTE, no bloquea).
 *
 * Compatible MySQL (producción, app.yaml) y SQLite (desarrollo/tests):
 * solo tipos estándar, sin enums ni generated columns.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('transportadoras', function (Blueprint $table) {
            $table->id();
            $table->string('nombre', 120);                      // nombre comercial visible
            $table->string('slug', 60)->unique();               // url/panel del tenant
            $table->string('razon_social', 160)->nullable();    // datos legales
            $table->string('nit', 20)->nullable()->unique();    // UNIQUE NULLABLE (varios NULL ok)
            $table->string('logo_path')->nullable();
            $table->string('contacto_nombre', 120)->nullable(); // "contacto nullable" del plan
            $table->string('contacto_email', 190)->nullable();
            $table->string('contacto_telefono', 30)->nullable();
            $table->string('plan', 20)->default('basico');      // trial|basico|pro|enterprise
            $table->boolean('activo')->default(true);           // kill-switch SaaS
            $table->timestamps();
            $table->softDeletes();

            $table->index('activo');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('transportadoras');
    }
};
