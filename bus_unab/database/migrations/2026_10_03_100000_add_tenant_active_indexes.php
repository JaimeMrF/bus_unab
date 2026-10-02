<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Rendimiento: el GlobalTenantScope + scopeActive() filtran buses y stops por
 * (transportadora_id, is_active) en cada listado de la API.
 */
return new class extends Migration
{
    public function up(): void
    {
        foreach (['buses', 'stops'] as $table) {
            Schema::table($table, function (Blueprint $t) use ($table) {
                $t->index(['transportadora_id', 'is_active'], "{$table}_tenant_active_index");
            });
        }

        Schema::table('qr_payment_tokens', function (Blueprint $t) {
            $t->index(['transportadora_id', 'used_at'], 'qr_tokens_tenant_used_index');
        });
    }

    public function down(): void
    {
        foreach (['buses', 'stops'] as $table) {
            Schema::table($table, fn (Blueprint $t) => $t->dropIndex("{$table}_tenant_active_index"));
        }
        Schema::table('qr_payment_tokens', fn (Blueprint $t) => $t->dropIndex('qr_tokens_tenant_used_index'));
    }
};
