# PRODUCT — BUCARATRANSIT (app móvil KMP)

## Qué es
Plataforma de movilidad urbana tipo SaaS para Bucaramanga, Colombia. Unifica a las
empresas de buses de la ciudad en un ecosistema digital: rutas y ETA en vivo, wallet
prepago, QR dinámico de pago y cobro a bordo (mPOS conductor). Reemplaza el pago en
efectivo y reduce la evasión.

## Piezas
- **Backend Laravel** (API REST `/api/v1`, autenticación Google + email/contraseña,
  wallet/ledger, QR HMAC 60s, paneles Filament multi-tenant) — desplegado en la VM
  Azure `57.156.69.85` → `https://bucaratransit.duckdns.org`.
- **App Kotlin Multiplatform** (Android/iOS, Compose) — package técnico `com.vibra.bus` (congelado).

## Modos de uso (escena real)
- **Pasajero:** mapa, buscar ruta/parada, ETA, saldo de wallet, QR de abordaje. Usa la
  app de día y de noche, de pie o en movimiento, bajo sol y bajo la lluvia tropical.
- **Conductor (mPOS):** el celular escanea el QR y cobra en milisegundos, controla
  ocupación y llegadas. Uso a bordo, con luz variable.

## Compromiso de marca (20-sep-2026)
- Mascota = **leopardo/tigrillo santandereano** (Bucaramanga). Reemplaza al búho (símbolo
  académico UNAB, retirado del producto). 9 expresiones del leopardo en `composeResources/drawable/`.
- Tema = Bucaramanga: dorado leopardo + espresso (rosetas) + verde parques.
- Nombre = **BUCARATRANSIT** (decisión definitiva). No se usa estética UNAB (sin morado/azul universitario).
- Buses en el mapa = **2D top-down** (Canvas Compose) que rota por heading. El modelo 3D
  (`bus_unab_3d.glb`) y las vistas pre-renderizadas fueron descartados y eliminados del repo.

## No inventado / pendiente
- Google Maps API key nueva (MAPS_API_KEY en `local.properties` y `GOOGLE_MAPS_API_KEY` del backend).
- FCM_SERVER_KEY (push) opcional.
- Confirmación de que el web client id de Google coincide con el de Firebase.
