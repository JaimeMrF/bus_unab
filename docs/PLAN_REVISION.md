# Plan de revisión — app BUCARATRANSIT + backend PHP

> Documento de trabajo (2026-10-04). Verificación hecha sobre el árbol en `main` @ `2f5823e`.
> Complementa `docs/ESTADO_PROYECTO.md`, `docs/DEMO.md` y `frontend/PERFORMANCE.md`.

## 0. Veredicto corto

**El código está listo; el despliegue no.** Todo lo que se puede verificar sin el teléfono está en verde
(356 pruebas, compila, íconos y favicon correctos, cero búhos). Lo único que impide probar la app
"de verdad" contra el VPS es que **los puertos 80 y 443 siguen cerrados** en el servidor: hoy solo
responde el 22. Todo lo demás que fallaba en marca/logo ya quedó corregido en este pase (ver §2.2).

## 1. Estado verificado (evidencia de hoy, no lectura de docs)

| Qué | Resultado | Evidencia |
|---|---|---|
| Backend, suite completa | **286 pruebas verdes**, 3 omitidas (hallazgos QA abiertos) | `php vendor/phpunit/phpunit/phpunit --no-coverage` |
| App Android, compilación | **BUILD SUCCESSFUL in 2m 12s** | `:composeApp:compileDebugKotlinAndroid` |
| App, pruebas unitarias | **70 pruebas, 0 fallos** | `:composeApp:testDebugUnitTest` |
| Ícono de la app (launcher) | **Logo real BUCARATRANSIT** (monograma BT + wordmark) sobre blanco, en 5 densidades + Play Store 512 | `frontend/composeApp/src/main/res/mipmap-*/`, `androidMain/ic_launcher-playstore.png` |
| Ícono de notificación | Silueta blanca de bus (correcto: Android la tiñe; multicolor se ve como manchón) | `mipmap-*/ic_notification.webp` |
| Favicon del backend | **Logo real BUCARATRANSIT** | `bus_unab/public/favicon.ico` |
| Marca del bus en el mapa | Bus visto desde arriba con el logo BT en el techo | `commonMain/composeResources/drawable/ic_bus_top.webp` |
| Búhos (UNAB) | **0 referencias** en código y 0 archivos: se borraron en `97b5c68` | `grep -ri "buho\|owl" composeApp/src` |
| Mascota del tenant `bucaratransit` | **9 poses de leopardo** (saludo, curioso, triste, espera, celular, mapa, conductor, ok, celebrando) | `bus_unab/database/seeders/assets/bucaratransit/*.webp` |
| DNS / red del VPS | `bucaratransit.duckdns.org` → `157.56.9.42`; **22 abierto, 80/443/8000 en timeout** | `nslookup`, sondeo de puertos |

## 2. Bloqueadores y hallazgos

### 2.1 BLOQUEANTE — puertos 80 y 443 cerrados en el VPS

- DNS resuelve bien; SSH (22) responde; 80, 443 y 8000 **no** (timeout de conexión, no error de certificado: no hay nada escuchando desde fuera).
- Consecuencia directa: el build de la app apunta por defecto a `https://bucaratransit.duckdns.org/api/v1` (`composeApp/build.gradle.kts`), así que **hoy la app no puede descargar la marca**, y cae al tema neutro con respaldo local. Ahí es donde aparecen los "símbolos de bus" genéricos en vez del logo/leopardo.

Acción 1 — **Abrir los puertos en el NSG de Azure.** Portal → la VM → *Redes* → *Reglas de puerto de entrada* → **Agregar regla de puerto de entrada**, dos veces:

| Campo | Regla HTTP | Regla HTTPS |
|---|---|---|
| Origen | `Any` | `Any` |
| Puertos de origen | `*` | `*` |
| Destino | `Any` | `Any` |
| Servicio | `Custom` | `Custom` |
| Intervalos de puertos de destino | `80` | `443` |
| Protocolo | `TCP` | `TCP` |
| Acción | `Permitir` | `Permitir` |
| Prioridad | `300` | `310` |
| Nombre | `HTTP-80` | `HTTPS-443` |

- **Origen tiene que ser `Any`**, no tu IP: el celular de cualquier usuario tiene que llegar al 443 desde cualquier red. Si lo restringes a tu IP, la app solo funciona en tu casa.
- Es un **bloqueador de puertos, no de rutas**: no hay que abrir nada para `/admin`, `/empresa` ni `/api`. Los tres van por el mismo 443.
- **No** hace falta abrir MySQL (3306) ni Redis (6379): en `docker-compose.yml` el único servicio que publica puertos es nginx; los demás viven dentro de la red de Docker.
- Si más adelante restringes el 22 a tu IP, hacelo aparte — pero el 22 no lo necesita nadie más.

Acción 2 — **Certificado y nginx en la VM.** Ojo con el orden: el `default.conf` ya tiene activo el bloque HTTPS apuntando a un certificado que todavía no existe, y nginx **no arranca** sin él. Por eso se emite primero con certbot en modo `standalone` (con nginx parado, para no pelear por el 80):

```
cd /opt/bus_unab/bus_unab
docker compose stop nginx
docker compose run --rm -p 80:80 certbot certonly --standalone -d bucaratransit.duckdns.org --email jtellez312@unab.edu.co --agree-tos --no-eff-email
docker compose up -d nginx
```

Después, desde cualquier dispositivo: `https://bucaratransit.duckdns.org/admin` y `https://bucaratransit.duckdns.org/empresa`.

### 2.2 CORREGIDO en este pase — el logo del tenant demo no era la marca real

El seeder de demo generaba, para `bucaratransit`, un **logo SVG sintético** (cuadrado azul + rectángulo blanco con forma de bus + la letra "B"), y además **Coil no decodifica SVG**, así que en la app terminaba cayendo a la letra inicial sobre el color primario. Nunca se veía el logo real.

- `frontend/tools/gen_brand_assets.py`: el logo de UI ahora se genera como raster en `bus_unab/database/seeders/assets/bucaratransit/logo.webp` (antes escribía en un drawable Compose que el pivote white-label había borrado: el script estaba desactualizado).
- `bus_unab/database/seeders/DemoSeeder.php`: nuevo `logo()` — usa el raster del set de assets si existe, si no el SVG genérico.
- Prueba nueva: `DemoSeederTest::test_bucaratransit_serves_the_real_raster_logo` (verde).
- Asset muerto eliminado: `frontend/composeApp/src/androidMain/res/drawable/ico.webp` era el ícono viejo "BUS UNAB" (búho conduciendo un bus morado), sin ninguna referencia en código, pero **empaquetado en el APK**.

> Ojo: esto arregla el **demo local**. El VPS no se toca con el seeder: para que la app muestre el logo real ahí hay que subirlo en el panel → `/empresa` → **Marca** → Logo (o correr `demo:setup` en el servidor, que solo funciona en `local|testing`).

### 2.3 Hallazgos QA abiertos (3 pruebas omitidas a propósito)

En `bus_unab/tests/Feature/EtaFavoritesEdgeTest.php`, se ejecutan con `RUN_QA_FINDINGS=1`:

| Severidad | Hallazgo | Impacto |
|---|---|---|
| baja | `/buses/{plate}/eta?stop_id=` devuelve ETA para una parada **que no pertenece a la ruta** de ese bus | dato inútil, no corrupción |
| baja | `stop_id: true` (JSON booleano) pasa la regla `integer` de Laravel y se guarda como `stop_id = 1` | favorito espurio |
| baja | No hay tope de favoritos por usuario (se pueden acumular todos los que existan) | ruido en BD |

### 2.4 Lo que la app NO puede mostrar sin marca (comportamiento correcto, no bug)

Sin branding cargado: `BrandLogo` cae a la inicial sobre el color primario, `BrandMascot` no emite nada
(sin hueco) y `EmptyState`/lista usan el ícono vectorial de bus. Es el respaldo neutro del diseño
white-label; desaparece en cuanto el branding carga.

## 3. Cómo probar la app

No se necesita Android Studio para instalar: el wrapper ya compila. En **PowerShell**, desde la raíz del repo.

### Camino A — local, sin abrir puertos (se puede hacer hoy)

```
cd "D:\jose sin tilde\bus_unab-1"
.\scripts\dev-up.ps1 -Fresh -Simulate
```

El script imprime la IP de la LAN, las credenciales y el comando de instalación. Copiar esa IP y, en otra terminal:

```
cd "D:\jose sin tilde\bus_unab-1\frontend"
.\gradlew.bat :composeApp:installDebug -PapiBaseUrl=http://IP_DE_TU_PC:8000/api/v1 -PdefaultOrgSlug=bucaratransit
```

Si el teléfono va por USB en vez de Wi-Fi:

```
C:\Users\jose\AppData\Local\Android\Sdk\platform-tools\adb.exe reverse tcp:8000 tcp:8000
.\gradlew.bat :composeApp:installDebug -PapiBaseUrl=http://127.0.0.1:8000/api/v1 -PdefaultOrgSlug=bucaratransit
```

Usuario demo: `pasajero.bucaratransit@demo.test` · conductor: `driver.bucaratransit@demo.test` · admin: `admin.bucaratransit@demo.test` — clave de todos `Demo12345!`.

> Cambiar `-PdefaultOrgSlug` a `metrobus`, `campus` o `logistica` muestra otras marcas (así se comprueba que el white-label funciona de verdad).
> Ojo: `dev-up.ps1` tiene que seguir corriendo mientras se prueba; se detiene con `.\scripts\dev-up.ps1 -Stop`.

### Camino B — contra el VPS (solo después de abrir 80/443)

```
curl -sS -o NUL -w "%{http_code}`n" https://bucaratransit.duckdns.org/api/v1/branding/bucaratransit
cd "D:\jose sin tilde\bus_unab-1\frontend"
.\gradlew.bat :composeApp:assembleDebug
C:\Users\jose\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
```

Sin `-PapiBaseUrl` el build ya trae la URL del dominio. Antes de instalar, subir el logo real en
`https://bucaratransit.duckdns.org/empresa` → Marca (§2.2), o la app mostrará el respaldo neutro.

## 4. Checklist de revisión en el teléfono

Marcar en el celular; lo que falle, anotar pantalla + captura.

### 4.1 Marca e íconos
- [ ] El ícono del launcher es el logo BUCARATRANSIT (no un bus genérico, no el búho morado).
- [ ] Splash: logo + "BucaraTransit" + mascota de saludo (leopardo). Sin huecos ni recuadros vacíos.
- [ ] Login: el mismo logo, tipografía Poppins, paleta azul rey `#01265A` + amarillo `#FCBB01`.
- [ ] Ajustes del sistema → Apariencia: ícono y nombre de la app.
- [ ] Llegar una notificación de prueba: el ícono es la silueta blanca de bus, **no** un cuadro blanco.

### 4.2 Búhos → leopardo
- [ ] Home sin buses: mascota "curioso"; con error de red: "triste".
- [ ] Espera de bus: "espera" al inicio y "celular" en el panel de viaje.
- [ ] Wallet y QR: "celular".
- [ ] Modo conductor: "conductor".
- [ ] Listas vacías / favoritos vacíos: "triste" (o ícono neutro, nunca búho).
- [ ] Cobro/celebración de QR: "celebrando" con confeti (36 partículas máx.).

### 4.3 Flujos funcionales
- [ ] Login por correo y (si aplica) por Google.
- [ ] Mapa: bus pintado con rumbo correcto, paradas, y la hoja de rutas.
- [ ] Paradas: lista, búsqueda instantánea, marcar/quitar favorito, y que sobreviva cerrar la app (pendientes offline).
- [ ] ETA: "llega en X min" al seleccionar parada y con `?eta=1` en la lista.
- [ ] Wallet: saldo, recarga, QR que rota 5 s antes de expirar.
- [ ] Escáner del conductor: cobro OK, QR repetido, QR vencido, sin red.
- [ ] Perfil: cambio de tema Sistema/Claro/Oscuro y cerrar sesión.

### 4.4 Rendimiento (gama media/baja)
- [ ] Aurora y listas fluidas; activar "reducir movimiento" en el sistema y confirmar versión estática.
- [ ] Arranque en frío con y sin marca cacheada.
- [ ] Batería/datos: el polling de buses es cada 5 s y se pausa con la app en segundo plano.

## 5. Pendientes de medir en dispositivo (heredados de `frontend/PERFORMANCE.md`)

Estos **no** se pueden cerrar por lectura, necesitan teléfono: tiempo a primer frame en frío, 60 fps de la
aurora, caché de disco de Coil (que el logo no se rebaje), build release con R8 (login, mapa, QR,
notificaciones, Google Sign-In) y tamaño real de las poses descargadas (tope 1 MB c/u).

## 6. Criterio de "lista"

La app se puede declarar lista para el piloto cuando: (a) los 3 hallazgos de §2.3 se decidan (arreglar o
aceptar por escrito), (b) el checklist §4 pasa en un teléfono real con el backend del VPS por HTTPS, y
(c) los pendientes de §5 quedan medidos. Antes de eso el código está listo, pero el sistema completo no.

## 7. Seguridad — pendiente inmediato

- La contraseña SSH del VPS quedó escrita en el chat: **rotarla** (`passwd` en la VM y actualizar el
  acceso), y pasar a llave pública (`bucaratransit_key.pem` ya está en el repo — revisar que no tenga
  passphrase débil y que la clave privada no se haya commiteado nunca).
- Sigue pendiente del informe anterior: rotar la API key de Google Maps y valorar rotar la firma del
  APK (el keystore `vibra-bus.jks` estuvo en el historial); gitleaks fallará sobre el historial hasta entonces.

---

# 8. RESULTADO DEL DESPLIEGUE (2026-10-05) — el backend ya está público

La VM `157.56.9.42` (hostname `bucaratransit`) estaba **virgen**: sin Docker, sin `/opt/bus_unab`, solo
`sshd` escuchando. Se montó todo desde cero. Verificado desde internet:

| Comprobación | Resultado |
|---|---|
| `https://bucaratransit.duckdns.org/admin/login` | **200** (curl sin `-k`, certificado válido y confiable) |
| `/empresa/login` | 200 |
| `/api/v1/branding/bucaratransit` | 200, ETag + `Cache-Control: max-age=300, public` |
| `logo_url` | webp real; **sirve 200 `image/webp` 44.744 B** |
| 9 poses de la mascota | presentes; **sirven 200 `image/webp`** (p. ej. saludo 95.030 B) |
| `/api/v1/*` sin token y con `Accept: application/json` | **401** (correcto) |
| Contenedores | 6/6 *Up*, app y mysql *healthy* |
| Renovación del certificado | cron `/etc/cron.d/bucaratransit-certbot`, diario 03:00 |

**Lo que se instaló:** Docker 29.8.2 + Compose v5.6.0, git, swap de 2 GB (la VM tiene 3,8 GB y el build
lo necesita), repo clonado en `/opt/bus_unab`, `.env.production` generado con secretos aleatorios
(permisos 600), imágenes construidas, 4 tenants demo sembrados, contraseñas de los 14 usuarios rotadas a
aleatorias, y el certificado Let's Encrypt emitido (vence 2027-01-03).

## 8.1 Bug corregido — el bloque HTTPS de nginx no resolvía el upstream

El `server` del 443 en `docker/nginx/default.conf` no declaraba `resolver 127.0.0.11`. Como el
`fastcgi_pass` usa una variable (`$upstream app:9000`) para resolución diferida, nginx no podía resolver
el nombre del contenedor y **todo el lado HTTPS devolvía 502**:

```
[error] no resolver defined to resolve app, server: bucaratransit.duckdns.org
```

Es un bug que nunca se había visto porque el 443 jamás estuvo abierto. Corregido en el repo y aplicado
en el servidor. **Falta commitearlo.**

## 8.2 Bug abierto (menor) — 500 en vez de 401 sin cabecera `Accept`

`/api/v1/*` protegido, pedido por un cliente que no manda `Accept: application/json` (un navegador, un
monitor de uptime), devuelve **500** con `Route [login] not defined` en vez de 401: Laravel intenta
redirigir a una ruta `login` web que en esta API no existe. **La app móvil no se ve afectada** — Ktor
añade el header solo por tener `ContentNegotiation` con `json()` — pero un API no debería responder 500.

Arreglo de una línea en `bootstrap/app.php` (Laravel 11):

```php
->withExceptions(function (Exceptions $exceptions) {
    $exceptions->shouldRenderJsonWhen(fn ($request) => $request->is('api/*'));
})
```

Requiere reconstruir la imagen (el código va horneado en ella).

## 8.3 Credenciales de los paneles

Las contraseñas NO se escribieron en este documento ni en el chat. Están en el servidor:

```
ssh azureuser@157.56.9.42
cat /opt/bus_unab/CREDENCIALES.txt
```

Trae `superadmin@demo.test` (para `/admin`) y `admin.bucaratransit@demo.test` (para `/empresa`).
**Guardalas y borrá ese archivo después.**

## 8.4 Advertencias de seguridad que quedan

- `AdminSeeder` crea `admin@unab.edu.co` con la contraseña `Admin2024*` **escrita en un repo público**.
  No se corrió `DatabaseSeeder`, así que esa cuenta **no existe** en el servidor — pero no hay que
  ejecutarlo nunca en producción sin cambiar esa clave antes.
- Los 14 usuarios demo siguen existiendo con correos adivinables (`admin.bucaratransit@demo.test`). Sus
  claves ya son aleatorias, pero conviene decidir si se borran los tenants que no se usan (`metrobus`,
  `campus`, `logistica`).
- La contraseña de la VM que se pegó en el chat sigue sin rotar.
