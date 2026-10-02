# PRODUCT — Plataforma de movilidad white-label (app móvil KMP)

## Qué es
Plataforma SaaS de movilidad urbana que cada **organización (transportadora)** ofrece con su
propia identidad: rutas y ETA en vivo, wallet prepago, QR dinámico de pago y cobro a bordo
(mPOS del conductor). Una sola base de código sirve a N organizaciones; la marca se
configura, no se programa.

## Piezas
- **Backend Laravel** (API REST `/api/v1`, Sanctum, Google + email/contraseña, wallet/ledger,
  QR HMAC de vida corta, paneles Filament multi-tenant). Endpoint público de marca:
  `GET /api/v1/branding/{slug}` (ETag, `max-age=300`, 404 genérico).
- **App Kotlin Multiplatform** (Android/iOS, Compose Multiplatform), package técnico
  `com.vibra.bus` (congelado por compatibilidad de publicación).

## Identidad por organización (`BrandConfig`)
`app_name`, `tagline`, `support_email`, `logo_url`/`logo_dark_url`/`icon_url`/`mascot_url`,
paleta clara y oscura de 11 colores, `font_family` (`poppins`|`inter`|`system`),
`corner_radius` (`sm`|`md`|`lg`), `features` (`qr_payments`, `wallet`, `driver_mode`) y
`version`. Las imágenes son opcionales: sin ellas se muestra un placeholder neutro.

### Resolución de la organización
1. `-PdefaultOrgSlug=<slug>` en el build (BuildConfig) para apps dedicadas a un tenant; o
2. pantalla **Código de organización** en el primer arranque.
3. Tras iniciar sesión, si `organization_slug` de `/auth/*` difiere del activo, se adopta y
   se recarga la marca.
La marca y el slug se guardan en `AppSettings`: la app arranca offline con la última marca
y, si nunca hubo una, con el tema neutro embebido.

## Modos de uso (escena real)
- **Pasajero:** mapa, buscar ruta/parada, ETA, saldo de wallet, QR de abordaje. Uso de día
  y de noche, de pie o en movimiento, con luz y señal variables.
- **Conductor (mPOS):** escanea el QR y cobra en milisegundos; controla ocupación y llegadas.
  Uso a bordo, con una mano.

## Mapa
Buses y ruta se dibujan con el color primario del tenant (capas MapLibre + OpenFreeMap, sin
API key). La vista inicial es genérica y se recentra con el GPS del usuario.

## Principios de producto
- Ninguna marca, nombre, color o mascota de un cliente concreto en el código.
- Nunca bloquear al usuario por un fallo de red de marca: se degrada a caché o a tema neutro.
- Accesibilidad AA y estados de carga/vacío/error en cada pantalla.

## Pendiente / no inventado
- Soporte de logos SVG (requiere `coil-svg`); hoy se aceptan PNG/WebP.
- Fuente Inter embebida (hoy `inter` usa la sans del sistema).
- FCM_SERVER_KEY (push) opcional; confirmar que el web client id de Google coincide con Firebase.
- Recentrado del mapa desde el botón "mi ubicación" (control retirado hasta tener API de cámara común).
