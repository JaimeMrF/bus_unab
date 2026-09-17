# CONCEPTO — BUCARATRANSIT (Pivote de "Bus UNAB / VibraBus")

> Documento conceptual del pivote. Fuentes: `PROMPT_IA.md` (brief), `README.md` de esta carpeta (visión), `.opencode/context.md` (baseline técnico).
> Fase: M1 · Task T1.1 · Subtask S1.1.1

---

## 1. Marca de trabajo

**Nombre de marca: "BUCARATRANSIT"**

Marca definitiva comercial para toda la comunicación visible del proyecto —documentación, UI, panel de administración— evolucionada desde el working name provisional "Bucaramanga Mobility". Ver sección 6.

- **Qué es:** un **SaaS de movilidad urbana** que unifica a las transportadoras de Bucaramanga en una sola plataforma digital.
- **Qué dejamos de ser:** una app universitaria de rastreo de buses (Bus UNAB / VibraBus), un nicho cerrado a estudiantes de una sola institución.
- **Antes → Ahora:**

| Dimensión | Bus UNAB / VibraBus (antes) | BUCARATRANSIT (ahora) |
|---|---|---|
| Público | Estudiantes UNAB (`@unab.edu.co`) | Cualquier pasajero de Bucaramanga |
| Operador | UNAB (único) | N transportadoras (multi-tenant) |
| Cobro | Validación QR sin dinero | Wallet prepago + cobro a bordo |
| Panel | 1 panel Filament | Panel Super Admin + panel por transportadora |
| Modelo de negocio | Proyecto académico | SaaS por suscripción a transportadoras |

---

## 2. Glosario (canónico para todo el repo)

| Término | Definición | Equivalente técnico |
|---|---|---|
| **Transportadora** | Empresa de buses de la ciudad que se onboarda como cliente SaaS. Dueña de su flota, rutas, conductores y recaudo. | **Tenant** (registro en tabla `transportadoras`, FK `transportadora_id`) |
| **Super Admin** | El equipo desarrollador/operator de la plataforma. Control maestro del ecosistema: transportadoras, soporte, auditoría, modelo SaaS. | Rol `super_admin` en Laravel/Filament |
| **Admin de transportadora** | Usuario de una empresa que gestiona su tenant (flota, rutas, choferes, finanzas). No ve otros tenants. | Rol `tenant_admin` scoped por `transportadora_id` |
| **Pasajero** | Usuario final que se mueve en la ciudad; consulta rutas y paga el pasaje desde su wallet. | Modelo `User` role `pasajero` (ex `student` — ver nota de compatibilidad) |
| **Conductor (mPOS)** | Chofer de una transportadora cuyo teléfono actúa como **terminal de cobro**: escanea el QR del pasajero y descuenta el pasaje. | Rol `driver`; app modo conductor = **mPOS** |
| **Wallet** | Billetera digital prepago, 1 por pasajero, con saldo en centavos. | Modelo `Wallet` (1-1 con `User`) |
| **Recarga** | Aporte de saldo a la wallet (en esta fase: mock/admin; proveedor de pagos externo = PENDIENTE, sin API keys). | Transacción `credit` en ledger |
| **Tarifa / Pasaje** | Precio del viaje; puede variar por transportadora y ruta. | Modelo `Fare` scoped por tenant |
| **Token QR dinámico** | Código QR que el pasajero muestra al abordar; cambia cada 30–60 s, firma HMAC, de un solo uso. | Modelo `QrToken` (`token_hash`, `expires_at`, `used_at`) |
| **Abordaje** | Acto de subir al bus y pagar: conductor escanea → validación + débito en milisegundos. | `POST /api/v1/qr/pay` |
| **Evasión / Colado** | Viajar sin pagar. Meta del modelo: evasión ≈ 0 (no hay molinetes que saltar; el cobro es a bordo y obligatorio). | KPI de negocio |
| **Ledger de doble partida** | Libro de contabilidad append-only: todo movimiento genera una transacción débito/crédito con `balance_after`; nunca se edita un saldo sin insertar una transacción. | Tabla `wallet_transactions` |

> **Nota de compatibilidad:** el rol `student` existente se renombra conceptualmente a *pasajero* en UI/docs; en código/API se mantiene compatibilidad hacia atrás (alias) para no romper la app publicada.

---

## 3. Pilares del pivote

1. **Multi-tenant (SaaS por transportadora).**
   Cada transportadora es un tenant aislado: ve solo sus buses, paradas, conductores y recaudo. Un esquema compartido con columna `transportadora_id` + scopes globales Eloquent (no una BD por tenant a esta escala). Paneles Filament separados: **Super Admin** (maestro) y **Panel de la Transportadora** (`/empresa`).

2. **Wallet prepago.**
   El pasajero recarga saldo por adelantado (tarjeta virtual recargable en la app). El pasaje se descuenta del saldo al abordar. Esto elimina el efectivo —hoy el sistema entero mueve millones en monedas— y da flujo de caja anticipado.

3. **QR dinámico hoy, NFC mañana.**
   El pasajero genera un token QR de un solo uso (rota cada ~60 s, firmado en servidor). El conductor lo escanea y el cobro se aplica en milisegundos. La misma superficie de token se abrirá a NFC en el futuro sin rediseñar el cobro.

4. **El conductor como terminal de cobro (mPOS).**
   No hay estaciones, molinetes ni validadores fijos: el teléfono del conductor, con cámara nativa optimizada, es el punto de venta. La app ya incluye un escáner QR (QRScannerViewModel) — activo reaprovechable del proyecto anterior.

5. **Motor de enrutamiento "¿dónde estás → a dónde vas?".**
   El corazón de la experiencia: recomendar ruta, paradero y tarifa para cualquier trayecto de la ciudad, unificando el conocimiento fragmentado de las rutas (barrera #1 para los jóvenes). Tracking bus→parada con ETA ya existe en el código.

---

## 4. Ventajas competitivas

- **0 gasto en infraestructura externa.** Metrolínea exigió estaciones; nosotros usamos la infraestructura que ya existe: el bus mismo. El control de acceso ocurre al subir.
- **Evasión ≈ 0 ("colados" casi imposibles).** El pago se hace a bordo bajo supervisión directa del conductor, con confirmación instantánea desde la wallet. Sin molinetes que saltar ni confianza del cobrador.
- **Flujo de caja y retención prepago.** Las recargas entran antes de viajar: la plataforma administra un float (saldo retenido dentro del ecosistema) y las transportadoras reciben recaudo digital conciliable — base del modelo SaaS robusto (suscripción + posible fee transaccional).
- **Datos donde antes había efectivo.** Cada abordaje es un registro: demanda por ruta/hora para las transportadoras, y mejor servicio para el pasajero.
- **Activos ya construidos.** Reaprovechamos del proyecto universitario: login Google + Sanctum, tracking GPS con ETA, escáner QR, modo conductor, app KMP Android/iOS. El pivote los *re-alinea*, no parte de cero.

---

## 5. Alcance del rebranding

- **SÍ cambia (visible):** strings de UI de la app, `android:label`, títulos de docs del repo, `brandName` del panel Filament, `APP_NAME` de entorno, documentos de esta carpeta.
- **NO cambia (congelado):** paquetes de código `com.vibra.*`, `applicationId`/`namespace`, `google-services.json`, keystore `vibra-bus.jks`, `GOOGLE_SERVER_CLIENT_ID`, rutas API `/api/v1/*`, nombres de clases Kotlin (`VibraBusTheme`, etc.).
- Detalle archivo por archivo: **[`MATRIZ_BRANDING.md`](MATRIZ_BRANDING.md)**.

---

## 6. Nombre de marca definitivo

> **Nombre de marca definitivo: CONFIRMADO POR EL USUARIO.**
>
> Se confirma **"BUCARATRANSIT"** como marca comercial definitiva de la plataforma. La Fase A del rebranding visible queda completada en toda la UI, manifiesto, paneles de Filament y documentación. Ver detalle en `NAMING_DECISION.md` y `MATRIZ_BRANDING.md`.
