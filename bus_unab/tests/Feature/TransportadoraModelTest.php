<?php

namespace Tests\Feature;

use App\Models\Transportadora;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Validation\ValidationException;
use Tests\TestCase;

/**
 * M3 · S3.1.1 — Ciclo de vida del modelo raíz de tenant.
 */
class TransportadoraModelTest extends TestCase
{
    use RefreshDatabase;

    private function payload(array $overrides = []): array
    {
        return array_merge([
            'nombre'         => 'Transportadora de Prueba',
            'slug'           => 'trans-prueba',
            'contacto_email' => 'prueba@trans.co',
        ], $overrides);
    }

    public function test_creates_transportadora_with_defaults(): void
    {
        $t = Transportadora::create($this->payload());
        $t->refresh(); // defaults de plan/activo viven en la BD; sin refetch el modelo los tiene null en memoria

        $this->assertDatabaseHas('transportadoras', [
            'id'     => $t->id,
            'slug'   => 'trans-prueba',
            'plan'   => 'basico',
            'activo' => true,
        ]);
        $this->assertTrue($t->activo);
        $this->assertSame('basico', $t->plan);
    }

    public function test_slug_is_unique(): void
    {
        Transportadora::create($this->payload());

        $this->expectException(\Illuminate\Database\QueryException::class);
        Transportadora::create($this->payload(['nombre' => 'Otra']));
    }

    public function test_soft_delete_hides_row_but_keeps_it_recoverable(): void
    {
        $t = Transportadora::create($this->payload());

        $t->delete();

        $this->assertSoftDeleted('transportadoras', ['id' => $t->id]);
        $this->assertNull(Transportadora::find($t->id));
        $this->assertNotNull(Transportadora::withTrashed()->find($t->id));

        // con soft delete la fila NO libera el slug en la BD (constraint sigue ahí):
        // el borrado lógico conserva la auditoría del tenant.
        $t->restore();
        $this->assertNotNull(Transportadora::find($t->id));
    }

    public function test_has_many_relations_resolve(): void
    {
        $t = Transportadora::create($this->payload());

        $this->assertCount(0, $t->buses);
        $this->assertCount(0, $t->stops);
        $this->assertCount(0, $t->users);
    }
}
