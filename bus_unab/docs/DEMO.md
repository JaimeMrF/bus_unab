# Demo white-label

Entorno de demostración con **3 organizaciones** de identidad muy distinta, para
ver cómo la app cambia de marca solo con el slug. Solo funciona con
`APP_ENV=local` o `testing`; en cualquier otro entorno los comandos se niegan.

## Puesta en marcha

```bash
php artisan demo:setup              # migrate:fresh --seed + DemoSeeder + storage:link (BORRA la BD)
php artisan demo:setup --no-fresh   # solo migra y re-ejecuta DemoSeeder (idempotente, no borra nada)
php artisan serve --host=0.0.0.0    # API
php artisan demo:simulate-buses     # opcional: buses moviéndose (bucle, Ctrl+C para salir)
```

`APP_URL` debe coincidir con el host que ve el cliente (las URLs de logo/mascota
salen de `APP_URL/storage`), p. ej. `http://10.0.2.2:8000` desde el emulador
Android. `CACHE_STORE` debe ser persistente (por defecto `database`) para que el
simulador y el servidor compartan las posiciones.

## Organizaciones

| Slug | App | Fuente | Radio | Primario (claro) | Funciones | Mascota |
|---|---|---|---|---|---|---|
| `metrobus` | MetroBus | poppins | lg | `#0F766E` (teal) | QR + wallet + conductor | sí |
| `campus` | Campus Go | inter | sm | `#6D28D9` (violeta) | solo conductor | sí |
| `logistica` | RutaCarga | system | md | `#C2410C` (naranja) | wallet + conductor | no |

Branding público: `GET /api/v1/branding/{slug}`. Los logos/mascotas son SVG
generados localmente en `storage/app/public/branding/demo/`.

## Credenciales

Password de **todos** los usuarios: `Demo12345!`

| Rol | Email | Acceso |
|---|---|---|
| Super admin | `superadmin@demo.test` | panel `/admin` |
| Admin de empresa | `admin.{slug}@demo.test` | panel `/empresa` (solo su organización) |
| Conductor | `driver.{slug}@demo.test` | app (modo conductor) |
| Pasajero | `pasajero.{slug}@demo.test` | app; wallet con $50.000 COP |
| Pasajero de ciudad | `pasajero@demo.test` | app, sin organización; wallet con $50.000 COP |

`{slug}` = `metrobus`, `campus` o `logistica`.

## Datos operativos

- Buses (2 por organización, el segundo recorre la ruta al revés):
  `MB101/102`, `CP201/202`, `LG301/302`.
- 4–5 paradas por organización en zonas distintas de Bucaramanga, rutas con
  waypoints y una tarifa (`DEMO-ORD`, `DEMO-EST`, `DEMO-CARGA`).
- Wallets con saldo inicial 5.000.000 centavos; el ledger cuadra con el saldo.

## Simulador de buses

`php artisan demo:simulate-buses [--interval=2] [--speed=40] [--ticks=0]`

Mueve cada bus por su ruta de ida y vuelta y publica posición y rumbo en la caché
`driver_location_{PLACA}` (TTL 60 s: si detienes el simulador, los buses
desaparecen). `--ticks=N` termina tras N actualizaciones (0 = infinito).
Lo consume el mapa del panel `/admin`; `GET /api/v1/buses` sigue consultando el
GPS externo.

## Idempotencia

Todo se resuelve por claves naturales (slug, email, placa, parada, `reference` del
ledger). Re-ejecutar no duplica filas, no cambia contraseñas ni saldos y no sube
`branding_version`.

## App móvil: servidor de desarrollo
- **Android:** en la pantalla de código de organización, *Opciones avanzadas > Servidor* acepta `http://192.168.x.x:8000` o `10.0.2.2:8000` solo en builds **debug**; el release exige HTTPS.
- **iOS:** el servidor de dev debe ser **https** (ATS bloquea http), o bien definir la clave `ApiBaseUrl` (y opcionalmente `DefaultOrgSlug`) en el `Info.plist` del target. Sin `ApiBaseUrl`, el campo *Servidor* es obligatorio en el primer arranque. El cliente iOS nunca acepta http (`ALLOW_CLEARTEXT = false`).
- Android también admite fijar el backend por build: `-PapiBaseUrl=https://.../api/v1`.
