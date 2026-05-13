<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call([
            BusSeeder::class,
            StopSeeder::class,
            AdminSeeder::class,
            DemoUserSeeder::class,
            Route2Seeder::class,
        ]);
    }
}
