# 💰 DB_SCHEMA_PAYMENTS — Wallet, ledger, tarifas y tokens QR

> M2 · S2.3.3 · Canónico de la parte **finanzas** de `DISEÑO_DB.md §4–§9`. Reglas duras: **centavos BIGINT enteros**, **ledger append-only**, **token de pago one-time firmado**. El detalle unificado queda en `DISEÑO_DB.md`; este archivo fija lo que M3 implementa (los nombres de columna exactos son los de las migraciones reales en `bus_unab/database/migrations/2026_09_*` — la implementación puede simplificar el diseño, p. ej. `reference` UNIQUE como clave de idempotencia en vez de `idempotency_key`).

## 1. `wallets`

```
id · user_id FK UNIQUE (1 wallet por usuario) · transportadora_id FK NULL (wallet
patrocinada; NULL = ciudad/plataforma) · balance_centavos BIGINT DEFAULT 0
· version BIGINT DEFAULT 0 (optimistic lock) · estado ('activa'|'congelada') · timestamps
CHECK (balance_centavos >= 0)
```
- `balance_centavos` es **proyección cacheada** del ledger: solo se escribe dentro de la transacción que inserta el asiento, con `version+1`.
- `congelada` = bloqueo por fraude (no borra saldo).

## 2. `wallet_transactions` (LEDGER append-only)

Asiento por movimiento, desde la perspectiva de la cuenta:
```
id · wallet_id FK · tipo ('credit'|'debit'|'ajuste') · monto_centavos BIGINT >0
· balance_after BIGINT (snapshot tras el asiento) · reference VARCHAR(190) UNIQUE
· contraparte VARCHAR NULL ('pasajero:{id}'|'transportadora:{id}'|'plataforma')
· metadata JSON NULL · timestamps
```
**Inquebrantables:**
1. Prohibido UPDATE/DELETE de asientos — correcciones = asiento inverso `ajuste` (el modelo lanza excepción en `updating/deleting`; evidencia implementación: `app/Models/WalletTransaction.php`).
2. Un evento de pago escribe débito(s) y crédito(s) **en la misma transacción DB**; el evento QR queda ligado por `reference='qrpay:{token_id}'`.
3. Idempotencia: `reference UNIQUE` — reintento con la misma referencia devuelve el asiento existente, nunca lo duplica.
4. Reconciliable: `SUM(creditos) − SUM(debitos) == balance_centavos` por wallet → job/comando `wallet:reconcile` (fase F2).
5. Escritura exclusiva vía `WalletService` (credit/debit con `DB::transaction` + `lockForUpdate` + guarda `balance>=monto`).

## 3. `fares`

```
id · transportadora_id BIGINT+index (NULL = tarifa general; FK consolidada luego)
· ruta_id NULLABLE · codigo/nombre · monto_centavos BIGINT >0 · activa BOOL
· vigente_desde/vigente_hasta TIMESTAMP (NULL = sin límite)
```
Calor de lectura: `INDEX(transportadora_id, activa, vigente_desde, vigente_hasta)`. Seed: tarifa default por transportadora demo.

## 4. Tokens de pago QR (`qr_payment_tokens`)

```
id · user_id FK · wallet_id FK · fare_id FK NULL · transportadora_id/bus_id (BIGINT+index)
· token_hash CHAR(64) UNIQUE (SHA-256 del selector — NUNCA el token en claro)
· monto_snapshot_centavos (congelado al emitir) · issued_at · expires_at (TTL 60 s)
· used_at NULL (ONE-TIME: primera marca gana, UPDATE condicional) · used_by_driver_id FK
· reference ('qrpay:{id}') · timestamps
```
QR visible = `selector.firmaHMAC` (HMAC-SHA256 con `app.key` sobre `selector|user_id|exp`). Ciclo: emisión (auth pasajero) → escaneo conductor → `POST /qr/pay` verifica firma + vigencia + un-solo-uso y liquida contra `WalletService` en UNA transacción. Purga de tokens vencidos (job). Anti-imagen: rotación por TTL corto. Ver `FLOWS.md` y `.opencode/docs/qr-nfc-token-security.md`.

## 5. Por qué centavos enteros (regla permanente)

DOUBLE/FLOAT descartado (aritmética no exacta rompe `SUM()`); BIGINT-centavos sobre DECIMAL: sin bugs de serialización en cliente Kotlin, índices enteros más baratos, reconciliación sin redondeo, margen para tarifas fraccionarias. **La capa de presentación divide por 100; el dominio jamás multiplica ni divide.**

## 6. `saas_subscriptions` (diseño reservado — F4)

`transportadora_id FK · plan · estado ('activa','gracia','suspendida','cancelada') · precio_mensual_centavos · comision_porcentaje DECIMAL(5,2) NULL · ciclo_inicio/fin · UNIQUE(transportadora_id, ciclo_inicio)`. Cobro automático **PENDIENTE** (proveedor de pagos = fuera de misión por regla de API keys).
