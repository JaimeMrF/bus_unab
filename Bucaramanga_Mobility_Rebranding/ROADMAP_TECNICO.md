# 🛣️ ROADMAP TÉCNICO — Migración de "Bus UNAB" a "Bucaramanga Mobility" (SaaS multi-tenant)

> Documento de planificación (M2 · S2.1.3). Insumos: `AUDITORIA.md` (gaps con evidencia) y `DISEÑO_DB.md` (schema objetivo).
> Regla de oro: **cada fase termina verificable** con `cd bus_unab && php artisan test` y `php artisan migrate:fresh --seed` (SQLite, baseline del repo), y no rompe la app KMP publicada (`/api/v1` existentes, frontend `com.vibra.bus` congelado como package).

---

## Visión de fases

```
F0 Foundations multi-tenant
 └─▶ F1 Wallet + QR de pago (backend)
      └─▶ F2 mPOS Conductor (app actual + endpoints)
           └─▶ F3 Paneles Filament por tenant + Super Admin
                 └─▶ F4 NFC + recarga con proveedor real  [BLOQUEADO: API keys — fuera del scope]
```

> **NOMENCLATURA CANONICA** (unica vigente desde 2026-09-09; coincide con el
> plan de mision `.opencode/todo.md`). El diagrama de arriba conservo la
> numeracion antigua de escritura; la tabla manda:
>
> | Fase | Contenido | Estado (2026-09-09) |
> |------|-----------|------------------------|
> | F0 | Foundations multi-tenant (transportadoras, scopes, migraciones) | HECHA |
> | F1 | Paneles con datos reales: /admin SaaS + /empresa tenant (el backend de wallet/QR/roles tambien cayo aqui) | HECHA |
> | F2 | Wallet + QR dinamico EN LA APP del pasajero (UI Kotlin sobre endpoints existentes) | PENDIENTE |
> | F3 | mPOS conductor en la app (cobro camara -> POST /qr/pay, endpoint ya operativo) | PENDIENTE |
> | F4 | NFC + recarga con proveedor real de pagos | BLOQUEADA (API keys + marca final) |
> | F5 | Lanzamiento piloto: 1-2 transportadoras reales + deploy (bus.finsik.site corre codigo viejo) | PENDIENTE |
>
> Total: **6 fases, F0 a F5**. F2 y F3 pueden ir en paralelo (misma pantalla
> compartida de scan) y no dependen de ninguna API externa.

---

## F0 — Foundations multi-tenant (M3 · T3.1 del plan)

**Objetivo:** que TODO dato de dominio nazca sabiendo a qué transportadora pertenece, sin romper API ni frontend.

**Alcance:**
- Migración `create_transportadoras_table` + modelo `Transportadora` (slug, activo, softDeletes) según `DISEÑO_DB.md §2`.
- Migración `add_tenant_to_core_tables`: `transportadora_id` en `buses`, `route_stops`, `bus_route_waypoints`, `bus_requests`, `users` (ver `DISEÑO_DB.md §3`); **swap de únicos de `buses.plate`/`external_vehicle_id`** (hoy globales: `create_buses_table.php:18-19`).
- Trait `BelongsToTenant` (GlobalScope + auto-`creating`) + escape `scopeWithoutTenant` para seeders/admin.
- `TransportadoraSeeder` + backfill idempotente de los 3 buses/usuarios existentes a la tenant demo "Metropolitana UNAB".
- `TenantIsolationTest`: usuario tenant A recibe 404 silencioso en recursos de tenant B (API actual, `routes/api.php:56-101`).
- Quitar el candado de dominio `@unab.edu.co` de `AuthController.php:36-37` → email verificado de ANY dominio para pasajero (Super Admin/tenant staff siguen gestionados por panel).

**Criterios de salida:** `migrate:fresh --seed` verde en SQLite **y** MySQL; suite antigua sin regresión; todo bus/stop/ruta/usuario de demo con tenant o NULL justificado; grep sin queries `DB::table(` sobre tablas scoped.

## F1 — Wallet + QR de pago (M3 · T3.2 del plan)

**Objetivo:** mover dinero con un ledger auditable y tokens QR de un solo uso.

**Alcance:**
- Tablas `wallets`, `wallet_transactions`, `fares`, `qr_tokens` (`DISEÑO_DB.md §4-§7`) + modelos append-only.
- `WalletService`: `credit()/debit()` en `DB::transaction` + `lockForUpdate` (MySQL) con guarda condicional `balance >= ?`; idempotencia por `idempotency_key`; excepción `InsufficientFunds`.
- `QrPaymentService`: emisión HMAC (`hash_hmac('sha256', user|issued|rotacion, QR_SIGNING_SECRET)`, TTL 60 s, rotación 30–60 s) + `pay()` que verifica firma → vigencia → `used_at IS NULL` → tarifa vigente del bus → débito pasajero + crédito transportadora en UN ledger group.
- Endpoints: `POST /api/v1/wallet/recharge` (MOCK interno — el proveedor real es F4), `GET /wallet`, `GET /wallet/transactions`, `POST /api/v1/qr/token` (pasajero, throttle por usuario), `POST /api/v1/qr/pay` (rol conductor; **convive con el `POST /qr/validate` legado**, `routes/api.php:91-92`).
- Tests: doble débito concurrente, replay mismo token, token expirado, saldo insuficiente, cross-tenant (pasajero wallet ciudad en bus tenant B), cuadre del invariante `SUM(creditos)-SUM(debitos)==balance`.

**Criterios de salida:** suite completa verde; **el invariante del ledger pasa en un test de reconciliación**; `/qr/validate` legacy sigue respondiendo igual (la app instalada no se rompe).

## F2 — mPOS Conductor en la app actual

**Objetivo:** el teléfono del conductor cobra el pasaje en segundos (ventaja "evasión ≈ 0", `PROMPT_IA.md`).

**Alcance (frontend KMP, solo UI/ViewModel — package `com.vibra.bus` congelado):**
- Reaprovechar el escáner ya integrado (`QRScannerViewModel.kt` consume `repository.validateQr`, `:49-64`) → nuevo modo "Cobro": parsea `BMOB1.{user}.{issued}.{rot}.{sig}`, llama `POST /qr/pay`, pantalla verde/roja con monto y saldo restante del pasajero (parcial: ***123).
- Reintentos: el botón re-pulsa con la **misma** `idempotency_key` (nunca duplica cargo). El chequeo de expiración del cliente (`QRScannerViewModel.kt:56`) se mantiene pero el servidor manda.
- Sin modo offline en esta fase (ver `CUELLOS_BOTELLA.md §2` para el diseño de grace-period futuro).
- MyQRScreen pasajero: mostrar saldo wallet + QR rotativo (reemplaza payload JSON actual de `MyQRViewModel.kt:31-38`).

**Criterios de salida:** demo E2E documentada: recarga mock → QR en pantalla del pasajero → escaneo conductor → `POST /qr/pay` → ledger cuadra → segunda lectura del mismo QR = rechazado; `:composeApp:assembleDebug` compila.

## F3 — Paneles Filament: Super Admin + Tenant (`filament/filament ^3.3`, soporta `ModelMultitenancy`)

**Alcance:**
- `AdminPanelProvider` (path `/admin`) → panel **Super Admin**: gate a `super_admin` (alias backward-compat de `admin` actual, `User.php:40-58` → `canAccessPanel`), + resources globales nuevos: `TransportadoraResource`, `SaasSubscriptionResource`, `WalletResource` (solo lectura + `ajuste_admin` vía WalletService).
- Nuevo `TenantPanelProvider` (path `/empresa`) con tenancy de Filament (`HasTenants`/`identifyUsing(user.transportadora_id)`): Resources Bus/Stop/Route/Fare/Driver/Reportes **scoped por tenant**; gate `admin_transportadora`.
- Branding visible en ambos paneles (`brandName('Bucaramanga Mobility')` reemplaza `'Bus UNAB — Admin'`, `AdminPanelProvider.php:32`) — coordenado con M4.

**Criterios de salida:** admin de tenant A NO ve ni edita datos de tenant B (test de policies + smoke manual con 2 tenants seedeados); Super Admin ve todo; login Filament por rol redirige al panel correcto.

## F4 — NFC + recarga con proveedor real ⛔ BLOQUEADO (PENDIENTE: API keys)

**No ejecutable en esta misión** (regla #2 del plan: API keys = pendiente del usuario). Prepara el terreno:
- Superficie de token idéntica QR/NFC: el mismo `qr_tokens.token_hash` servirá de credential-URI; agregar `qr_tokens.credential_type ENUM('qr','nfc')` solo en esta fase.
- Pasarela de pagos: adapter `PaymentProvider` + webhooks idempotentes → `WalletService::credit('recarga')`; `saas_subscriptions` pasa de manual a débito automático.
- Hardware NFC (lectores / Android HostCardEmu): evaluar tras piloto.

**Criterios de salida:** definidos cuando existan proveedor contratado y claves; hoy: **NADA se implementa aquí** salvo las tablas ya soportan la extensión.

---

## Tabla de dependencias y riesgos

| Fase | Depende de | Riesgo principal | Prob. | Impacto | Mitigación |
|---|---|---|---|---|---|
| F0 | — | Romper app publicada al cambiar únicos/roles | Med | Alto | Columnas NULL primero, backfill idempotente, `/api/v1` sin cambios de contrato, tests de regresión en 2 drivers BD |
| F1 | F0 | Race conditions en saldos / duplicación de cargos | Med | Crítico | Ledger append-only + locks + `idempotency_key` + tests de concurrencia (ver `CUELLOS_BOTELLA.md §4`) |
| F2 | F1 | Conectividad a bordo mala → cobros fallidos | Alto | Medio | Timeout corto + retry idempotente; grace offline diferido a post-piloto |
| F3 | F0,F1 | Fuga cross-tenant en paneles por olvido de scope | Med | Crítico | GlobalScope del modelo (defensa primaria) + policies + `TenantIsolationTest` obligatorio |
| F4 | F2 + decisión usuario | Bloqueo externo (API keys) | Cierto | — | Fuera de scope; diseño de tablas ya lo anticipa |

**Milestone de piloto:** al cerrar F3, el demo "mañana" (`Demo_roadmap.md`) actualizable a: pasajero UNAB(any email) + pasajero Transportadora B, cada empresa con su panel y recaudo visible.

> Coherencia con `.opencode/todo.md` vigente: F0→M3/T3.1, F1→M3/T3.2, F2→(T3.2 endpoints + UI F2 de roadmap), F3→M3/T3.3, F4→notas "PENDIENTE" del plan. Esta nomenclatura F0–F4 es la del `todo.md` histórico del brief; el Commander mantiene el mapeo en `work-log.md`.
