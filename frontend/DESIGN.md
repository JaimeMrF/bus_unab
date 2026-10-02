# DESIGN — Sistema visual white-label

Sistema visual **neutro y themable**: ninguna marca, color, mascota ni tipografía de una
organización vive en el código. Todo aspecto de marca llega en runtime como `BrandConfig`
(`GET /api/v1/branding/{slug}`) y `AppTheme` lo traduce a Material 3.

## Principios
1. **Neutral por defecto, de marca por configuración.** Sin `BrandConfig` la app usa el tema
   neutro embebido (`BrandConfig.Neutral`): azul pizarra, sin logo ni mascota.
2. **Contraste primero (WCAG AA).** El cliente garantiza ≥4.5:1 (texto) y ≥3:1 (bordes) con
   `ensureContrast()`, aunque el tenant elija una paleta pobre.
3. **Calma visual.** Superficies tonales con borde fino en lugar de sombras pesadas; el
   color de marca se reserva para la acción principal y el estado activo.
4. **Movimiento con propósito.** Entradas escalonadas cortas, escala sutil al presionar;
   nada decorativo en bucle salvo el shimmer de carga.
5. **Todos los estados diseñados:** carga (shimmer), vacío, error con reintento, sin conexión.

## Tokens (`presentation/theme/`)
| Archivo | Contenido |
|---|---|
| `Color.kt` | `BrandPalette → ColorScheme` (claro/oscuro), `AppColors` (glass, éxito, aviso, estados de bus), `contrastRatio`, `readableOn`, `ensureContrast` |
| `Typography.kt` | Escala M3 completa (`display`…`label`) sobre la familia de marca: `poppins` (embebida), `inter`/`system` (sans del sistema) |
| `Shape.kt` | `AppShape.*` semántico (Card, Chip, Sheet, Input…) escalado por `corner_radius` (`sm` 0.6×, `md` 1×, `lg` 1.4×) |
| `Tokens.kt` | `Spacing` (4/8/12/16/24/32/48), `Elevation`, `Sizing.touchTarget = 48dp`, `Motion` (120/220/360 ms, curvas) |
| `Theme.kt` | `AppTheme(brand, darkTheme)`, `LocalBrand`, `MaterialTheme.appColors` |

### Derivación de color desde las 11 claves de marca
`primary`, `on_primary`, `secondary`, `on_secondary`, `background`, `surface`, `on_surface`,
`accent`, `success`, `warning`, `error` (hex `#RRGGBB`, por modo). El resto del esquema se
deriva: contenedores = mezcla de color con `surface` (86 % claro / 78 % oscuro),
`surfaceContainer*` = mezclas de `surface` hacia `onSurface`, `outline` con ≥3:1,
`onSurfaceVariant` con ≥4.5:1. `accent` → `tertiary`; mapa y ruta usan `primary`.

### Modo claro / oscuro
Preferencia del usuario `Sistema` (por defecto) · `Claro` · `Oscuro`
(`AppSettings.themeMode`). La paleta de cada modo la define el tenant.

## Componentes (`presentation/components/`)
- **Botones:** `PrimaryButton`, `SecondaryButton`, `GoogleSignInButton` (≥52 dp, estado de carga anunciado, escala al presionar sin recomposición).
- **Campos:** `AppTextField` (etiqueta persistente, error asociado, ≥56 dp).
- **Superficies:** `AppCard`, `GlassPanel` (vidrio sutil sobre el mapa), `StatusPill` (color + texto + punto "en vivo"), `ScreenHeader`.
- **Navegación:** `AppTopBar` (superficie neutra, título como heading), `BottomNavBar` (M3 `NavigationBar`, etiquetas siempre visibles; oculta Wallet/Escáner según `features`).
- **Marca:** `BrandLogo`, `BrandMascot`, `NeutralBadge` (Coil por URL con placeholder neutro).
- **Estados:** `EmptyState`, `ShimmerBox/ShimmerList`, `Staggered` (entrada escalonada).

## Accesibilidad
- Áreas táctiles ≥48 dp (`Sizing.touchTarget`); filas de lista ≥64 dp.
- Cada acción de gesto (deslizar para cancelar/eliminar) tiene alternativa con botón.
- Estado nunca solo por color: siempre texto o icono (`StatusPill`, banners del escáner).
- `contentDescription` en iconos accionables; decorativos con `null`. Encabezados con `heading()`.
- Cambios dinámicos (cuenta atrás de QR, validación de escaneo, "bus llegando") usan `liveRegion`.
- El QR siempre se dibuja sobre blanco para máxima legibilidad de lectura.

## Rendimiento Compose
- Animaciones de presión y shimmer se leen en la fase de dibujo (`graphicsLayer`, `drawWithCache`): no recomponen.
- `remember` de `ColorScheme`/`Shapes`/`Typography` keyed por paleta/modo/escala.
- Listas con `key` estable y `animateItem()`; sin `composed {}` en modificadores.
- Un solo `AppTheme` en la raíz; las pantallas anidadas heredan `CompositionLocal`.

## Prohibiciones
- Colores, nombres, logos, mascotas o emojis de una marca concreta en código o recursos.
- `Color(0x…)` fuera de `theme/` (excepto negro/blanco funcionales: QR, sombras).
- Texto sobre color de marca sin pasar por `readableOn`/`ensureContrast`.
- Emojis como iconografía: usar iconos Material.
