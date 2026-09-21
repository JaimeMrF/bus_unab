# HANDOFF — estado y próximos pasos (2026-09-20)

> Para el equipo: qué quedó hecho, qué queda pendiente y por dónde seguir. Complementa `PRODUCT.md` / `DESIGN.md`.

## 1. 🚍 Bus en el mapa — nuevo enfoque (top-down, ya implementado)

**Decisión:** Google Maps **sí** puede mostrar el bus "con dirección" si usamos **vista full-top + rotación por heading** (Google Maps no soporta modelos 3D reales; eso sería Mapbox). Implementado en Android:

- `MapViewComposable.android.kt`: ahora cada bus es un **marker de Google Maps con `ic_bus_top.webp`** (imagen top-down), `flat = true` (se tiende sobre el mapa) y `rotation = bus.heading` (apunta hacia dónde va). Se eliminó todo el overlay Canvas (`BusIconOverlay`), la proyección por pantalla y el código 3D.
- Imagen copiada a `androidMain/res/drawable/ic_bus_top.webp` (para `BitmapFactory.decodeResource`).
- `MapViewComposable.kt` (common/iOS): marcador del bus alineado al dorado leopardo (ya no morado UNAB).

**Pendiente de PROBAR:** rebuild en Android Studio + E2E en dispositivo (rotación del bus con heading, tap → detalle). El `rotation`/`icon` del marker de maps-compose hay que validarlo en el emulador.

## 2. 🗺️ Mapa / 3D — decisión futura

- **Quedarse en Google Maps** con el bus top-down (este cambio). Cero riesgo, sin dependencias nuevas.
- Si más adelante se quiere **3D real** (bus tridimensional viendo hacia dónde va): es **Mapbox** (ModelLayer con `.glb`). Hacer un **spike/PoC** de una pantalla antes de migrar todo el mapa. No cambiar de proveedor a mitad de release.

## 3. 🖥️ Backend — deploy en la VM (bucaratransit.duckdns.org)

**Estado:** desplegado y funcional → Docker (nginx + Laravel + MySQL + Redis + worker + scheduler) corriendo en la VM Azure `57.156.69.85`; BD migrada + seed (3 users, 2 transportadoras, 3 buses, 22 paradas); API responde (401 sin token ✅, `/auth/google` viva ✅); `GOOGLE_CLIENT_ID` configurado.

**PENDIENTE (bloqueado por el usuario):**
1. **Abrir puertos 80 y 443** en el Network Security Group de Azure (hoy solo 22). Portal: VM → Networking → NSG → Inbound rules → agregar 80 y 443.
2. Al abrirlos: emitir cert Let's Encrypt (webroot, dominio `bucaratransit.duckdns.org`, email `jtellez312@unab.edu.co`), activar el bloque HTTPS de `docker/nginx/default.conf` (ya listo), verificar de punta a punta. La app ya apunta a `https://bucaratransit.duckdns.org/api/v1` (build.gradle.kts).

**Secretos a llenar en `.env.production` del servidor (hoy vacíos):**
- `GOOGLE_MAPS_API_KEY` (mapas).
- `FCM_SERVER_KEY` (opcional, notificaciones push — sin ella lo demás funciona).
- `GPSMOBILE_*` (opcional, tracking GPS real — hoy buses de seed).

> ⚠️ `.env.production` vive SOLO en el servidor (~/app), no se commitea; la app Android lleva su `GOOGLE_SERVER_CLIENT_ID` en build.gradle.kts.

## 4. 🐆 Branding Bucaramanga (hecho)

- Leopardo como mascota: 9 imágenes en `composeResources/drawable/leopardo_*.webp` (7 usadas en código; `leopardo_ok` y `leopardo_celebrando` sin uso aún).
- Paleta en `theme/Color.kt`: LeopardGold / Espresso / Sand / ParqueGreen. Screens ya re-tematizados.
- Se eliminaron búhos y assets 3D (`bus_unab_3d.glb`, vistas por ángulo).
- Residuales alineados: morado mapa → dorado, glass input → dorado, verde Material → `TransportColors.Success`.

## 5. Próximos pasos recomendados (por prioridad)

1. **Rebuild + E2E del mapa** en Android Studio (validar marker top-down rotado).
2. **Abrir NSG 80/443** → terminamos SSL + HTTPS público + verificación externa del backend.
3. Decidir **GOOGLE_MAPS_API_KEY** nueva (hoy vacía en local.properties) para que el mapa cargue.
4. (Opcional) Definir/liquidar las 2 imágenes de leopardo sin uso y el verde/ámbar residual restante.

**Revisión de diseño pendiente (impeccable):** pasada de finish (contrastes 4.5:1 en fondos de color) y validación visual en emulador/dispositivo de los screens de login, home, mapa y modo conductor.
