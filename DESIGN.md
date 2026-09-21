# Design — BUCARATRANSIT · Bucaramanga / leopardo

<!-- impeccable:design-schema 1 -->

## World

**Leopardo + "Ciudad de los Parques"**. La app se siente de y para Bucaramanga: mascota leopardo (símbolo santandereano), dorado del leopardo como acento, verde de parques, sobre bases cálidas arena (light) / espresso con rosetas (dark). Glassmorphism como material de los paneles; mapa minimalista tipo Uber. Reemplaza el mundo académico UNAB (búho, morado).

## Palette

Definida en `presentation/theme/Color.kt`:

| Token | Hex | Rol |
|---|---|---|
| LeopardGold | `#E8A33D` | Primario / dorado leopardo |
| LeopardEspresso | `#17130E` | Fondo dark (rosetas) |
| LeopardSand | `#FDF8F0` | Fondo light (arena cálida) |
| ParqueGreen | `#3D6B4F` | Secundario / verde parques |
| onPrimary light | `#241807` | Texto sobre dorado |
| TransportColors | — | success/warning/ocupación/estado bus |

Estrategia de color: **Committed** — el dorado leopardo lleva la superficie (primario dominante), verde parque como secundario. Light = arena cálida; dark = espresso con dorado.

## Components / System

- **Mascota leopardo** en 9 expresiones (saludo, curioso, triste, triste_espera, celular, mapa, conductor, ok, celebrando) en `composeResources/drawable/leopardo_*.webp`. Login usa `leopardo_saludo`; empty states `leopardo_curioso`/`_triste`; conductor `leopardo_conductor`; etc.
- **Glass panels**: `GlassColors` (Surface/Border/Highlight blanco translúcido; variante dark espresso glass) — paneles flotantes sobre el mapa.
- **Botones**: `GlassButton` / `PrimaryGlassButton` / `SecondaryGlassButton` (glass con resplandor).
- **Barra de navegación**: `BottomNavBar` con vibraBusColors.
- **Mapa** (Google Maps, `maps.compose`): bus renderizado **2D Canvas orientado por heading** (`BusIconOverlay`), paradas con marcadores, polilínea punteada para ETA. Leopard gold/espresso en el bus del mapa.
- **QR** de pasajero (rotativo) y scanner conductor (mPOS).

## Type

Fuente de display con carácter (`rubikGlitchFamily`) para "Plataforma de Movilidad"; el resto Material 3 (`Typography.kt`). (Verificar tamaños/medida en la pasada de finish.)

## Behavior / Modes

- Operate (rider/driver): escaneabilidad y consistencia sobre expresión; la marca vive en detalles (leopard, dorado, glass).
- Estados cubiertos: loading (shimmer), empty (leopardo curioso/triste), error (leopardo muy triste), llegada ("¡Tu bus está llegando!").

## Recording

Este DESIGN.md describe el mundo ya construido en código (no un reemplazo); verifica contra `Color.kt`, los screens y las imágenes `leopardo_*` antes de editar.
