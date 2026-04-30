<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class DemoUserSeeder extends Seeder
{
    public function run(): void
    {
        // Crear Estudiante
        User::updateOrCreate(
            ['email' => 'estudiante@unab.edu.co'],
            [
                'name'     => 'Estudiante de Prueba',
                'password' => Hash::make('password123'),
                'role'     => 'student',
            ]
        );

        // Crear Conductor
        User::updateOrCreate(
            ['email' => 'conductor@unab.edu.co'],
            [
                'name'     => 'Conductor de Prueba',
                'password' => Hash::make('password123'),
                'role'     => 'driver',
            ]
        );
    }
}
