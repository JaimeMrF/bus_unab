<?php

namespace Tests\Feature;

// use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ExampleTest extends TestCase
{
    /**
     * A basic test example.
     */
    public function test_unauthenticated_api_request_returns_401(): void
    {
        $this->getJson('/api/v1/buses')->assertStatus(401);
    }
}
