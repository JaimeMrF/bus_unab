<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use App\Models\User;

class AdminSeeder extends Seeder
{
    public function run(): void
    {
        User::updateOrCreate(
            ['email' => 'admin@unab.edu.co'],
            [
                'name'     => 'Administrador UNAB',
                'email'    => 'admin@unab.edu.co',
                'password' => Hash::make('Admin2024*'),
                'role'     => 'admin',
            ]
        );
    }
}
