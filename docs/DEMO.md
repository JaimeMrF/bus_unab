# Demo local (solo desarrollo)

Levanta backend + datos demo y deja la API accesible desde el celular en la misma red Wi-Fi.

## Arranque

```powershell
# Windows (principal)
.\scripts\dev-up.ps1 [-Fresh] [-Simulate] [-Queue] [-Port 8000]
.\scripts\dev-up.ps1 -Stop
```
```bash
# Linux/macOS
scripts/dev-up.sh [--fresh] [--simulate] [--queue] [--port 8000]
scripts/dev-up.sh --stop
```

El script: verifica PHP 8.2+ y `composer` (extensiones obligatorias: mbstring, pdo_sqlite, intl, bcmath; `gd` solo avisa si falta), instala dependencias, crea `bus_unab/.env` si falta y fija `APP_URL=http://<IP_LAN>:8000` y `CORS_ALLOWED_ORIGINS`, ejecuta `migrate` + `demo:setup --no-fresh`, arranca `php artisan serve --host=0.0.0.0`, abre el firewall de Windows (perfil Privado; sin admin imprime el comando), comprueba `/api/v1/branding/<slug>` e imprime URL, credenciales y el comando de build.

| Flag | Efecto |
|---|---|
| `-Fresh` / `--fresh` | `demo:setup` completo: **borra la BD** y la recrea con datos demo |
| `-Simulate` | lanza `demo:simulate-buses` (buses en movimiento) |
| `-Queue` | lanza `queue:work` |
| `-Port` | puerto (8000 por defecto) |
| `-Stop` | detiene los procesos lanzados por el script |

Es idempotente: sin `-Fresh` repetirlo no borra datos. Solo funciona con `APP_ENV=local|testing`.

## Organizaciones y credenciales demo
Contraseña de todos: `Demo12345!`

| Org (slug) | Admin tenant (`/empresa`) | Conductor | Pasajero |
|---|---|---|---|
| `bucaratransit` | admin.bucaratransit@demo.test | driver.bucaratransit@demo.test | pasajero.bucaratransit@demo.test |
| `metrobus` | admin.metrobus@demo.test | driver.metrobus@demo.test | pasajero.metrobus@demo.test |
| `campus` | admin.campus@demo.test | driver.campus@demo.test | pasajero.campus@demo.test |
| `logistica` | admin.logistica@demo.test | driver.logistica@demo.test | pasajero.logistica@demo.test |

Super admin (`/admin`): superadmin@demo.test. Pasajero de ciudad (sin org): pasajero@demo.test.
Buses: BT401/BT402 (bucaratransit, con 9 poses de mascota), MB101/MB102, CP201/CP202, LG301/LG302. Con `-Simulate`, `/api/v1/buses` usa las posiciones del simulador (no requiere gpsmobile.co).

## App Android
Desde `frontend/` (teléfono físico en la misma red; con emulador usa `10.0.2.2` como IP):
```
./gradlew :composeApp:installDebug -PapiBaseUrl=http://<IP>:8000/api/v1 -PdefaultOrgSlug=bucaratransit
```
Cambia `-PdefaultOrgSlug` a `metrobus`, `campus` o `logistica` para ver otro branding.

## Problemas frecuentes
- **El teléfono no conecta**: falta la regla de firewall (ejecuta el comando que imprime el script como administrador) o la red es "Pública".
- **Logos no cargan**: `APP_URL` debe coincidir con el host que ve el cliente; reejecuta el script tras cambiar de red.
- **Logs**: `bus_unab/storage/logs/dev-serve.*.log`.
