# ⚠️ BOTTLENECKS — Riesgos técnicos y recomendaciones

> M2 · S2.4.2 · Versión canónica de `CUELLOS_BOTELLA.md` (borrador previo, mismo contenido). Baseline: Laravel 12 + Sanctum 4.3 + SQLite dev / MySQL prod (ver `AUDIT.md`, `DB_SCHEMA.md`, `DB_SCHEMA_PAYMENTS.md`).

## 1. Seguridad/aislamiento por tenant en BD
**Riesgo:** hoy TODAS las rutas `/api/v1` comparten los datos (`routes/api.php:56-101`); un olvido de scope = fuga cross-tenant (buses, conductores, recaudo de otra empresa).
**Recomendación:**
- Trait `BelongsToTenant` con GlobalScope (`where transportadora_id = ??`) + auto-set en `creating` + escape `scopeWithoutTenant()` solo para seeders/tests/Super Admin.
- Policies Laravel además del scope (defensa en profundidad: el scope filtra queries, la policy autoriza acciones).
- 404 silencioso cross-tenant (no 403, para no revelar existencia del recurso).
- **SQLite vs MySQL:** SQLite no aplica `FOR UPDATE` y su locking es a nivel de archivo; el aislamiento por scope funciona igual en ambos, pero NUNCA confiar en locks para validar en SQLite lo que correrá en MySQL — los tests de concurrencia deben exigir MySQL (CI) y documentar la limitación.
- Auditoría: comando `tenant:audit` que busca filas con `transportadora_id NULL` en tablas NOT NULL-ready.

## 2. Latencia de validación QR a bordo (conectividad celular mala)
**Riesgo:** el bus arranca con pasajero esperando; validación lenta (>2 s) rompe la experiencia y genera cobros dobles por reintentos.
**Presupuesto:** objetivo `< 150 ms` server-side (hash lookup + UPDATE marca uso + asientos ledger), red ~1-3 s típico 4G.
**Recomendación:**
- Token verificable **sin round-trip de decisión de negocio**: firma HMAC server-side embebida (`signature` en el token, ver `DB_SCHEMA_PAYMENTS.md §4`) → el app del conductor puede validar estructura/firma offline y mostrar "pendiente de sync"; la acreditación real se aplica al reconectar.
- TTL corto (60 s) + one-time (`used_at`) → ventana de riesgo offline acotada.
- Reintentos idempotentes: `reference`/`idempotency_key` UNIQUE en el ledger (un escaneo duplicado por mala red no doble-cobra).
- `token_hash` con UNIQUE index → lookup O(log n) sin escaneos; purga de tokens viejos para mantener la tabla pequeña.
- No usar el patrón actual de confianza en `ts` del cliente (`QrController.php:27-32`): server timestamp autoritativo, tolerancia de reloj ±10 s en el escáner.

## 3. Sincronización de saldos multi-dispositivo
**Riesgo:** pasajero con wallet en 2 teléfonos; el cliente cachea saldo y muestra valor viejo (o "inventado").
**Recomendación:**
- **Prohibido que el cliente mantenga saldo autoritativo en memoria** — la wallet es proyección del ledger server-side (`balance_after` + invariante `SUM(creditos)−SUM(debitos) == balance`).
- `version` optimista en `wallets`: toda escritura compara-anda version; conflicto → re-read + re-intent.
- El QR dinámico neutraliza el problema: el conductor no confía en lo que "cree" el pasajero; el server cobra contra el saldo real en el momento de `POST /qr/pay`.
- UI muestra "última actualización" + refresh al abrir MyQR.

## 4. Concurrencia de descuento (race condition: dos cobros, un saldo)
**Riesgo:** dos conductores escanean casi a la vez un pasajero con saldo para UN pasaje → doble débito o saldo negativo.
**Recomendación (canal único de escritura):**
```php
DB::transaction(function () use (...) {
    $wallet = Wallet::whereKey($id)->lockForUpdate()->first();   // MySQL InnoDB
    if ($wallet->balance_centavos < $monto) throw new InsufficientFunds();
    // UPDATE balance + version, INSERT asiento débito (+ crédito transportadora)
});
```
- Alternativa sin lock (requerida para SQLite y como red de seguridad): `UPDATE wallets SET balance=balance-?, version=version+1 WHERE id=? AND balance>=?` y verificar `affected rows == 1`.
- `CHECK (balance_centavos >= 0)` como última barrera (falla la transacción aunque el código tenga un bug).
- Ledger append-only: un escaneo perdido no puede "rehacerse" editando filas — solo con asiento inverso.
- Test obligatorio: dos débitos concurrentes secuenciales sobre saldo de 1 pasaje → el segundo lanza `InsufficientFunds` (no saldo negativo).

## 5. Throttling / abuso del flujo QR (Sanctum)
**Riesgo:** `/qr/validate` hoy tiene `throttle:60,1` (`routes/api.php:91-92`) — suficiente para abordaje, NO para un endpoint que mueve dinero (enumeración de tokens, fuerza bruta de firmas, spam de generación de QR).
**Recomendación:**
- `POST /qr/token` (pasajero): `throttle:10,1` por usuario — 60 s TTL + rotación 30 s ≈ 2 legítimos/min; 10 da margen sin permitir barrido.
- `POST /qr/pay` (conductor): `throttle:60,1` por token (un bus realista: ≤2 abordajes/parada) + **rate extra por wallet objetivo** para frenar colusión.
- Fallo de firma/expiración ≥5 veces/min desde un mismo driver → lockout temporal del panel de cobro (cache `RateLimiter`).
- Mantener `auth:sanctum` en ambos (nunca endpoints de pago anónimos); tokens Sanctum con ability `qr:*`.
- Logging de intentos fallidos con `user_id`, `device` y `plate_snapshot` para auditoría Super Admin.

## 6. Extra detectado en auditoría
- **Migration risk:** los `UNIQUE` globales de `buses` (`plate`, `external_vehicle_id`; `create_buses_table.php:18-19`) bloquean dos transportadoras con placas repetidas → cambiar a únicos compuestos ANTES del onboarding real (ver `DB_SCHEMA.md §3`).
- **Email candado** `@unab.edu.co` (`AuthController.php:36-37`) trunca el mercado de pasajeros — eliminar en F0, no es cuello sino pré-requisito del pivote.
