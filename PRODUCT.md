# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Pasajeros de transporte público en Bucaramanga (rider), y conductores de bus (driver) que cobran a bordo. Admin de transportadoras y equipo plataforma vía paneles Filament (backend).

## Product Purpose

BUCARATRANSIT unifica a las empresas de bus de Bucaramanga en un ecosistema digital: pasajero paga con wallet prepago y QR dinámico; el conductor cobra a bordo (mPOS) eliminando efectivo, evasión y fragmentación de rutas. Cada transportadora ve su flota, rutas, tarifas y recaudo aislada (SaaS multi-tenant).

## Positioning

El QR rotativo firmado (HMAC, un solo uso, 60 s) + cobro bajo supervisión del conductor hace el pasaje de todos los tenant en un solo wallet y un solo ledger auditable de doble entrada — evasión ≈ 0.

## Operating Context

App móvil Compose Multiplatform (Kotlin), usada en la calle (Android, en autobús, luz variable). Algunos modos requieren datos en vivo: mapa de buses, ETA, QR de abordaje, cobro con cámara. El backend Laravel corre en nginx/MySQL/Redis (VM Azure bucaratransit.duckdns.org).

## Capabilities and Constraints

- Modo pasajero: mapa en vivo, búsqueda de rutas, ETA, wallet + recarga, QR rotativo de abordaje, historial de viajes, notificaciones push.
- Modo conductor: el teléfono es un mPOS que escanea el QR y cobra en milisegundos; controla ocupación y llegadas.
- Multi-tenant: cada transportadora aislada (tenant_id en todo dominio).
- Wallet: ledger append-only de doble entrada; tarifas por transportadora; recarga mock (proveedor real = F4, bloqueada por API keys).
- Firebase: solo notificaciones push (FCM) y Google Sign-In; no se usa Firebase Auth como sistema de usuarios (Laravel + Sanctum es la identidad).
- Login: email/contraseña + Google (cualquier dominio; sin candado UNAB). Pasajeros se auto-registran; credenciales de panel solo se emiten desde Filament.
- Package técnico congelado `com.vibra.bus` (identificador de Play Store); branding visible es BUCARATRANSIT.

## Brand Commitments

- Nombre definitivo **BUCARATRANSIT** (antes Bus UNAB / VibraBus).
- Mascota: **leopardo** (ej: leopardo_saludo, _curioso, _triste, _triste_espera, _celular, _mapa, _conductor, _ok, _celebrando) — reemplaza al búho UNAB.
- Tema **Bucaramanga** = "Ciudad de los Parques": LeopardGold (dorado), Espresso (negro-café rosetas), Sand (arena clara), ParqueGreen (verde parques). Marca Santander verde-amarillo.
- Estética glassmorphism + estética de mapa minimalista tipo Uber; buses 2D orientados por heading.

## Evidence on Hand

- Fotos del leopardo en `frontend/composeApp/src/commonMain/composeResources/drawable/leopardo_*.webp`.
- Paleta y temas en `frontend/composeApp/src/commonMain/kotlin/com/vibra/bus/presentation/theme/Color.kt`.
- Roadmap y pivote en `Bucaramanga_Mobility_Rebranding/` y `Demo_roadmap.md`.
- (Inferido del repo/docs, no de entrevista nueva: roles y flujos arriba; corregir si algo cambió.)

## Product Principles

- Movimiento de dinero siempre auditable (ledger, idempotencia por idempotency_key).
- Aislamiento multi-tenant como defensa primaria (GlobalScope + tests cross-tenant).
- El conductor supervisa el cobro: evasión ≈ 0.
- Branding local por encima del legado académico UNAB: Bucaramanga primero.
- Un solo sistema de identidad (backend), sin auth duplicada.

## Accessibility & Inclusion

Sin requisito estándar confirmado; la UI usa Material 3 y mantiene contraste por tema (verificar contrastes ≤4.5:1 en los fondos de color en la pasada de finish).
