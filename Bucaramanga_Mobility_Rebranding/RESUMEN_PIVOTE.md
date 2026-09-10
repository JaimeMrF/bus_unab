# Resumen del Pivote: de Bus UNAB a Bucaramanga Mobility

Sintesis al 2026-09-09. Fuentes: .opencode/context.md, .opencode/work-log.md
(sesiones cmd_recovery_s33, cmd_s33b_final, cmd_final y cmd_roles_h1h4),
.opencode/status.md y los documentos de Bucaramanga_Mobility_Rebranding/.

## 1. Que era el proyecto antes

- "Bus UNAB" (alias VibraBus): rastreador de buses universitario de la UNAB.
- Backend Laravel 11 con UN solo panel Filament y roles admin / driver / student.
- Login Google con candado de dominio institucional (@unab.edu.co); no existia
  registro autonomo de usuarios.
- Base de datos single-tenant: sin transportadoras, wallets, transacciones,
  tarifas ni tokens QR de pago.
- El QR de la app era una credencial de abordaje, NO un instrumento de pago.
- Frontend Kotlin Multiplatform (Android): mapa, tracking bus-parada con ETA y
  escaner QR basico.

## 2. Vision nueva

"Bucaramanga Mobility" (nombre de trabajo; marca final aun por decidir): SaaS
de movilidad urbana para Bucaramanga que unifica transportadoras.

- Multi-tenant: cada transportadora es un tenant aislado con panel propio
  (rutas, flota, conductores, tarifas, finanzas).
- Super Admin: panel maestro del equipo dev (ecosistema, soporte, auditoria).
- App Pasajero: motor de enrutamiento (donde estas -> a donde vas) con wallet
  prepago y QR dinamico; futuro soporte NFC.
- App Conductor (mPOS): terminal de cobro a bordo; escanea el QR del pasajero
  y descuenta el saldo en milisegundos.
- Ventaja: cobro a bordo sin estaciones fisicas; evasion casi cero y costo de infraestructura casi nulo.

## 3. Que se construyo

- Docs: ROADMAP_TECNICO (fases F0-F4), AUDIT (gap vs vision, que refactors y
  que se conserva), ARCHITECTURE (ADRs: shared DB con columna transportadora_id,
  roles, dos paneles, ledger doble entrada, QR HMAC one-time), diseno de BD de
  pagos y matriz de branding. Matriz graphify: 1.354 nodos, 2.691 aristas, 111 comunidades.
- Tenencia: modelo Transportadora, columnas transportadora_id, TenantContext y
  middleware EnsureTenantScope; panel /empresa (TenantPanelProvider) con 16 rutas.
- Dinero: Wallet de pasajeros con ledger de doble entrada append-only, alta
  lazy de la wallet y recarga simulada (recharge-mock) para desarrollo.
- QR de pago: token HMAC efimero y de un solo uso, con monto snapshot de la
  tarifa y TTL de 60 s; reintentos (replay) rechazados.
- API v1: auth (google, login, register), emision y cobro de QR (qr/issue,
  qr/pay) y recarga mock, con Sanctum y rate limiting.
- Paneles Filament: AdminPanel maestro + panel tenant con Transportadora,
  Fare (scoped por tenant), Wallet con transacciones solo lectura y
  UserResource con roles segun el panel.
- Modelo de roles: rol "student" ELIMINADO del producto; vigentes super_admin,
  tenant_admin, driver y pasajero. Migration de datos 2026_09_09_130000 con
  down() reversible; alias isPasajero() acepta el valor legado; assignableRoles()
  y defaultRoleForPanel() por panel; candado @unab eliminado del login Google.
- Rebranding visible: docs, strings y defaults Kotlin a "pasajero"; labels de
  perfil "Estudiante" -> "Pasajero". Los identificadores tecnicos no se tocaron.
- Calidad e higiene: gates de evidencia en cada ola (php -l, migrate:fresh
  --seed, route:list, suite completa) y exclusion del ruido graphify con un
  .gitignore en la raiz.

## 4. Estado de verificacion (numeros reales)

- Suite: 88 passed / 0 failed (257 assertions); base del pivot era 82, la ola
  de roles sumo 6 tests (RegisterPasajeroTest).
- TenantIsolationTest: 13/13 (37 assertions) de aislamiento por transportadora.
- migrate:fresh --seed: OK (2 transportadoras; buses 3/3, stops 22/22, users
  3/3 con tenant; wallets 0 por diseno lazy).
- route:list: 16 rutas /empresa + rutas admin; arranque Filament limpio con
  ambos paneles registrados.
- php -l: 14/14 en la ola de roles; 11/11 y 15/15 en la ola S3.3.
- Flujo QR en vivo: recharge-mock 500.000 c -> qr/issue (snapshot 185.000,
  TTL 60 s) -> qr/pay conductor -> "Abordaje cobrado" -> replay rechazado.
- Registro probado en vivo: POST /auth/register -> 200, rol pasajero, token
  valido en /auth/me.
- Server de desarrollo corriendo en 127.0.0.1:8000.
- Archivos congelados verificados intactos (git status limpio sobre ellos).

## 5. Congelado por seguridad

- Package "com.vibra" en el codigo, google-services.json y keystore
  vibra-bus.jks.
- Motivo: cambiarlos rompe instalaciones existentes de la app, la identidad
  del proyecto Firebase y la firma con la que la app se actualiza en tiendas.
- OAuth de Google: mientras la marca no este decidida, registrar credenciales
  nuevas seria trabajo perdido. Las API keys (Firebase, Maps, pagos) quedaron
  fuera de scope por decision del usuario.
- Regla: el rebranding solo toca branding visible (docs, strings, labels);
  nada de identificadores tecnicos ni credenciales.

## 6. Que falta

Decisiones humanas (bloquean trabajo):
- Nombre definitivo y verificacion de marca (trademark) antes de tocar
  package, OAuth y tienda.
- Proveedor real de pagos para recargar la wallet (hoy solo mock).
- Piloto: transportadora y rutas reales de Bucaramanga.
- Aprobar el gerente demo en el TransportadoraSeeder y cerrar la
  NAMING_DECISION.

Fases de codigo:
- F2: UI de wallet en la app pasajero y mPOS completo en la app conductor.
- Cobro offline del conductor con cola local y sincronizacion posterior.
- F4: NFC (superficie ya prevista en el diseno del token QR) y recarga con
  proveedor real; bloqueada por las API keys.
- Deploy de produccion en bus.finsik.site.
- Commits organizados: ~94 archivos sin commitear (plan: backend+roles, docs, branding, gitignore).

## 7. Como probarlo hoy

- Backend: cd bus_unab && php artisan serve (ya corre en 127.0.0.1:8000).
- Paneles: http://127.0.0.1:8000/admin (Super Admin), http://127.0.0.1:8000/empresa (gerente).
- Credenciales: admin@unab.edu.co / Admin2024*; conductor@unab.edu.co y
  estudiante@unab.edu.co / password123 (hoy entra como pasajero); gerente demo
  gerente@metropolitana.demo / Prueba2026* (se pierde con migrate:fresh).
- Registro publico (throttle 10 req/min):
  curl -X POST http://127.0.0.1:8000/api/v1/auth/register -H "Content-Type: application/json" -d "{\"name\":\"Ana Perez\",\"email\":\"ana@gmail.com\",\"password\":\"Secreta123\",\"password_confirmation\":\"Secreta123\"}"
- Prueba QR: login pasajero -> recharge-mock -> qr/issue con bus_id -> login
  conductor -> qr/pay; comprobar saldo, "Abordaje cobrado" y rechazo de replay.
- En /empresa, al crear usuarios el gerente solo ve Conductor y Pasajero
  (default Conductor); el Super Admin ve Admin de Empresa, Conductor, Pasajero
  y los roles legados.
