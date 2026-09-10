<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

/**
 * H1 · Rol 'student' ELIMINADO del producto (pivote Bucaramanga Mobility).
 *
 * Solo DATOS: UPDATE massivo student -> pasajero. El enum físico de la BD
 * (ver 2026_09_08_120001) conserva 'student' como valor tolerado para que
 * una app publicada antigua (que aún envía/espera 'student') no reviente
 * durante la transición — el código ya trata ambos como equivalentes vía
 * User::isPasajero(). down() invierte el mapeo para rollback limpio.
 */
return new class extends Migration
{
    public function up(): void
    {
        DB::table('users')->where('role', 'student')->update(['role' => 'pasajero']);
    }

    public function down(): void
    {
        DB::table('users')->where('role', 'pasajero')->update(['role' => 'student']);
    }
};
