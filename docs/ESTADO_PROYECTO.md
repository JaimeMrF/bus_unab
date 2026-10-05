# Estado del proyecto — plataforma white-label de movilidad

> Documento de lo ya construido en la transición de "app de bus de una universidad" a **plataforma vendible a varias organizaciones**, cada una con su propia marca. Actualizado el 2026-10-05. Para arrancar una demo ver [DEMO.md](DEMO.md); para dar de alta una organización ver [ONBOARDING_TENANT.md](../bus_unab/docs/ONBOARDING_TENANT.md).

## 1. Idea central

Una sola aplicación y un solo backend sirven a N organizaciones (tenants). Cada organización tiene un **perfil de marca** (nombre, logos, mascota opcional, colores claro/oscuro, tipografía, radio de esquinas, funciones activas). La app lo descarga en runtime y se re-tematiza completa: no hay logo, color ni texto de marca fijo en el código.

```
Admin de la organización ──edita──► Panel Filament /empresa ("Marca")
                                         │ guarda branding + sube versión
                                         ▼
App móvil ──GET /api/v1/branding/{slug}──► Backend Laravel (caché + ETag)
   │ cachea último perfil (offline)
   └─ si falla: tema neutro embebido
```

## 2. Backend (Laravel, `bus_unab/bus_unab`)

| Área | Qué se hizo |
|---|---|
| Branding | Migración nueva con columnas de branding y versionado en `transportadoras`; `BrandingService`; reglas de validación (hex `#RRGGBB`, contraste mínimo 4.5:1 entre color primario y su texto, SVG saneado, imágenes por tipo real y tamaño máx. 1 MB) |
| API pública | `GET /api/v1/branding/{slug}`: ETag, `Cache-Control: public, max-age=300`, 304, 404 idéntico para inexistente/inactivo (no revela existencia), slug normalizado a minúsculas, caché en servidor por slug invalidada al guardar |
| Panel | Página "Marca" para el admin de cada organización en `/empresa` y sección para super admin; un admin solo edita su propia organización |
| Auth | `organization_slug` en login, Google y `/auth/me`; `organization` opcional en `/auth/register` |
| Seguridad | CORS restringido (`CORS_ALLOWED_ORIGINS`); se valida el emisor (`iss`) del token de Google; expiración Sanctum configurable (`SANCTUM_EXPIRATION`); `rechargeMock` solo en local/testing; limitadores de peticiones nombrados (`auth`, `branding`, `api-N`) con contador propio para que un endpoint no agote el de otro (bug real detectado y corregido: consultar branding bloqueaba el login) |
| Demo | `demo:setup` (migra y siembra 3 organizaciones con marcas muy distintas, usuarios por rol, buses, tarifas, wallets) y `demo:simulate-buses` (mueve buses por su ruta); solo local/testing; ubicación del conductor unificada en un único formato |
| Tests | 188 tests verdes: branding, validación, aislamiento entre organizaciones, rate limiting, seguridad base |

## 3. App móvil (Kotlin Multiplatform / Compose, `frontend`)

- **Marca dinámica**: `BrandConfig`/`BrandApi`/`BrandRepository` con caché offline y tema neutro de respaldo. El tema (colores con contraste AA garantizado, tipografía Poppins/Inter/sistema, radios sm/md/lg) sale del perfil. Las funciones (wallet, escáner, modo conductor) se muestran u ocultan por organización.
- **Código de organización**: pantalla nueva; también se puede fijar por build (`-PdefaultOrgSlug`).
- **Servidor configurable en runtime** (ya no hay `localhost` fijo): "Opciones avanzadas > Servidor" valida y prueba la URL antes de guardarla. Prioridad: valor del dispositivo > `-PapiBaseUrl` > por defecto. HTTP sin cifrar solo en debug y solo hacia hosts locales; release es solo HTTPS.
- **Design system neutro**: tokens (espaciado, elevación, tamaños, movimiento), componentes (botones, campos, tarjetas, panel de vidrio, etiquetas de estado, barra superior, barra inferior, shimmer, logo/mascota de la organización) y las 15 pantallas rediseñadas con estados de carga, vacío y error, accesibilidad (descripciones, encabezados, zonas táctiles de 48 dp) y selector Sistema/Claro/Oscuro.
- **Animaciones optimizadas** (`presentation/motion`): fondo aurora, transición del splash, entrada escalonada de listas, saldo animado, anillo de cuenta regresiva del QR, ondas en la espera, indicador de la barra inferior con resorte, efecto de pulsación, confeti ligero (máx. 36 partículas), parallax. Se dibujan sin recomponer la interfaz, se pausan con la app en segundo plano, respetan "reducir movimiento" del sistema y los dispositivos de gama baja usan una versión estática.
- **Limpieza**: eliminada la marca anterior (leopardo, UNAB, Bucaramanga, VibraBus) de la interfaz, notificaciones y mapa; logs de red solo en debug con `Authorization` oculto.

## 4. Infraestructura y repo

- **Producción:** el backend está publicado en **https://bucaratransit.duckdns.org** (VM Azure `157.56.9.42`, Docker Compose con nginx + Laravel + MySQL + Redis + worker + scheduler, HTTPS de Let's Encrypt con renovación por cron y respaldo diario de la base). El build de la app apunta ahí por defecto. Detalle del despliegue y hallazgos en [PLAN_REVISION.md](PLAN_REVISION.md).
- **`scripts/dev-up.ps1` / `.sh`**: un comando levanta el servidor accesible desde otros dispositivos de la red, carga datos demo, detecta la IP, abre el firewall e imprime URL, credenciales y comando de instalación. `-Fresh`, `-Simulate`, `-Queue`, `-Port`, `-Stop`. Probado en Windows; la versión `.sh` solo con `bash -n`.
- **CI** (`.github/workflows`): backend (formato + tests PHP 8.2/8.3), frontend (build debug Android), escaneo de secretos semanal (gitleaks) y Dependabot. Aún sin ejecutar en GitHub.
- **Hardening**: cabeceras de seguridad y límite de peticiones en nginx, Dockerfile reproducible, `.env.example` genérico sin secretos, `.gitignore` ampliado, archivos de caché y configuración local sacados del control de versiones.
- **Flujo de commits**: autor JaimeMrF, sin trailer de IA; cada commit se sube a `main` con un hook local `post-commit`.

## 5. Pendientes y riesgos

| Prioridad | Tema | Detalle |
|---|---|---|
| Alta | Secretos en el historial | Clave de Google Maps y keystore `vibra-bus.jks` estuvieron commiteados. Rotar la clave; valorar rotar la firma. Gitleaks fallará sobre el historial hasta entonces. Limpiar historial requiere decisión explícita |
| Alta | Verificación en dispositivo | La app se compiló en Android Studio y se vio bien; falta medir fluidez de la aurora en gama media y probar "Quitar animaciones" |
| Media | Rendimiento y QR | En curso: perfilado de arranque/mapa/listas y auditoría de casos límite del QR (replay, expiración, doble cobro, concurrencia) |
| Media | Nombre del producto | **Resuelto:** la marca es BUCARATRANSIT; el paquete `com.vibra.bus` y el proyecto Gradle `VibraBus` siguen con el identificador congelado (no es branding visible) |
| Media | Íconos de la app | **Resuelto:** el launcher y el favicon salen del logo oficial vía `frontend/tools/gen_brand_assets.py`; `ic_notification` es la silueta blanca de bus que Android tiñe |
| Media | Firma del release | `local.properties` no tiene `KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD`, así que `assembleRelease` no firma. Decidir si se usa el keystore actual o se genera uno nuevo |
| Baja | Mapa | Sin pulso animado sobre bus/parada (capa nativa de MapLibre) |
| Baja | CI y nginx | Workflows sin ejecutar en GitHub; nginx **sí** validado con `nginx -t` en el VPS (producción) |
| Baja | iOS | Servidor de desarrollo requiere https o `Info.plist`; sin probar en simulador |
| Baja | Pruebas de UI | Hay tests de tema/branding/URL, no de pantallas Compose |
