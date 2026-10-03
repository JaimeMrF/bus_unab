<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/** Un QR nuevo revoca los anteriores sin usar del mismo usuario (anti capturas). */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('qr_payment_tokens', function (Blueprint $table) {
            $table->timestamp('revoked_at')->nullable()->after('used_at');
        });
    }

    public function down(): void
    {
        Schema::table('qr_payment_tokens', function (Blueprint $table) {
            $table->dropColumn('revoked_at');
        });
    }
};
