# BUCARATRANSIT — Informe Integral del Sistema

> **Materia:** Backend
> **Universidad Autónoma de Bucaramanga (UNAB)**
> **Proyecto:** BUCARATRANSIT (antes "Bus UNAB" / "VibraBus")
>
> **Equipo de desarrollo:**
>
> | Nombre | Código |
> |---|---|
> | José Alejandro Téllez Prada | U00060479 |
> | Marcos Valera Daza | U00177223 |
> | Jaime Alejandro Vega Barbosa | U00178766 |
>
> **Historia de una línea:** el proyecto inició como *Bus UNAB*, un rastreador de buses universitario, y evolucionó hasta convertirse en *BUCARATRANSIT*, un SaaS de movilidad urbana multi-tenant para la ciudad de Bucaramanga.

---

## Tabla de contenidos

1. [Descripción del software (300 palabras)](#1-descripción-del-software-300-palabras)
2. [Resumen ejecutivo](#2-resumen-ejecutivo)
3. [Historia y evolución del proyecto](#3-historia-y-evolución-del-proyecto)
4. [Problema y oportunidad](#4-problema-y-oportunidad)
5. [Visión, misión y objetivos](#5-visión-misión-y-objetivos)
6. [Arquitectura general del sistema](#6-arquitectura-general-del-sistema)
7. [Backend: Laravel (API REST + paneles Filament)](#7-backend-laravel-api-rest--paneles-filament)
8. [Frontend: app móvil Kotlin Multiplatform](#8-frontend-app-móvil-kotlin-multiplatform)
9. [Modelo de datos](#9-modelo-de-datos)
10. [Flujos de negocio](#10-flujos-de-negocio)
11. [Seguridad y diseño defensivo](#11-seguridad-y-diseño-defensivo)
12. [Infraestructura y despliegue](#12-infraestructura-y-despliegue)
13. [Roadmap y fases del pivote](#13-roadmap-y-fases-del-pivote)
14. [Pruebas y verificación](#14-pruebas-y-verificación)
15. [Rendimiento y resiliencia (Chaos Engineering)](#15-rendimiento-y-resiliencia-chaos-engineering)
16. [Marca y rebranding](#16-marca-y-rebranding)
17. [Participación y roles del equipo](#17-participación-y-roles-del-equipo)
18. [Instalación y puesta en marcha](#18-instalación-y-puesta-en-marcha)
19. [Conclusiones](#19-conclusiones)
20. [Trabajo futuro](#20-trabajo-futuro)
21. [Glosario](#21-glosario)

---

## 1. Descripción del software (300 palabras)

BUCARATRANSIT es una plataforma de movilidad urbana tipo *Software as a Service* (SaaS) para Bucaramanga, Colombia. Su propósito es unificar a las empresas transportadoras de buses de la ciudad en un solo ecosistema digital, eliminando el efectivo, la evasión y la fragmentación de rutas. El proyecto nació en la materia de Backend como *Bus UNAB* (alias *VibraBus*): un rastreador de buses universitario que mostraba en tiempo real la posición de los buses de la UNAB y su hora estimada de llegada, con notificaciones push. Con el tiempo, ese prototipo académico evolucionó hacia un producto comercial de ciudad, bajo la marca definitiva BUCARATRANSIT.

El sistema se compone de dos grandes piezas. La primera es un **backend Laravel 12** (PHP 8.2) que expone una API REST bajo `/api/v1`, protegida con Laravel Sanctum y límites de peticiones (rate limiting) por endpoint. Aporta autenticación con Google y con correo/contraseña; el rastreo GPS de la flota con ETA en tiempo real; el catálogo de buses, paradas, rutas y puntos de interés; notificaciones push mediante Firebase Cloud Messaging y colas asíncronas; y el corazón del pivote: una **wallet prepago** por pasajero con *ledger* contable de doble entrada, tarifas por transportadora y **códigos QR dinámicos firmados** (HMAC-SHA256) de un solo uso y expiración de 60 segundos. El backend también incluye dos paneles de administración construidos con Filament: un panel maestro de **Super Admin** (`/admin`) y un **panel por transportadora** (`/empresa`) con aislamiento multi-tenant.

La segunda pieza es una **app móvil Kotlin Multiplatform** (Android e iOS) escrita con Compose Multiplatform. En modo pasajero permite ver el mapa, buscar rutas y paradas, seguir un bus con su ETA, consultar el saldo de la wallet, recargar y mostrar el QR dinámico para abordar. En modo conductor convierte el teléfono en un terminal de punto de venta (mPOS): escanea el QR del pasajero y cobra el pasaje en milisegundos, controlando además ocupación y llegadas a parada.

La infraestructura es containerizada (Nginx, PHP-FPM, worker de colas, scheduler, MySQL y Redis) y está preparada para desplegarse en Google Cloud, con un diseño pensado desde su origen para la resiliencia: degradación elegante, patrones de *fallback*, *bulkhead*, cache-aside y procesamiento asíncrono que se validan mediante pruebas de *Chaos Engineering*.

---

## 2. Resumen ejecutivo

BUCARATRANSIT dejó de ser un rastreador de buses universitarios para convertirse en el "motor de movilidad" de Bucaramanga. La plataforma reúne cuatro tipos de usuario claramente separados:

| Rol | Quién es | Qué ve / hace |
|---|---|---|
| **Super Admin** | El equipo operador de la plataforma | Control maestro: transportadoras, tarifas globales, wallets, suscripciones SaaS, soporte y auditoría. Panel `/admin`. |
| **Admin de transportadora** | Gerente o administrador de una empresa de buses | Gestiona solo su tenant: flota, rutas, paradas, conductores, tarifas y recaudo. Panel `/empresa`. |
| **Conductor** | Chofer de una transportadora | Modo conductor en la app, su teléfono es el terminal de cobro (mPOS): escanea QR, cobra el pasaje, reporta llegadas y ocupación. |
| **Pasajero** | Cualquier usuario de la ciudad | Busca rutas, consulta ETA, recarga su wallet, muestra el QR dinámico y paga a bordo. |

El modelo de negocio es SaaS: las transportadoras se suscriben a la plataforma, y a cambio reciben gestión digital de su operación (flota, rutas, recaudo conciliable, datos de demanda) mientras los pasajeros pagan con una billetera prepago. El cobro se hace **a bordo**, bajo supervisión directa del conductor, lo que hace la evasión prácticamente imposible y elimina la necesidad de estaciones o molinetes.

---

## 3. Historia y evolución del proyecto

### 3.1 Fase 1 — Bus UNAB / VibraBus (punto de partida académico)

El proyecto comenzó en la materia de Backend como un sistema de rastreo en tiempo real para los buses universitarios de la UNAB. El problema era simple y real: **los estudiantes no sabían dónde estaba el bus ni cuándo llegaría a su parada**. La primera versión fue una API REST containerizada que conectaba el GPS real de los buses con una app móvil, incluyendo notificaciones push cuando el bus se acercaba.

Características de la fase 1:

- Backend Laravel con un solo panel Filament (`/admin`) pensado únicamente para la UNAB.
- Roles únicos: `admin`, `driver` y `student`.
- Login con Google restringido al dominio institucional `@unab.edu.co`.
- Base de datos *single-tenant* (sin empresas, sin dinero).
- La app móvil (Kotlin Multiplatform) mostraba el mapa, el tracking bus→parada con ETA y un QR básico que servía de credencial de abordaje (no de pago).
- Infraestructura Docker con seis servicios: nginx, app (PHP-FPM), worker de colas, scheduler, MySQL y Redis.
- Entrega académica con demo de **Chaos Engineering** (pruebas de resiliencia ante caídas de nginx, GPS externo, Google OAuth y ataques de fuerza bruta).

### 3.2 Fase 2 — El pivote: de la universidad a la ciudad

El equipo identificó que el mismo motor tecnológico podía resolver un problema mucho mayor: la crisis de movilidad de Bucaramanga. La ciudad tiene una red extensa de buses tradicionales e informales, pero hay fragmentación de operadores, desconocimiento de rutas (sobre todo en los jóvenes) y una dependencia del dinero en efectivo que vuelve el sistema ineficiente. Nació así el pivote:

- **De** *Bus UNAB* (nicho universitario) **a** *BUCARATRANSIT* (SaaS multi-tenant de ciudad).
- **De** un único operador (la UNAB) **a** N transportadoras como tenants aislados.
- **De** la validación QR sin dinero **a** wallet prepago y cobro a bordo.
- **De** un solo panel Filament **a** panel Super Admin + panel por transportadora.
- **De** proyecto académico **a** modelo de negocio SaaS por suscripción.

### 3.3 Línea de tiempo sintética

| Fecha clave | Hito |
|---|---|
| Abril 2026 | Migraciones base: buses, paradas, rutas, users, roles driver, Google auth, POI, device tokens, bus_requests. |
| Mayo 2026 | Waypoints de ruta; motor de rutas/ETA listo. |
| Jun–Jul 2026 | Entrega universitaria original de Bus UNAB con Chaos Engineering. |
| 08 Sep 2026 | Auditoría formal del código (AUDIT.md) y diseño del pivote multi-tenant. |
| Sep 2026 | Fases F0–F3: tenancy, wallets+ledger, QR de pago, paneles Filament Supervisor/tenant, mPOS. |
| 14 Sep 2026 | F2/F3 compilan y pasan smoke API; pendiente E2E en dispositivo. |
| 16 Sep 2026 | Nombre de marca definitivo confirmado: **BUCARATRANSIT**; rebranding visible completado. |

---

## 4. Problema y oportunidad

### 4.1 Problema

Bucaramanga sufre una crisis de movilidad: exceso de vehículos particulares y ausencia de un sistema de transporte masivo estructurado generan trancones paralizantes. Existe una red amplia de buses tradicionales e informales, pero:

- **Fragmentación:** cada transportadora opera por su cuenta; no hay una vista unificada de rutas ni tarifas.
- **Desconocimiento:** los ciudadanos (especialmente los jóvenes) no saben qué ruta tomar ni a qué paradero ir.
- **Efectivo:** la economía del bus mueve millones en monedas y billetes, costosa de operar y sin datos.
- **Evasión:** los sistemas de estaciones como otras ciudades sufren altas tasas de "colados".

### 4.2 Oportunidad y ventaja competitiva

A diferencia de sistemas que exigen inversiones masivas en infraestructura (estaciones, molinetes, validadores), BUCARATRANSIT aprovecha **la infraestructura que ya existe: el bus mismo**.

- **0 gasto en infraestructura externa:** el control de acceso ocurre al subir al vehículo.
- **Evasión ≈ 0:** el pago se hace a bordo, bajo la supervisión directa del conductor y con confirmación instantánea desde la wallet.
- **Flujo de caja prepago:** las recargas entran antes de viajar; la plataforma administra el saldo retenido (float) y las transportadoras reciben recaudo digital conciliable.
- **Datos donde antes había efectivo:** cada abordaje es un registro; se conoce la demanda por ruta y hora.
- **Reuso de activos:** el pivote reaprovechó el login Google, el tracking GPS con ETA, el escáner QR, el modo conductor y la app móvil ya construidos.

---

## 5. Visión, misión y objetivos

- **Misión:** unificar a todas las empresas transportadoras de la ciudad en una sola plataforma, fomentar el uso del transporte público masivo como solución al tráfico y modernizar una industria olvidada: *"Haremos el transporte público a nuestra manera: ágil, digital y sin fricciones."*
- **Visión:** devolver a los bumangueses el tiempo que pierden en el tráfico, reducir la huella de carbono incentivando el transporte colectivo y digitalizar una economía que hoy mueve millones en efectivo.
- **Objetivo técnico general:** construir un backend SaaS multi-tenant robusto (tenancy, wallets, pagos por QR) que conviva sin romper con la app móvil ya publicada, y que cada fase termine verificable con pruebas.

---

## 6. Arquitectura general del sistema

```
┌───────────────────────── CLIENTES ─────────────────────────┐
│  App Pasajero (KMP/Compose)   App Conductor = mPOS (KMP)   │
│  Panel Super Admin (Filament)  Panel Transportadora        │
└──────────────────────────────┬─────────────────────────────┘
                               │ HTTPS (Bearer token)
┌──────────────────────────────▼─────────────────────────────┐
│                BACKEND Laravel 12 + Sanctum                 │
│  · AuthController (Google / password / register → token)    │
│  · Middlewares: EnsureTenantScope · role: · throttle:       │
│  · Dominio: Bus · Stop · RouteStop · BusRequest · POI       │
│  · WalletService (crédito/débito atómico, ledger)           │
│  · QrPaymentService (emisión HMAC / cobro one-time)         │
│  · SyncGPS (gpsmobile.co) · Notifications (FCM + colas)     │
└──────────────────────────────┬─────────────────────────────┘
                               │
                    (MySQL 8 prod / SQLite dev)
                   Shared schema + transportadora_id
```

La defensa multi-tenant se apoya en **tres capas**:

1. **GlobalScope Eloquent** (`BelongsToTenant` → `GlobalTenantScope`): filtra automáticamente las consultas por `transportadora_id` cuando hay un tenant en contexto.
2. **Políticas de autorización** por acción dentro de Filament.
3. **Middleware `EnsureTenantScope`** en la API: valida que el recurso solicitado de la ruta pertenezca al tenant (si no, **404 silencioso**, nunca 403) y activa el contexto de tenant para toda la request.

---

## 7. Backend: Laravel (API REST + paneles Filament)

### 7.1 Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | PHP ^8.2 |
| Framework | Laravel ^12.0 |
| Autenticación de API | Laravel Sanctum ^4.3 (tokens Bearer) |
| Paneles de administración | Filament ^3.3 |
| Autenticación Google | google/auth ^1.50 |
| Base de datos | MySQL 8 (producción) / SQLite (desarrollo) |
| Colas y cache | Redis 7 |

### 7.2 API REST — rutas principales (`/api/v1`)

**Autenticación (públicas, throttle 10/min):**

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/google` | Login con Google (idToken). |
| POST | `/auth/login` | Login con email/contraseña. |
| POST | `/auth/register` | Autoregistro de pasajeros (H3, pivote). |
| POST | `/auth/logout` | Cierra sesión revocando el token. |
| GET | `/auth/me` | Perfil del usuario autenticado. |

**Buses y paradas (autenticadas):**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/buses` | Lista los buses (scoped por tenant). |
| GET | `/buses/catalog` | Catálogo para el modo conductor. |
| GET | `/buses/{plate}` | Detalle de un bus. |
| GET | `/buses/{plate}/stops` | Paradas de la ruta del bus. |
| GET | `/buses/{plate}/route` | Geometría de la ruta. |
| GET | `/buses/{plate}/occupancy` | Aforo/ocupación actual. |
| POST | `/buses/{plate}/arrived` · `/approaching` | Reportes del conductor. |
| POST | `/buses/{plate}/occupancy` | Actualiza estado lleno/libre. |
| POST | `/buses/{plate}/location` | Actualiza la posición GPS del bus. |
| GET | `/stops` | Paradas de la ciudad. |

**Wallet y pago QR (pivote):**

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/wallet` | Saldo e historial de la wallet. |
| POST | `/wallet/recharge-mock` | Recarga simulada (solo local/testing; el proveedor real es F4). |
| POST | `/wallet/qr/issue` | El pasajero emite su QR dinámico de pago. |
| POST | `/qr/pay` | El conductor cobra escaneando (rol admin/driver). |
| POST | `/qr/validate` | Validación legada de abordaje (convive sin romperse). |
| POST | `/admin/broadcast` | Broadcast global de notificaciones (admin). |
| GET | `/poi` | Puntos de interés. |

Todas las rutas protegidas exigen token de Sanctum, y los endpoints sensibles tienen **rate limiting diferenciado** (p. ej. `throttle:60,1`, `throttle:10,1`, `throttle:5,1` en device tokens, `throttle:20,1` en wallet).

### 7.3 Servicios de dominio

| Servicio | Responsabilidad |
|---|---|
| `AuthService` | Login Google + password + registro. |
| `WalletService` | Único módulo que escribe saldos: crédito, débito y ajuste sobre ledger de doble entrada, con locks, idempotencia por `reference` y guarda `balance >= monto`. |
| `QrPaymentService` | Emisión de tokens efímeros (selector aleatorio + firma HMAC-SHA256 con `APP_KEY`) y cobro atómico de un solo uso. |
| `BusRequestService` | Solicitudes de bus / notificaciones de llegada y aproximación. |
| `GpsMobileService` | Integración con proveedor externo gpsmobile.co; falla→retorna null y el controlador responde 503 amigable (degradación elegante). |
| `NotificationService` | Notificaciones push vía colas asíncronas (worker Redis). |

### 7.4 Paneles Filament

- **Panel Super Admin** (`/admin`): gate `super_admin` (con compatibilidad del rol históórico `admin`). Resources globales: `TransportadoraResource` (CRUD de tenants), `WalletResource` (solo lectura + ajustes), `BusRequestResource`, `BusResource`, `StopResource`, `UserResource`, `PointOfInterestResource`, páginas `BusMap` y `BroadcastPage`, widgets de estadísticas y calendario.
- **Panel de la Transportadora** (`/empresa`): gate `tenant_admin` (requiere `transportadora_id` no nulo). Solo ve: `BusResource`, `StopResource`, `FareResource` (scoped)` y `UserResource` (crea solo conductores y pasajeros). El middleware `EnsureTenantScope` activa el aislamiento duro durante toda la petición, y al crear registros el hook `creating` auto-asigna `transportadora_id`.

### 7.5 Roles

El enum de rol evolucionó de `('student','admin','driver')` a:

```
pasajero | student (alias legado) | admin (alias legado) |
super_admin | tenant_admin | driver
```

- `pasajero` ≡ `student` (alias de lectura, sin romper la app instalada).
- `admin` ≡ `super_admin` (backward compatibility).
- `User::assignableRoles(panel)` y `User::defaultRoleForPanel(panel)` controlan qué roles crea cada panel (un gerente de empresa no puede crear admins).

---

## 8. Frontend: app móvil Kotlin Multiplatform

### 8.1 Stack

- **Kotlin Multiplatform + Compose Multiplatform** (un solo código para Android e iOS).
- **Voyager** (navegación), **Koin** (inyección de dependencias), **Ktor Client** (HTTP), **kotlinx.serialization**, **Coil** (imágenes), **zxing** (escáner QR), **Google Maps / Credential Manager**.
- Identificador técnico congelado por continuidad operativa: `com.vibra.bus` (no es branding visible).

### 8.2 Pantallas principales

| Pantalla | Rol |
|---|---|
| `SplashScreen`, `LoginScreen` | Splash, login con Google o credenciales (mascota búho histórica). |
| `HomeScreen`, `MainScreen` | Mapa con estilo minimalista, bottom navigation. |
| `StopSelectionScreen`, `StopsListScreen` | Selección de parada. |
| `WaitingBusScreen` | Seguimiento del bus: polilínea, ETA y distancia cada 3 s, alerta "¡Tu bus está llegando!". |
| `MyQRScreen` | QR dinámico del pasajero con cuenta regresiva ("Expira en Ns"), avatar y nombre. |
| `QRScannerScreen` | Escáner del conductor con dos modos: `ACCESS` (valida reserva legada) y `PAY` (cobra wallet). Acepta código manual como fallback sin cámara. |
| `DriverModeScreen` | Dashboard del conductor: selección de ruta, "EN RUTA", stats (pasajeros/paradas/estado), confirmación de llegadas, toggle de bus lleno, fuente de ubicación (GPS del bus o teléfono). |
| `WalletScreen` | Saldo prepago, recarga y operaciones de billetera. |
| `MyTripsScreen`, `ProfileScreen`, `NotificationsScreen` | Viajes, perfil y notificaciones. |

### 8.3 Capa de datos

- `data/api/*` — clientes HTTP (AuthApi, BusApi, StopApi, WalletApi, RequestApi, PoiApi, GoogleMapsApi) con `SafeCall.kt`.
- `data/repository/*` — repositorios (AuthRepository, BusRepository, StopRepository, WalletRepository, RequestRepository, PoiRepository).
- `data/model/*` — DTOs serializables (AuthModels, BusModels, StopModels, WalletModels, RequestModels, PoiModels, DirectionsModels).
- `presentation/viewmodel/*` — ViewModels con `StateFlow` (AuthViewModel, HomeViewModel, StopSelectionViewModel, WaitingBusViewModel, MyQRViewModel, QRScannerViewModel, DriverModeViewModel, WalletViewModel, NotificationsViewModel, MyTripsViewModel, ProfileViewModel).
- `util/*` — QRScanner, LocationManager, GoogleSignInManager, MapUtils/MapStyle, AppSettings, ApiResult.

### 8.4 Flujo técnico del cobro en la app

En modo conductor (`REFERENCIA QRScannerViewModel`):

1. El conductor pulsa "Escanear QR de pasajero" → pantalla `QRScannerScreen`.
2. Se escanea el contenido `selector.firma` (32 hex + "." + 64 hex).
3. En modo PAY se llama `walletRepository.pay(qr)` → `POST /api/v1/qr/pay`.
4. El servidor valida firma HMAC, vigencia (TTL 60 s) y un solo uso, debita la wallet del pasajero y acredita el recaudo a la transportadora → el teléfono muestra pantalla verde/roja con el monto.
5. Un reintento del mismo QR responde `422 already_used` sin efecto en el saldo.
6. La app tiene *cooldown* de escaneo (2,5 s) y acepta entrada manual del QR si la cámara falla.

---

## 9. Modelo de datos

### 9.1 Tablas de tenancy

- **`transportadoras`** — la raíz del tenant: `nombre`, `slug` (UNIQUE), `razon_social`, `nit`, `plan` (default `trial`), `activo`, soft deletes.

### 9.2 Tablas de negocio (con `transportadora_id` cuando son propiedad de la empresa)

| Tabla | Contenido |
|---|---|
| `users` | Usuarios con `role`; `transportadora_id` NULL = Super Admin o pasajero de ciudad. |
| `buses` | Flota; `UNIQUE(transportadora_id, plate)`; `external_vehicle_id` para GPS; capacidad y aforo. |
| `route_stops` | Paradas por ruta con orden y `estimated_minutes` (base del ETA). |
| `bus_route_waypoints` | Geometría de las rutas (polilíneas). |
| `bus_requests` | Solicitudes de bus y abordajes (capa operativa). |
| `stops`, `points_of_interest` | Datos **compartidos de ciudad** (sin tenant; se conectan vía route_stops/waypoints). |
| `device_tokens` | Tokens FCM para push. |

### 9.3 Tablas financieras (el pivote)

- **`wallets`** — 1 por usuario: `balance_centavos BIGINT` (siempre ≥ 0), `version` (lock optimista), `estado` (activa/congelada). El saldo es una **proyección cacheada** del ledger.
- **`wallet_transactions`** — **LEDGER append-only**: todo movimiento es un asiento `credit` / `debit` / `ajuste` con `monto_centavos`, `balance_after`, `reference` UNIQUE (idempotencia) y `contraparte`. Prohibido editar o borrar asientos (el modelo lanza excepción en `updating`/`deleting`).
- **`fares`** — tarifas por transportadora (o global con `transportadora_id` NULL) con vigencia temporal.
- **`qr_payment_tokens`** — token de pago: `token_hash` (SHA-256 del selector, nunca el selector en claro), `monto_snapshot_centavos` (congelado al emitir), `expires_at` (TTL 60 s), `used_at` (one-time: la primera marca gana). El QR visible es `selector.firmaHMAC`.

**Regla de oro financiera:** centavos como `BIGINT` enteros, nunca `DOUBLE`/`FLOAT` (aritmética exacta para cuadrar `SUM(credit) − SUM(debit) == balance`). La presentación divide por 100; el dominio jamás multiplica ni divide.

---

## 10. Flujos de negocio

### 10.1 Recarga de wallet

1. El pasajero recarga saldo (hoy `wallet/recharge-mock` para desarrollo; en F4 un proveedor de pagos real mediante webhooks idempotentes).
2. `WalletService::credit()` en una transacción: lock → actualiza saldo (`version+1`) → inserta asiento `credit` con `balance_after`.
3. El `reference` único hace que reintentos de red nunca dupliquen la recarga.

### 10.2 Emisión del QR de pago

1. El pasajero abre "Mi QR" → `POST /wallet/qr/issue`.
2. El servidor resuelve la tarifa vigente (del bus/ruta si se conoce, o tarifa global; fallback $1.850 COP) y **congela el monto** en `monto_snapshot_centavos`.
3. Genera un selector aleatorio de 16 bytes (jamás se persiste en claro) y firma HMAC-SHA256 con `APP_KEY`.
4. Guarda el hash SHA-256 del selector con TTL 60 s y devuelve `selector.firma` para el QR.
5. Saldo insuficiente → 422 "recarga para continuar" (no se emite el token).

### 10.3 Cobro a bordo (mPOS)

1. El conductor escanea el QR → `POST /qr/pay` (rol admin/driver, `throttle:60,1`).
2. En una **única transacción DB**:
   a. Recalcula el HMAC → firma inválida = 422;
   b. reclama el token con `UPDATE ... WHERE used_at IS NULL` (solo el primer escaneo afecta 1 fila; si `affected != 1` → 422 `already_used`);
   c. debita la wallet del pasajero (lock + guarda de saldo) y registra la contraparte `transportadora:{id}` / `plataforma`;
   d. si algo falla, todo se revierte (la marca de uso se deshace).
3. Respuesta al conductor: `ok | monto | saldo restante enmascarado` (nunca expone el historial del pasajero).
4. Latencia objetivo del servidor: < 150 ms.

### 10.4 Flujo del pasajero (núcleo de experiencia)

Home (mapa) → seleccionar parada → buscar buses cercanos → elegir bus → seguir su aproximación con ETA cada 3 segundos → alerta "¡Tu bus está llegando!" → mostrar el QR al conductor → el conductor escanea y el pasaje se descuenta en milisegundos.

### 10.5 Flujo del conductor

Entrar a modo conductor → seleccionar ruta asignada → reportar "EN RUTA" → confirmar llegada/aproximación de paradas → actualizar ocupación (LLENO/Disponible) → escanear QR para cobrar → ver stats del turno.

---

## 11. Seguridad y diseño defensivo

| Amenaza | Defensa implementada |
|---|---|
| Fuerza bruta / abuso | Rate limiting por endpoint (`throttle:10,1` en login/registro, 60/min en lectura, 20/min en wallet). |
| QR falsificado/editado | Firma HMAC-SHA256 con `APP_KEY`; el servidor recalcula y compara con `hash_equals`. |
| QR reutilizado (replay) | `used_at` con UPDATE condicional: la primera marca gana; reintento → 422 `already_used` sin efecto. |
| QR expirado | `expires_at` **server-authoritative** (TTL 60 s); el `ts` del cliente no manda. |
| Doble débito por reintento de red | `reference` UNIQUE en el ledger: reintento devuelve el asiento existente. |
| Saldo insuficiente | Débito atómico con guarda `balance >= monto` dentro de la transacción + `CHECK(balance >= 0)` (rollback total si falla). |
| Fuga cross-tenant | GlobalScope Eloquent + middleware `EnsureTenantScope` + 404 silencioso (no revela que el recurso existe) + `TenantIsolationTest`. |
| Manipulación del ledger | Modelo append-only: bloquea `update`/`delete` de asientos; correcciones = asiento inverso `ajuste`. |
| Acceso no autorizado | Tokens Sanctum, middleware `role:`, gates de panel Filament por panel. |
| Descubrimiento de recursos | 404 (no 403) ante recursos de otro tenant. |

**Patrones de resiliencia ya demostrados** (entrega universitaria de Bus UNAB, validada con Chaos Monkey):

- **Graceful Degradation** — el GPS externo cae → el endpoint responde 503 amigable, no un crash.
- **Fallback Pattern** — Google OAuth cae → el login email/contraseña sigue funcionando.
- **Bulkhead** — contenedores aislados: la caída de MySQL no mata a Nginx.
- **Cache-Aside** — Redis cachea GPS (30 s) y rutas (24 h); sin Redis la app degrada, no explota.
- **Async Processing** — el worker procesa notificaciones; si cae, la API sigue respondiendo.
- **Rate Limiting** — el middleware throttle protege cada endpoint.

---

## 12. Infraestructura y despliegue

### 12.1 Docker Compose (producción)

| Servicio | Rol |
|---|---|
| `nginx` | Reverse proxy, único punto de entrada HTTP (TLS con certbot). |
| `app` | API Laravel en PHP-FPM, lógica de negocio. |
| `worker` | Procesador de colas Redis (`queue:work`), notificaciones push. |
| `scheduler` | Tareas programadas (cron de Laravel cada 60 s). |
| `mysql` | MySQL 8: persistencia multi-tenant. |
| `redis` | Redis 7: cache de GPS + cola de notificaciones. |
| `certbot` | Emisión/renovación de certificados SSL (perfil `ssl`). |

El archivo `GoogleCloudMigration_Tutorial.md` documenta el despliegue en **Google Cloud (App Engine PHP 8.2, F1 / 1 instancia)** del backend Laravel.

---

## 13. Roadmap y fases del pivote

```
F0 Foundations multi-tenant  → HECHA
F1 Wallet + QR de pago + paneles  → HECHA
F2 Wallet + QR dinámico en la app (pasajero, UI Kotlin) → HECHA (compila + smoke API; falta E2E en dispositivo)
F3 mPOS conductor en la app (cobro por cámara → POST /qr/pay) → HECHA (compila + smoke API; falta E2E cámara)
F4 NFC + recarga con proveedor real → BLOQUEADA (API keys + marca final)
F5 Lanzamiento piloto (1-2 transportadoras reales + deploy) → PENDIENTE
```

Regla de oro: cada fase termina verificable con `php artisan test` y `migrate:fresh --seed`, y nunca rompe la app KMP publicada (contratos `/api/v1` intactos, package `com.vibra.bus` congelado).

---

## 14. Pruebas y verificación

### 14.1 Suite de pruebas del backend (Pest/PHPUnit)

| Suite | Cubre |
|---|---|
| `AuthTest` | Login Google/password, logout, me. |
| `RegisterPasajeroTest` | Autoregistro de pasajeros, validaciones. |
| `BusTest` / `StopTest` | CRUD y consultas de buses/paradas. |
| `BusRequestTest` | Solicitudes, llegadas, aforo. |
| `WalletServiceTest` | Crédito/débito/ajuste, idempotencia, saldo insuficiente. |
| `QrPaymentTest` | Emisión, cobro, replay, expiración, firma inválida. |
| `TransportadoraModelTest` | Modelo tenant raíz. |
| `TenantIsolationTest` | Aislamiento cross-tenant: usuario de tenant A recibe 404 silencioso en recursos del tenant B. |

**Números reportados:** al cierre del pivote, 88 tests pasando / 0 fallos (257 aserciones), `migrate:fresh --seed` verde en SQLite y MySQL, y más de 40 rutas registradas entre API y paneles.

### 14.2 Validaciones de calidad por fase

- `php -l` (lint) sobre cada archivo tocado.
- `migrate:fresh --seed` en SQLite **y** MySQL.
- `route:list` con los dos paneles Filament registrados y arranque limpio.
- Prueba viva del flujo QR: recarga mock 500.000 c → `qr/issue` (snapshot 185.000, TTL 60 s) → `qr/pay` → "Abordaje cobrado" → replay rechazado.

---

## 15. Rendimiento y resiliencia (Chaos Engineering)

En la entrega universitaria de "Bus UNAB" se construyó `chaos_monkey.sh`, un script que apaga cada servicio del stack para demostrar el comportamiento ante fallos en vivo. Las pruebas estrella de la presentación fueron:

- **Nginx caído:** sin reverse proxy la app es 100% inaccesible → solución de arquitectura: múltiples instancias + load balancer.
- **GPS externo caído:** el endpoint responde 503 con mensaje amigable (`GpsMobileService` detecta el fallo y retorna `null` → el controlador produce una respuesta HTTP correcta).
- **Google OAuth caído:** la app no muere; el login con email/contraseña toma el relevo (fallback).
- **Rate limiting:** 20 requests seguidas a login; desde la request 11 el servidor responde HTTP 429 (protección sin firewall externo).

Conclusión de esa demo: *"La resiliencia no es un feature — es una decisión de diseño desde el día uno."* Cada contenedor es reemplazable e independiente, y `docker compose restart <servicio>` recupera cualquier falla en segundos.

---

## 16. Marca y rebranding

La documentación de pivote vive en la carpeta `Bucaramanga_Mobility_Rebranding/` y registra, entre otros:

- **`NAMING_DECISION.md`** — decisión de marca cerrada: **BUCARATRANSIT** (confirmada 2026-09-16).
- **`CONCEPTO.md`** — concepto y pilares: multi-tenant, wallet prepago, QR dinámico→NFC, conductor=mPOS, motor de enrutamiento.
- **`BRAND.md`** — posicionamiento, tono y voz, matriz de lo que cambia vs lo congelado.
- **`ARCHITECTURE.md`** — ADRs (tenancy shared-schema, roles, dos paneles, ledger, QR HMAC).
- **`ROADMAP_TECNICO.md`** — fases F0–F5.
- **`MATRIZ_BRANDING.md`** — qué se renombra (UI, `android:label`, `brandName`, docs) y qué queda **congelado** (`com.vibra.bus`, `google-services.json`, `vibra-bus.jks`, `GOOGLE_SERVER_CLIENT_ID`, contratos `/api/v1`).

> ⚠️ El **package técnico sigue siendo `com.vibra.bus`** (identificador de Play Store, no branding visible). Renombrarlo es un proyecto aparte de migración (Firebase + Play + OAuth).

---

## 17. Participación y roles del equipo

| Integrante | Código | Áreas observadas en el repositorio |
|---|---|---|
| **José Alejandro Téllez Prada** | U00060479 | Diseño del pivote, arquitectura multi-tenant y documentación técnica del rebranding. |
| **Marcos Valera Daza** | U00177223 | Backend Laravel: wallet/ledger, tokens QR de pago, paneles Filament y pruebas de aislamiento. |
| **Jaime Alejandro Vega Barbosa** | U00178766 | App móvil Kotlin Multiplatform: pantallas, viewmodels, repositorios, escáner QR y modo conductor. |

> El trabajo se desarrolló de manera colaborativa sobre un único repositorio: las decisiones de arquitectura (ADRs), la auditoría de código y la matriz de pivote documentadas en `Bucaramanga_Mobility_Rebranding/` son el registro conjunto del equipo para la materia.

---

## 18. Instalación y puesta en marcha

### 18.1 Backend (Laravel)

```bash
cd bus_unab
composer install
cp .env.example .env
php artisan key:generate
# edita .env (MySQL o usa SQLite por defecto para pruebas locales)
php artisan migrate --seed
php artisan serve --host=0.0.0.0 --port=8000
```

La API queda en `http://TU_IP_LOCAL:8000/api/v1`.

### 18.2 Frontend (KMP)

1. Abre `frontend/` en Android Studio (Hedgehog+; JDK 17).
2. Ajusta `BASE_URL_ANDROID` / `BASE_URL_IOS` en `frontend/composeApp/build.gradle.kts`.
3. Coloca tu `google-services.json` en `frontend/composeApp/`.
4. Configura `GOOGLE_SERVER_CLIENT_ID` (Firebase Console → Authentication → Google → ID de cliente web).
5. Sync, Make Project, Run.

### 18.3 Credenciales de demo (después de `db:seed`)

| Rol | Email | Contraseña |
|---|---|---|
| Administrador (Super Admin) | `admin@unab.edu.co` | `password` |
| Conductor | `driver@unab.edu.co` | `password` |
| Pasajero | `student@unab.edu.co` | `password` |

> ⚠️ Cambiar estas credenciales antes de producción.

### 18.4 Paneles web

- Super Admin: `http://localhost:8000/admin`
- Transportadora (tenants): `http://localhost:8000/empresa`

---

## 19. Conclusiones

1. **El pivote fue incremental y sin romper la app publicada:** se conservaron los activos del proyecto universitario (auth, tracking, ETA, escáner, aforo) y se construyó encima el SaaS (tenancy, dinero, QR de pago, dobles paneles).
2. **El multi-tenancy se resolvió con shared-schema + `transportadora_id` + tres capas de defensa** (GlobalScope, policies, middleware), verificado con tests de aislamiento cross-tenant.
3. **El dinero se trató como infraestructura crítica :** centavos enteros, ledger de doble entrada append-only, idempotencia por `reference`, locks y porcentajes de concurrencia con pruebas dedicadas.
4. **El QR dejó de ser una credencial de abordaje y pasó a ser un instrumento de pago seguro:** token HMAC efímero, de un solo uso y con monto congelado; la misma superficie servirá para NFC en el futuro.
5. **La app móvil convierte el teléfono del conductor en un terminal de punto de venta (mPOS):** cobro a bordo, evasión ≈ 0 y recaudo digital conciliable.
6. **La base de datos y la infraestructura están listas para un piloto real**, pendiente únicamente del proveedor de pagos y de las decisiones de despliegue final.

---

## 20. Trabajo futuro

- **F2/F3 E2E en dispositivo:** completar la validación de cámara/QR en equipos físicos y emuladores.
- **F4 — NFC y recarga con proveedor real:** webhooks idempotentes → `WalletService::credit('recarga')`; suscripciones SaaS automáticas; columna `credential_type('qr','nfc')`.
- **Cobro offline del conductor** con cola local y sincronización posterior (grace period).
- **Lanzamiento piloto con 1-2 transportadoras reales** y despliegue en `bus.finsik.site` (hoy corre el código anterior).
- **Migración del package técnico** (`com.vibra.bus` → nuevo `applicationId`) si se decide estrategia de app nueva, con migración de Firebase/Play/OAuth.
- **Consolidación de recaudo** por tenant y cuadre del invariante del ledger con un job `wallet:reconcile`.

---

## 21. Glosario

| Término | Definición |
|---|---|
| **Tenant / Transportadora** | Empresa de buses onbordada como cliente SaaS; dueña aislada de flota, rutas, conductores y recaudo. |
| **Super Admin** | Equipo operador de la plataforma; control maestro del ecosistema. |
| **mPOS** | *Mobile Point of Sale*: el teléfono del conductor como terminal de cobro. |
| **Wallet** | Billetera prepago del pasajero, saldo en centavos enteros. |
| **Ledger de doble entrada** | Libro contable append-only donde todo movimiento genera un asiento con `balance_after`. |
| **Abordaje** | Acto de subir al bus y pagar; el conductor escanea el QR y debita la wallet. |
| **Tarifa (Fare)** | Precio del pasaje, scoped por transportadora y ruta, con vigencia temporal. |
| **ETA** | Estimated Time of Arrival: tiempo estimado de llegada del bus a la parada. |
| **QR token** | Credencial de pago firmada (HMAC-SHA256), TTL 60 s, un solo uso. |
| **Evasión ≈ 0** | Objetivo de negocio: al cobrar a bordo bajo supervisión, "colados" casi imposibles. |

---

*Documento elaborado para la materia de Backend — UNAB. Proyecto "BUCARATRANSIT", evolución del sistema "Bus UNAB / VibraBus".*