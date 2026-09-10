<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

class DemoUserSeeder extends Seeder
{
    public function run(): void
    {
        // Crear Pasajero (H1: rol 'student' eliminado; email legacy se conserva
        // porque AuthTest y las credenciales documentadas lo usan)
        User::updateOrCreate(
            ['email' => 'estudiante@unab.edu.co'],
            [
                'name'     => 'Estudiante de Prueba',
                'password' => Hash::make('password123'),
                'role'     => 'pasajero',
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
