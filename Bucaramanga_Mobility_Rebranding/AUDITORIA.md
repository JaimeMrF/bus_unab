# 📋 AUDITORÍA — Estado actual del código vs Visión "BUCARATRANSIT"

> **Misión:** SaaS multi-tenant de movilidad para Bucaramanga (transportadoras = tenants, wallet prepago, QR dinámico/NFC, conductor como mPOS, Super Admin).
> **Fuente de verdad de la visión:** `Bucaramanga_Mobility_Rebranding/PROMPT_IA.md` y `README.md` de esta carpeta.
> **Fecha de auditoría:** 2026-09-08 · Baseline: rama actual pre-pivot.
> Toda afirmación incluye evidencia real `archivo:línea` del repositorio. Stack verificado en `bus_unab/composer.json:9-13`: **PHP ^8.2, Laravel ^12.0, Filament ^3.3, Sanctum ^4.3**.

---

## 1. Inventario del sistema HOY (baseline)

### 1.1 Backend Laravel (`bus_unab/`)

| Área | Existente | Evidencia |
|---|---|---|
| Modelos Eloquent | `Bus`, `BusRequest`, `BusRouteWaypoint`, `DeviceToken`, `PointOfInterest`, `RouteStop`, `Stop`, `User` (8) | `bus_unab/app/Models/` |
| Migraciones | 16 archivos, de `0001_01_01_000000_create_users_table.php` a `2026_05_02_222457_create_bus_route_waypoints_table.php` | `bus_unab/database/migrations/` |
| Roles | **UN SOLO** enum `role` con `'student','admin','driver'`, default `student` | `2026_04_17_400000_add_driver_role_to_users.php:19-21`, `2026_04_30_053309_fix_sqlite_roles.php:11-13` |
| Helpers de rol | `isAdmin()` / `isDriver()` / `isStudent()` comparan string plano, sin alcance por empresa | `app/Models/User.php:40-53` |
| Acceso panel | `canAccessPanel()` = solo `isAdmin()` | `app/Models/User.php:56-59` |
| Panel Filament | **1 único panel** `id('admin')->path('admin')`, brand `Bus UNAB — Admin`, sin tenancy | `app/Providers/Filament/AdminPanelProvider.php:27-35` |
| API | `/api/v1/*` con Sanctum + throttle por endpoint | `routes/api.php:26-101` |
| Auth | Login Google (`POST /v1/auth/google`) + login/password (`POST /v1/auth/login`) | `routes/api.php:31-36` |
| GPS externo | `buses.external_vehicle_id` → integración con `gpsmobile.co` | `2026_04_17_100000_create_buses_table.php:15` |
| Seeders | `AdminSeeder`, `BusSeeder`, `DatabaseSeeder`, `DemoUserSeeder`, `Route2Seeder`, `StopSeeder` (sin transportadoras) | `bus_unab/database/seeders/` |

### 1.2 Frontend KMP (`frontend/composeApp/src/commonMain/kotlin/com/vibra/bus/`)

| Capability | Existente | Evidencia |
|---|---|---|
| Generador QR pasajero | `MyQRViewModel` arma `QRPayload` JSON con `ts` y lo rota en pantalla | `presentation/viewmodel/MyQRViewModel.kt:31-38` |
| Escáner QR conductor | `QRScannerViewModel` parsea payload, checa expiración 60 s **del lado cliente** y llama `validateQr` | `presentation/viewmodel/QRScannerViewModel.kt:49-64` |
| Pantallas QR | `MyQRScreen.kt`, `QRScannerScreen.kt`, `QRCodeImage.kt`, `util/QRScanner.kt` (zxing/cámara ya integrados) | `presentation/screens/`, `util/` |

---

## 2. QUÉ FALTA (gap vs visión nueva)

Confirmado por inspección exhaustiva de `bus_unab/database/migrations/` (16 archivos) y `bus_unab/app/Models/` (8 modelos): **no existe absolutamente ninguna de estas tablas/modelos**:

| # | Pieza faltante | Necesidad en la visión |
|---|---|---|
| G1 | **`transportadoras` (tenant raíz)** | Cada empresa de buses = inquilino con datos y panel aislados (`PROMPT_IA.md §1`). No hay columna `transportadora_id` en NINGUNA tabla actual. |
| G2 | **`wallets`** | Billetera prepago por pasajero (`PROMPT_IA.md §2`). No hay ningún modelo/tabla de saldo. |
| G3 | **`wallet_transactions` (ledger de doble entrada)** | Descuentos de pasaje y recargas auditables y a prueba de race conditions. |
| G4 | **`fares` (tarifas por transportadora)** | El cobro a bordo necesita un precio formal por empresa/ruta; hoy no hay noción de dinero en el sistema. |
| G5 | **`qr_tokens` firmados (one-time, cortos)** | El QR actual NO es una credencial de pago (ver §3.3). Para wallet se requieren tokens HMAC con expiración y marca de uso. |
| G6 | **Paneles separados Super Admin vs Tenant** | `AdminPanelProvider.php` define un solo panel para `role=admin` (`User.php:56-59`). Falta panel `/empresa` con tenancy de Filament v3 (`HasTenants`/`ModelMultitenancy`, soportado en `filament/filament ^3.3`). |
| G7 | **Aislamiento de datos por tenant (API)** | `routes/api.php` solo aplica `role:` y `throttle:` (p. ej. `:67-76`, `:91-92`); ningún middleware resuelve/scopea por transportadora → hoy TODOS los usuarios autenticados ven TODOS los buses/paradas (`GET /v1/buses` `routes/api.php:58`, `GET /v1/stops` `:80`, `GET /v1/poi` `:99`). |
| G8 | **`saas_subscriptions`** | Modelo de negocio SaaS (`PROMPT_IA.md §1`): plan/estado de cobro a transportadoras. No existe. |
| G9 | **Endpoints de wallet y pago** | No hay `/wallet/*` ni `/qr/pay` en `routes/api.php`. |
| G10 | **Rol `super_admin` vs `admin_transportadora`** | El enum actual (`add_driver_role_to_users.php:19-21`) no distingue entre el equipo plataforma y el admin de una empresa. |
| G11 | **Soporte NFC** | Cero referencias en backend ni frontend; es fase posterior del roadmap. |

---

## 3. QUÉ REFACTORIZAR (existe pero no sirve tal cual)

### 3.1 Modelo de usuario y roles (monolito single-tenant)
- **Evidencia:** `User.php:40-53` — `isAdmin()` es booleano global: un admin de "Transportadora X" tendrías acceso total al panel de TODAS (`AdminPanelProvider.php:29-31`, `User.php:56-59`).
- **Refactor:** `users.transportadora_id` (nullable: NULL = Super Admin de plataforma), split de `admin` → `super_admin` / `admin_transportadora` manteniendo el string viejo como alias de compatibilidad para no romper la app ni el middleware `role:` (`routes/api.php:67-76`, `:91-92`).

### 3.2 Identificadores globales que deben volverse por-tenant
- **Evidencia:** `create_buses_table.php:19-20` — `plate` y `external_vehicle_id` son `unique()` **globales**. Con dos transportadoras que repitan convenciones de placa/ruta, la migración de datos fallaría.
- **Refactor:** único compuesto `(transportadora_id, plate)`; igual para slugs de ruta/nombres de parada donde aplique (`stops`, `route_stops`: `2026_04_17_200001_create_stops_table.php`, `2026_04_17_200002_create_route_stops_table.php`).

### 3.3 QR actual = credencial de abordaje, NO de pago
- **Evidencia:** `QrController.php:16-58` — valida `request_id/user_id/bus_id/stop_id/ts` contra la tabla `bus_requests`; sin firma HMAC, sin wallet, sin tarifa. El `ts` (`:27-32`) viene del cliente (spoofeable con reloj alterado) y la antiforja depende solo de adivinar IDs enteros (`:34`, `:41-45`). El anti-replay es el status `boarded` de la solicitud (`:47-49`), no un token.
- **Frontend:** `MyQRViewModel.kt:31-38` emite JSON plano; `QRScannerViewModel.kt:56` hace el chequeo de expiración en el cliente (debe duplicarse en servidor).
- **Refactor:** nuevo `POST /api/v1/qr/pay` con tokens HMAC one-time (`qr_tokens`), manteniendo `/qr/validate` vigente durante la transición (compatibilidad con la app instalada). El flujo de solicitud de abordaje (`bus_requests`, `create_bus_requests_table.php:16-26`) se conserva como capa "operativa" separada de la capa "pago".

### 3.4 Filament: un panel → dos paneles
- **Evidencia:** `AdminPanelProvider.php:23-61` — único provider en `app/Providers/Filament/` (verificado con `ls`), `discoverResources` apunta a un mismo namespace (`:36`), sin `->tenant()`.
- **Refactor:** panel `super` (todo el ecosistema) + panel `empresa` con `ModelMultitenancy` de Filament v3 y recursos scoped; los Resources actuales (Bus, Stop, User, BusRequest, POI) se duplican/reasignan por panel con scopes de tenant.

### 3.5 Rutas API sin alcance de negocio multi-tenant
- **Evidencia:** `routes/api.php:26-101` — grupos por recurso con `throttle`/`role` pero sin ninguna noción de empresa; `POST /v1/qr/validate` con `throttle:60,1` (`:91-92`) para un endpoint que pronto moverá dinero → insuficiente como única defensa.
- **Refactor:** middleware `EnsureTenantScope` + policies; throttles diferenciados (generar token 6/min usuario, pagar 10-15/min conductor).

### 3.6 Config/branding
- **Evidencia:** `.env.example:1` `APP_NAME=Laravel`; panel con `brandName('Bus UNAB — Admin')` (`AdminPanelProvider.php:32`).
- **Refactor:** visible branding a marca definitiva "BUCARATRANSIT" — gestionado por M4 del plan, **no** en este doc.

---

## 4. QUÉ SE CONSERVA INTACTO (activos reaprovechables) ✅

| Activo | Por qué sobrevive al pivot | Evidencia |
|---|---|---|
| **Motor de enrutamiento/paradas/ETA** | La visión exige "dónde estás → a dónde vas" (`PROMPT_IA.md §2`); `stops` + `route_stops` (orden/`estimated_minutes`) + `bus_route_waypoints` ya lo soportan. Solo se le añade `transportadora_id`. | `Stop.php` (Haversine `distanceTo/isNearby`), `Bus.php:33-46` (`stops`, `routeWaypoints`), `2026_05_02_222457_create_bus_route_waypoints_table.php` |
| **Ocupación/aforo (capacidad, niveles low→full)** | Diferenciador directo de la app ("bus lleno" ya en modo conductor). | `Bus.php:11-13,57-83` (`capacity`, `occupancyPercentage`, `occupancyLevel`), `2026_04_17_200000_add_capacity_to_buses_table.php` |
| **Auth Google + Sanctum** | Base de identidad de pasajeros intacta; se amplía, no se reemplaza. | `AuthController` vía `routes/api.php:31-36`, `personal_access_tokens` `2026_04_18_004740_create_personal_access_tokens_table.php` |
| **Tracking en vivo + GPS externo** | Integración `gpsmobile.co` sigue siendo la fuente de posición por bus; mapping bus→transportadora permite filtrar flota por empresa. | `buses.external_vehicle_id` `create_buses_table.php:15`, `BusController::updateDriverLocation` `routes/api.php:73-76` |
| **Push notifications (FCM)** | Notificaciones de cercanía del bus siguen vigentes por pasajero. | `DeviceToken.php`, `NotificationController` `routes/api.php:50-53`, `:95-96` |
| **Escáner QR del frontend** | El conductor ya escanea: solo cambia el payload procesado y el endpoint. Activo mPOS listo. | `util/QRScanner.kt`, `QRScannerScreen.kt`, `QRScannerViewModel.kt:49-64` |
| **Infra docker/GCP** | `docker-compose.yml` (nginx, app php82, colas, scheduler) y `app.yaml` (App Engine) soportan el mismo stack con tablas nuevas. | `bus_unab/docker-compose.yml`, `bus_unab/app.yaml` |

---

## 5. Semáforo de esfuerzo estimado

| Workstream | Esfuerzo | Riesgo | Motivo |
|---|---|---|---|
| Tenancy en BD (G1, índices §3.2) | Medio | Medio | Backfill de datos existentes + únicos compuestos |
| Roles + paneles Filament (G6, G10, §3.4) | Medio | Medio | Dos paneles conviviendo; Filament 3.3 soporta tenancy nativamente |
| Wallet + ledger (G2-G4, G9) | Alto | **Alto** | Dinero real: requiere doble entrada, locks, idempotencia, tests de concurrencia |
| QR de pago firmado (G5, §3.3) | Alto | **Alto** | Seguridad criptográfica + coexistencia con `/qr/validate` legacy |
| Aislamiento API (G7, §3.5) | Medio | **Alto si se omite** | Una ruta sin scope = fuga de datos cross-tenant |
| NFC / pagos externos (G11) | — | — | **FUERA DEL SCOPE actual: pendiente API keys/proveedores (decisión del usuario)** |

**Conclusión:** el repo es una base sólida single-tenant (routing, tracking, auth, aforo, escáner QR) sobre la que faltan los tres pilares del pivote: **tenancy, dinero (wallet/ledger/tarifas) y tokens de pago firmados**. Nada del núcleo operativo actual necesita reescribirse; casi todo se amplía con una columna `transportadora_id` y servicios nuevos. Ver `DISEÑO_DB.md` para el schema propuesto y `ROADMAP_TECNICO.md` para la secuencia.
