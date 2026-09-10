# GLOSSARY — Glosario canónico del pivote (ES)

> M1 · S1.1.2. Términos oficiales para docs, UI y código. Equivalencias técnicas alineadas con `DISEÑO_DB.md` (nombres reales de tablas/columnas). Base: `CONCEPTO.md §2`.

| Término (ES) | Definición funcional | Equivalente técnico |
|---|---|---|
| **Transportadora** | Empresa de buses que se onboarda como cliente SaaS; dueña de su flota, rutas, conductores y recaudo. | **Tenant** — tabla `transportadoras`; FK `transportadora_id` en tablas de dominio. |
| **Super Admin** | Equipo plataforma: gestión del ecosistema, soporte, auditoría, SaaS. | Rol `super_admin` (hoy string `admin` en BD como alias de compatibilidad, `User.php:40-42`); `users.transportadora_id IS NULL`. |
| **Admin de transportadora** | Usuario de una empresa que gestiona SOLO su tenant. | Rol `admin_transportadora` (a crear; hoy no existe); panel `/empresa` con tenancy Filament. |
| **Pasajero** | Usuario final; consulta rutas, recarga y paga. Ex-"estudiante". | `User` con role `student` en BD (alias histórico) → *pasajero* en UI/API visible. |
| **Conductor / mPOS** | Chofer cuyo teléfono es el terminal de cobro. | Rol `driver`; endpoints `POST /api/v1/qr/pay`; app modo conductor. |
| **Wallet** | Billetera prepago 1-por-pasajero; saldo en centavos. | Tabla `wallets` (`user_id` UNIQUE, `balance_centavos` BIGINT, `version`, `estado`). |
| **Recarga** | Aporte de saldo (hoy: mock/admin; proveedor externo = PENDIENTE, sin API keys). | Ledger `tipo='credito'`, `concepto='recarga_mock'`. |
| **Tarifa / Pasaje** | Precio del viaje; varía por transportadora/ruta. | Tabla `fares` (`transportadora_id`, `monto_centavos`, vigencia, `activa`). |
| **Token QR dinámico** | Credencial de pago efímera que muestra el pasajero; rota cada 30–60 s, un solo uso. | Tabla `qr_tokens` (`token_hash` UNIQUE, `signature` HMAC, `issued_at`/`expires_at` TTL 60 s, `rotacion`, `used_at` one-time). |
| **Abordaje** | Subir al bus y pagar: escaneo → validación + débito en ms. | `POST /api/v1/qr/pay` exitoso. |
| **Evasión / "Colado"** | Viajar sin pagar. KPI del modelo: ≈ 0. | Métrica de negocio (sin tabla propia; se mide por recaudo vs aforo). |
| **Ledger (doble partida)** | Libro append-only: todo evento inserta asientos débito/crédito con `balance_after`; saldo es proyección, nunca se edita sin insertar. | Tabla `wallet_transactions` (`transaction_group`, `tipo`, `cuenta_tipo`/`cuenta_id`, `amount_centavos`, `idempotency_key` UNIQUE). |
| **Transaction group** | UUID que agrupa los asientos DEBE/HABER de un mismo evento. | `wallet_transactions.transaction_group`. |
| **Idempotency key** | Clave determinística que hace seguros los reintentos (mala red ≠ doble cobro). | `wallet_transactions.idempotency_key` UNIQUE. |
| **Centavos** | Unidad monetaria interna: pesos × 100, enteros BIGINT; dividir solo en la capa de presentación. | `balance_centavos`, `amount_centavos`, `monto_centavos`. |
| **Sustento SaaS** | Suscripción mensual de la transportadora + comisión opcional por pasaje. | Tabla `saas_subscriptions` (`plan`, `estado`, `comision_porcentaje`). |

## Reglas de uso
1. En UI visible: solo términos ES ("transportadora", "pasajero", "recarga"); nunca "tenant"/"ledger" ante el usuario.
2. En código/docs técnicos: se permiten ambos, pero los nombres de tabla/columna siguen `DISEÑO_DB.md` a la letra.
3. "Estudiante" queda retirado del lenguaje de producto; sobrevive únicamente en el string de rol `student` (BD/compatibilidad) y en menciones históricas del UNAB (`MATRIZ_BRANDING.md §3`).
4. Cualquier término nuevo de negocio se añade aquí PRIMERO (este archivo es la fuente canónica acordada en `CONCEPTO.md §2`).
