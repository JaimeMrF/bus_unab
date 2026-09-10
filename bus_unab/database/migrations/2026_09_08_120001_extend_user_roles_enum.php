<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * M3 · S3.3.1 — Amplía users.role para el modelo SaaS multi-tenant.
 *
 * Roles nuevos: 'pasajero' (alias visible de 'student'), 'super_admin'
 * (alias operativo de 'admin'), 'tenant_admin' (admin de transportadora).
 * Los roles históricos ('student','admin','driver') SIGUEN VÁLIDOS: no se
 * migra data, el backward compat es por aliasing en el modelo (User::isSuperAdmin()).
 *
 * Precedente sqlite: 2026_04_30_053309_fix_sqlite_roles.php usa ->change()
 * y migra limpio en sqlite (dev/tests) y MySQL (prod).
 */
return new class extends Migration
{
    private const ROLES = ['student', 'pasajero', 'admin', 'super_admin', 'tenant_admin', 'driver'];

    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->enum('role', self::ROLES)->default('student')->change();
        });
    }

    public function down(): void
    {
        // Solo se puede revertir si nadie usó los roles nuevos.
        Schema::table('users', function (Blueprint $table) {
            $table->enum('role', ['student', 'admin', 'driver'])->default('student')->change();
        });
    }
};
