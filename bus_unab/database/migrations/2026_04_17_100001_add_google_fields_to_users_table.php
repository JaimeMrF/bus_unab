<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->string('google_id')->nullable()->unique()->after('id');
            $table->string('avatar')->nullable()->after('google_id');
            $table->enum('role', ['student', 'admin'])->default('student')->after('email');
            $table->string('password')->nullable()->change(); // nullable: login es solo via Google
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn(['google_id', 'avatar', 'role']);
            $table->string('password')->nullable(false)->change();
        });
    }
};
