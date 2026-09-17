# 🔍 AUDIT — Estado actual del código vs Visión "BUCARATRANSIT"

> **Misión:** SaaS multi-tenant de movilidad para Bucaramanga (transportadoras = tenants, wallet prepago, QR dinámico/NFC, conductor como mPOS, Super Admin).
> **Fuentes de la visión:** `Bucaramanga_Mobility_Rebranding/PROMPT_IA.md` + `README.md` (esta carpeta).
> Fecha de auditoría: 2026-09-08 · baseline pre-pivot. Toda afirmación lleva evidencia `archivo:línea` verificada.
> Stack verificado (`bus_unab/composer.json:9-13`): **PHP ^8.2 · Laravel ^12.0 · Filament ^3.3 · Sanctum ^4.3**.
> Apoyo: matriz de conocimiento graphify en `graphify-out/` (`.graphify_extract.json`: 4.483 nodos / 15.907 aristas — 4.435 AST + 48 semánticos; grafo completo en construcción, ver work-log M5).

---

## 1. Inventario del sistema HOY (baseline)

### 1.1 Backend Laravel (`bus_unab/`)

| Área | Existente | Evidencia |
|---|---|---|
| Modelos Eloquent | `Bus`, `BusRequest`, `BusRouteWaypoint`, `DeviceToken`, `PointOfInterest`, `RouteStop`, `Stop`, `User` (8) | `app/Models/` |
| Migraciones | 16 archivos (`0001_01_01_000000_create_users_table.php` → `2026_05_02_222457_create_bus_route_waypoints_table.php`) | `database/migrations/` |
| Roles | **UN SOLO** enum `role` con `'student','admin','driver'`, default `student` | `2026_04_17_400000_add_driver_role_to_users.php:19-21`, `2026_04_30_053309_fix_sqlite_roles.php:11-13` |
| Helpers de rol | `isAdmin()`/`isDriver()`/`isStudent()`: string plano, sin alcance por empresa | `app/Models/User.php:40-53` |
| Acceso panel | `canAccessPanel()` = solo `isAdmin()` | `app/Models/User.php:56-59` |
| Panel Filament | **1 panel único** `id('admin')->path('admin')`, brand `Bus UNAB — Admin`, sin tenancy | `app/Providers/Filament/AdminPanelProvider.php:27-35` |
| API | `/api/v1/*` con Sanctum + throttle por endpoint; cero middleware de tenant | `routes/api.php:26-101` |
| Auth | Login Google + password (`POST /v1/auth/google|login`) | `routes/api.php:31-36` |
| GPS externo | `buses.external_vehicle_id` → integración gpsmobile.co | `2026_04_17_100000_create_buses_table.php:15` |
| Seeders | `AdminSeeder`, `BusSeeder`, `DatabaseSeeder`, `DemoUserSeeder`, `Route2Seeder`, `StopSeeder` — sin transportadoras | `database/seeders/` |

### 1.2 Frontend KMP (`frontend/composeApp/src/commonMain/kotlin/com/vibra/bus/`)

| Capacidade | Existente | Evidencia |
|---|---|---|
| Generador QR pasajero | `QRPayload` JSON con `ts` rotando en pantalla | `presentation/viewmodel/MyQRViewModel.kt:31-38` |
| Escáner QR conductor | parsea payload, expiración 60 s **del lado cliente**, llama `validateQr` | `presentation/viewmodel/QRScannerViewModel.kt:49-64` |
| Pantallas QR | `MyQRScreen.kt`, `QRScannerScreen.kt`, `QRCodeImage.kt`, `util/QRScanner.kt` (zxing/cámara integrados) | `presentation/screens/`, `util/` |

---

## 2. QUÉ FALTA (gap vs visión)

Inspección exhaustiva de `database/migrations/` (16) y `app/Models/` (8): **no existe ninguna de estas piezas**:

| # | Pieza faltante | Necesidad (`PROMPT_IA.md`) |
|---|---|---|
| G1 | **`transportadoras` (tenant raíz)** | Cada empresa = inquilino con datos/panel aislados (§1). Ninguna tabla actual tiene `transportadora_id`. |
| G2 | **`wallets`** | Billetera prepago del pasajero (§2). Cero modelos/tablas de saldo. |
| G3 | **`wallet_transactions` (ledger doble entrada)** | Descuentos y recargas auditables, a prueba de race conditions. |
| G4 | **`fares` (tarifas por tenant)** | El cobro a bordo exige precio formal por empresa/ruta; hoy no hay noción de dinero. |
| G5 | **`qr_payment_tokens` firmados, one-time** | El QR actual NO es credencial de pago (§3.3). |
| G6 | **Paneles separados Super Admin vs Tenant** | Un solo panel para `role=admin` (`User.php:56-59`); falta `/empresa` con tenancy Filament v3 (`ModelMultitenancy`, disponible en `filament/filament ^3.3`). |
| G7 | **Aislamiento cross-tenant en API** | `routes/api.php` solo aplica `role:`/`throttle:` (`:67-76`, `:91-92`); hoy todo usuario autenticado ve TODOS los buses (`:58`), paradas (`:80`) y POI (`:99`). |
| G8 | **`saas_subscriptions`** | Modelo SaaS de cobro a transportadoras (`PROMPT_IA.md §1`). No existe. |
| G9 | **Endpoints wallet/pago** | Sin `/wallet/*` ni `/qr/pay` en `routes/api.php`. |
| G10 | **Roles `super_admin` vs `tenant_admin`** | El enum actual (`add_driver_role_to_users.php:19-21`) no distingue plataforma vs empresa. |
| G11 | **NFC** | Cero referencias en backend/frontend — fase posterior (**PENDIENTE**, fuera del scope actual). |

## 3. QUÉ REFACTORIZAR (existe, no sirve tal cual)

### 3.1 Usuario/roles monolito single-tenant
`User.php:40-53` — `isAdmin()` es booleano global: un admin de "Transportadora X" vería TODO el panel (`AdminPanelProvider.php:29-31`). **Refactor:** `users.transportadora_id` (NULL = Super Admin) + split `admin` → `super_admin`/`tenant_admin`, conservando `admin`/`student` como alias backward-compat para no romper middleware `role:` (`routes/api.php:67-76`) ni la app publicada (student→pasajero solo en semántica/UI; ver `ARCHITECTURE.md §4`).

### 3.2 Identificadores globales que deben ser por-tenant
`create_buses_table.php:18-19` — `plate` y `external_vehicle_id` son `UNIQUE` **globales**: dos transportadoras con placas/rutas homónimas romperían el onboarding. **Refactor:** `UNIQUE(transportadora_id, plate)` (ver `DB_SCHEMA.md §3`).

### 3.3 El QR actual es credencial de abordaje, NO de pago
`QrController.php:16-58` — valida `request_id/user_id/bus_id/stop_id/ts` contra `bus_requests`: sin wallet, sin tarifa, sin firma. El `ts` (`:27-32`) llega del cliente (spoofeable) y la antiforja es "adivinar IDs enteros" (`:34,41-45`); el anti-replay es el `status='boarded'` de la solicitud (`:47-49`), no un token. En frontend, `MyQRViewModel.kt:31-38` emite JSON plano y `QRScannerViewModel.kt:56` caduca en cliente. **Refactor:** nuevo `POST /api/v1/qr/pay` con tokens HMAC one-time; **mantener `/qr/validate` vigente** durante la transición (compatibilidad app instalada). `bus_requests` sigue como capa operativa (solicitud/abordaje), separada de la capa de pago.

### 3.4 Filament: 1 panel → 2 paneles
`AdminPanelProvider.php:23-61` (único provider; `discoverResources` un solo namespace `:36`, sin `->tenant()`). **Refactor:** panel `/admin` Super + panel `/empresa` con tenancy; resources actuales (Bus/Stop/User/BusRequest/POI) reasignados y scoped (`ARCHITECTURE.md §5`).

### 3.5 Rutas API sin alcance de negocio
`routes/api.php:26-101` — sin noción de empresa; `qr/validate` con `throttle:60,1` (`:91-92`) insuficiente como única defensa para un endpoint que moverá dinero. **Refactor:** middleware `EnsureTenantScope` + policies + throttles diferenciados (`BOTTLENECKS.md §5`).

### 3.6 Branding/config
`.env.example:1` `APP_NAME=Laravel`; panel `brandName('Bus UNAB — Admin')` (`AdminPanelProvider.php:32`) → handled by M4 (rebranding visible), no por este doc.

## 4. QUÉ SE CONSERVA INTACTO ✅

| Activo | Por qué sobrevive | Evidencia |
|---|---|---|
| **Motor de enrutamiento/paradas/ETA** | La visión pide "dónde estás → a dónde vas" (`PROMPT_IA.md §2`); `stops`+`route_stops`(orden,`estimated_minutes`)+`bus_route_waypoints` ya lo dan; solo añade `transportadora_id` | `Stop.php` (`distanceTo`/`isNearby` Haversine), `Bus.php:33-46`, `2026_05_02_222457_create_bus_route_waypoints_table.php` |
| **Ocupación/aforo** | Capacidades y niveles low→full diferenciador del mPOS | `Bus.php:57-83`, `2026_04_17_200000_add_capacity_to_buses_table.php` |
| **Auth Google + Sanctum** | Identidad del pasajero se amplía, no reemplaza | `routes/api.php:31-47`, `2026_04_18_004740_create_personal_access_tokens_table.php` |
| **Tracking GPS + FCM push** | Fuente de posición (gpsmobile) y notificaciones cercanas siguen vigentes; mapear bus→tenant filtra la flota por empresa | `create_buses_table.php:15`, `routes/api.php:50-53,73-76` |
| **Escáner QR frontend** | El conductor ya escanea: cambia el payload y el endpoint; activo mPOS listo | `util/QRScanner.kt`, `QRScannerViewModel.kt:49-64` |
| **Infra docker/GCP** | Mismo stack soporta tablas nuevas | `docker-compose.yml` (nginx, php82, worker, scheduler, cron, db), `app.yaml` (App Engine php82) |

## 5. Semáforo de esfuerzo

| Workstream | Esfuerzo | Riesgo | Motivo |
|---|---|---|---|
| Tenancy BD (G1, §3.2) | M | M | Backfill + swap de únicos |
| Roles + paneles (G6,G10,§3.4) | M | M | Dos paneles conviviendo (Filament 3.3 soporta) |
| Wallet+ledger (G2-G4,G9) | A | **A** | Dinero real: locks, idempotencia, tests de concurrencia |
| QR pago firmado (G5,§3.3) | A | **A** | Criptografía + coexistencia con `/qr/validate` |
| Aislamiento API (G7,§3.5) | M | **A si se omite** | Una ruta sin scope = fuga cross-tenant |
| NFC / pagos externos (G11) | — | — | **FUERA DE SCOPE: proveedor de pagos y API keys = PENDIENTE del usuario** |

**Conclusión:** base sólida single-tenant (routing, tracking, auth, aforo, escáner QR) a la que le faltan los tres pilares del pivote: **tenancy, dinero (wallet/ledger/fares) y tokens de pago firmados**. Nada del núcleo operativo se reescribe: casi todo se amplía con `transportadora_id` + servicios nuevos. Detalle: `DB_SCHEMA.md`, `DB_SCHEMA_PAYMENTS.md`, `FLOWS.md`, `ROADMAP_TECNICO.md`.
