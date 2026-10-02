<?php

namespace Tests\Feature\Api;

use App\Models\Transportadora;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Tests\TestCase;

class BrandingCacheTest extends TestCase
{
    use RefreshDatabase;

    private function tenant(): Transportadora
    {
        return Transportadora::create(['nombre' => 'Acme', 'slug' => 'acme']);
    }

    public function test_second_request_does_not_query_transportadoras(): void
    {
        $this->tenant();
        $this->getJson('/api/v1/branding/acme')->assertOk();

        DB::enableQueryLog();
        $this->getJson('/api/v1/branding/acme')->assertOk();
        $queries = collect(DB::getQueryLog())->filter(fn ($q) => str_contains($q['query'], 'transportadoras'));

        $this->assertCount(0, $queries);
    }

    public function test_saving_tenant_invalidates_cache(): void
    {
        $t = $this->tenant();
        $this->getJson('/api/v1/branding/acme')->assertJsonPath('data.app_name', 'Acme');

        $t->update(['branding' => ['app_name' => 'Nueva']]);

        $this->getJson('/api/v1/branding/acme')
            ->assertJsonPath('data.app_name', 'Nueva')
            ->assertJsonPath('data.version', 2);
    }

    public function test_deactivating_tenant_invalidates_cache(): void
    {
        $t = $this->tenant();
        $this->getJson('/api/v1/branding/acme')->assertOk();

        $t->update(['activo' => false]);

        $this->getJson('/api/v1/branding/acme')->assertNotFound();
    }

    public function test_slug_change_invalidates_old_key(): void
    {
        $t = $this->tenant();
        $this->getJson('/api/v1/branding/acme')->assertOk();

        $t->update(['slug' => 'acme2']);

        $this->getJson('/api/v1/branding/acme')->assertNotFound();
        $this->getJson('/api/v1/branding/acme2')->assertOk();
    }

    public function test_misses_are_not_cached(): void
    {
        $this->getJson('/api/v1/branding/futuro')->assertNotFound();
        $this->assertFalse(Cache::has('branding:futuro'));

        Transportadora::create(['nombre' => 'F', 'slug' => 'futuro']);
        $this->getJson('/api/v1/branding/futuro')->assertOk();
    }
}
