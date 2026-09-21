# DESIGN — BUCARATRANSIT · Mundo visual "Leopardo de Bucaramanga"

> World creado el 20-sep-2026 tras el pivote de UNAB (búho/azul) → BUCARATRANSIT (leopardo/dorado).
> Reemplaza el mundo visual académico anterior; no es un refinamiento de él.

## Idea central
Bucaramanga es la "Ciudad de los Parques" y el leopardo/tigrillo santandereano es su
emblema. La app se siente como la ciudad: **oro cálido del leopardo**, **espresso** (las
rosetas / noche tropical) y **verde de los parques**. Operate: movilidad clara y de
alto contraste, con la personalidad del leopardo en los detalles (mascota, estados vacíos,
directividad del bus).

## Paleta (tokens en `theme/Color.kt`)
- `LeopardGold  #E8A33D` — primario, dorado.
- `LeopardEspresso #17130E` — fondo oscuro (rosetas), glass oscuro.
- `LeopardSand  #FDF8F0` — fondo claro (arena cálida).
- `ParqueGreen  #3D6B4F` — secundario, verde parques (acento).
- Errores rojos, ocupación verde/ámbar/rojo (sin cambios).

### Luz (día, uso al aire libre soleado)
Fondo arena cálido, primario dorado, verde parques como secundario, texto #211C14 (>4.5:1).

### Oscuro (noche, a bordo)
Fondo espresso #17130E, primario dorado, verde claro #8FD49F secundario, texto #ECE3D4.

## Mascota — sistema de 9 expresiones
Archivos `leopardo_*.webp` (800×800, fondo transparente) en `composeResources/drawable/`:

| Recurso | Uso |
|---|---|
| `leopardo_saludo` | Login + Mi QR |
| `leopardo_curioso` | Vacío por defecto / Home / Lista paradas |
| `leopardo_triste` | Estados de error |
| `leopardo_triste_espera` | Mis viajes (sin viajes) |
| `leopardo_celular` | Notificaciones / Esperando bus |
| `leopardo_mapa` | Buscar ruta / Esperando bus |
| `leopardo_conductor` | Modo conductor |
| `leopardo_celebrando` · `leopardo_ok` | Reserva (sin referencia aún) |

## Bus en el mapa
2D top-down dibujado con Canvas Compose (`MapViewComposable.android.kt`, `BusIconOverlay`),
rota por `heading`. Carrocería dorada en claro / espresso en oscuro, franja alternada,
flecha de dirección. El modelo 3D y las vistas por ángulo están fuera del repo.

## Tipo / forma / glass
- `Theme.kt` con Material 3, `Typography.kt`/`Shape.kt` existentes (no se tocaron).
- Glassmorphism cálido: `GlassColors` (espresso glass en oscuro).
- Login: gradiente primario→primarioContainer, etiqueta "⭐ TODA BUCARAMANGA".

## Prohibiciones (craft floor)
- Sin morado/azul universitario UNAB (eliminado de theme, grádientes, notificaciones, mapa).
- Sin emoji supliendo iconografía real del sistema; el ⭐ es etiqueta de marca existente.
- Contraste texto ≥4.5:1 en cuerpo; alto contraste en los dos modos.
