# 🏗️ ARCHITECTURE — Componentes, límites de tenant y ADRs

> M2 · S2.3.1. Insumos: `AUDIT.md` (estado actual) + visión (`PROMPT_IA.md`). Schemas: `DB_SCHEMA.md` / `DB_SCHEMA_PAYMENTS.md`. Secuencias: `FLOWS.md`.

## 1. Diagrama de componentes (objetivo)

```mermaid
flowchart LR
    subgraph Clientes
      P["App Pasajero (KMP Compose)\npassenger: mapa/rutas/ETA · wallet · QR dinámico"]
      D["App Conductor = mPOS (KMP)\nescáner QR zxing · cobro · aforo"]
      SA["Panel Super Admin\nFilament /admin"]
      TA["Panel Transportadora\nFilament /empresa (ModelMultitenancy)"]
    end
    subgraph Backend["Laravel 12 + Sanctum 4.3 (API /api/v1)"]
      AUTH["AuthController (Google/password)\n→ token Sanctum"]
      MWS["EnsureTenantScope\n+ role: + throttle:"]
      CORE["Dominio: Bus / Stop / RouteStop /\nBusRequest / POI (trait BelongsToTenant)"]
      WALLET["WalletService\n(crédito/débito atómico, ledger)"]
      QRP["QrPaymentService\n(issue HMAC / pay one-time)"]
      GPS["SyncGPS (gpsmobile.co)\nposición de flota"]
    end
    DB[("MySQL 8 prod / SQLite dev\ncompartido + transportadora_id")]
    PROV["Proveedor de pagos externo\n⚠️ PENDIENTE: API keys (fuera de misión)"]

    P -->|HTTPS Bearer| AUTH --> DB
    P --> MWS --> CORE --> DB
    P -->|POST /qr/token| QRP --> DB
    D -->|POST /qr/pay| MWS --> QRP --> WALLET --> DB
    SA -->|Filament sin scope| DB
    TA -->|Filament con tenancy| DB
    GPS --> DB
    PROV -.->|webhooks (F4, bloqueado)| WALLET
```

## 2. Límites de tenant (quién ve qué)

| Dato | Pasajero | Conductor | Admin tenant | Super Admin |
|---|---|---|---|---|
| Buses/paradas/rutas | visibles según servicio (paradas compartidas de ciudad sí) | solo su bus/tenant | CRUD de SU tenant | todo |
| Requests/abordajes | los propios | de SU bus | de SU tenant | todo |
| Wallet | la propia (saldo + historial) | NO ve saldo ajeno (solo resultado del cobro: ok/monto, enmascarado) | NO (salvo auditoría que defina el plan) | todo, lectura + `ajuste` |
| Tarifas | las vigentes de la ruta | la tarifa cobrada | CRUD de SU tenant | todo |
| Recaudo del tenant | — | totales de SU turno | SU tenant | todo |
| Otras transportadoras | — | — | **404 silencioso** | sí |

Defensa en 3 capas: **GlobalScope Eloquent** (`BelongsToTenant`: primarias queries), **policies** (autorizaciones por acción), **middleware `EnsureTenantScope`** en API (validación del recurso solicitado; `AUDIT.md §3.5`). Ninguna capa basta sola (ver `BOTTLENECKS.md §1`).

## 3. ADR-001 — Multi-tenancy: shared database + shared schema + columna `transportadora_id`
- **Estado:** Aceptado · **Contexto:** decenas de tenants previstos (transportadoras de Bucaramanga), miles de pasajeros; equipo pequeño; hosting actual modesto (`app.yaml`: F1, 1 instancia; docker-compose 6 servicios).
- **Opciones:** (a) DB por tenant → aislamiento físico pero: migraciones ×N, backups ×N, conexiones dinámicas, sin paquete de tenancy en `composer.json` y la regla del plan prohíbe introducir `stancl/tenancy` sin decisión explícita; (b) schema por tenant → misma complejidad operativa menor ganancia; (c) **shared schema + columna tenant + GlobalScope** → elegida.
- **Consecuencias:** riesgo de fuga por olvido de scope (mitigado §2 + tests cross-tenant); queries siempre indexadas por `transportadora_id` (`DB_SCHEMA.md`); aislamiento lógico, no físico — aceptable en este dominio (datos de flota no son PII médica/legal).

## 4. ADR-002 — Roles: `super_admin` | `tenant_admin` | `driver` | `pasajero` (renombrar `student` sin romper)
- **Contexto:** enum actual `('student','admin','driver')` (`AUDIT.md §1.1`) con `student` = pasajero universitario; la app publicada y el middleware `role:` envían/esperan esos strings.
- **Decisión:** migrar el enum ampliándolo a `('pasajero','student','admin','super_admin','tenant_admin','driver')`, donde `pasajero`≡`student` (alias) y `admin` queda deprecated≡`super_admin` (backward compat). API responde rol canónico nuevo pero ACEPTA los viejos; UI llama a los usuarios "pasajero". `User::isAdmin()` mantiene semítica (true para admin/super_admin/tenant_admin según panel, ver S3.3.1 del plan).
- **Alternativas descartadas:** tabla `roles` + pivote (sobrediseño para 4 roles); forzar rename limpio (rompería apps instaladas).
- **Consecuencia:** el tenant de cada usuario vive SOLO en `users.transportadora_id` (NULL = Super Admin o pasajero de ciudad).

## 5. ADR-003 — Dos paneles Filament (no uno con conmutador)
- Panel **Super Admin** `/admin` (gate `super_admin`): `TransportadoraResource`, `FareResource` global, `WalletResource` lectura+ajuste, `SaasSubscriptionResource`, todos los resources legacy sin scope.
- Panel **Transportadora** `/empresa` (gate `tenant_admin`): `ModelMultitenancy` de Filament 3.3 (`HasTenants`, tenant identificado por `User::transportadora`) sobre Bus/Stop/Route/User(driver)/Reportes scoped + `FareResource` propio.
- **Descartado:** un solo panel con filtros condicionales — mezclaría navigation/permisos y duplicaría la lógica de gates en cada resource.

## 6. ADR-004 — Pago con dinero: ledger de doble entrada append-only (ver `DB_SCHEMA_PAYMENTS.md`)
Saldo = cache de proyección; toda mutación = asiento(s) + `balance_after` + `idempotency_key`. Prohibido `UPDATE` de saldo fuera del servicio. Test de invariante `SUM(creditos)-SUM(debitos)==balance`.

## 7. ADR-005 — QR: token HMAC efímero one-time; superficie reutilizable para NFC
Mismo modelo de credencial servirá a NFC futuro (`FLOWS.md §5`). Mientras tanto `/qr/validate` legacy convive (no se toca su contrato — `AUDIT.md §3.3`).

## 8. Trazabilidad con el plan (todo.md M3)
`BelongsToTenant`+migraciones→S3.1.x · Wallet/QrPayment services→S3.2.x · paneles+`/empresa`→S3.3.1-2 · `EnsureTenantScope`+tests aislamiento→S3.3.3.
