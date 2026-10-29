<?php

namespace Tests\Feature;

use Tests\TestCase;

class LandingPageTest extends TestCase
{
    public function test_root_serves_the_public_landing_instead_of_redirecting(): void
    {
        $this->get('/')
            ->assertOk()
            ->assertSee('BUCARATRANSIT')
            ->assertSee('landing/landing.css', false);
    }

    public function test_landing_assets_and_deck_exist(): void
    {
        foreach (['landing/landing.css', 'landing/landing.js', 'landing/img/logo.webp', 'presentacion/index.html'] as $file) {
            $this->assertFileExists(public_path($file));
        }
    }
}
