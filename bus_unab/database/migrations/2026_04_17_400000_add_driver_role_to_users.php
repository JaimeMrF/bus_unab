<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    /**
     * Agrega el rol 'driver' (conductor) al enum de roles de usuarios.
     *
     * SQLite no impone restricciones de enum (almacena como TEXT), por lo que
     * solo se requiere alterar la columna en MySQL/MariaDB.
     */
    public function up(): void
    {
        if (DB::getDriverName() === 'mysql') {
            DB::statement(
                "ALTER TABLE users MODIFY COLUMN role ENUM('student', 'admin', 'driver') NOT NULL DEFAULT 'student'"
            );
        }
        // SQLite: no requiere cambio — el valor 'driver' es válido como TEXT.
    }

    public function down(): void
    {
        if (DB::getDriverName() === 'mysql') {
            // Primero degradar cualquier driver a student para no violar el constraint
            DB::statement("UPDATE users SET role = 'student' WHERE role = 'driver'");
            DB::statement(
                "ALTER TABLE users MODIFY COLUMN role ENUM('student', 'admin') NOT NULL DEFAULT 'student'"
            );
        }
    }
};
