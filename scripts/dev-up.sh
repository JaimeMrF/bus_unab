#!/usr/bin/env bash
# Levanta el backend Laravel en modo DEV/demo accesible desde la LAN (solo desarrollo).
# Uso: scripts/dev-up.sh [--fresh] [--stop] [--simulate] [--queue] [--port N] [--org slug]
set -euo pipefail

FRESH=0; STOP=0; SIMULATE=0; QUEUE=0; PORT=8000; ORG=metrobus
while [ $# -gt 0 ]; do
  case "$1" in
    --fresh|-Fresh) FRESH=1;; --stop|-Stop) STOP=1;; --simulate|-Simulate) SIMULATE=1;;
    --queue|-Queue) QUEUE=1;; --port|-Port) PORT="$2"; shift;; --org|-OrgSlug) ORG="$2"; shift;;
    *) echo "flag desconocido: $1" >&2; exit 2;;
  esac; shift
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/bus_unab"
PIDFILE="$BACKEND/storage/dev-up.pids"
info() { printf '\033[36m[dev-up]\033[0m %s\n' "$*"; }
fail() { printf '\033[31m[dev-up] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

stop_dev() {
  if [ -f "$PIDFILE" ]; then
    while read -r p; do
      [ -n "$p" ] || continue
      pkill -TERM -P "$p" 2>/dev/null || true   # hijo `php -S` de `artisan serve`
      kill "$p" 2>/dev/null && info "detenido PID $p" || true
    done < "$PIDFILE"
    rm -f "$PIDFILE"
  fi
  # Red de seguridad: lo que siga escuchando en el puerto
  if command -v lsof >/dev/null; then lsof -ti tcp:"$PORT" -sTCP:LISTEN 2>/dev/null | xargs -r kill 2>/dev/null || true
  elif command -v fuser >/dev/null; then fuser -k "$PORT"/tcp 2>/dev/null || true; fi
}
if [ "$STOP" = 1 ]; then stop_dev; exit 0; fi

for c in php composer; do command -v "$c" >/dev/null || fail "'$c' no esta en PATH (o usa docker compose, ver README)"; done
mods="$(php -m)"
echo "$mods" | grep -qix gd || echo "WARN: extension PHP gd no habilitada (opcional)" >&2
for m in mbstring pdo_sqlite intl bcmath; do echo "$mods" | grep -qix "$m" || fail "extension PHP '$m' no habilitada"; done

IP="$( (hostname -I 2>/dev/null | tr ' ' '\n' | grep -E '^(192\.168|10\.|172\.(1[6-9]|2[0-9]|3[01]))' | head -1) || true)"
[ -z "$IP" ] && IP="$(ipconfig getifaddr en0 2>/dev/null || true)"
if [ -z "$IP" ]; then IP=127.0.0.1; echo "WARN: sin IP LAN; usando 127.0.0.1" >&2; fi
APP_URL="http://$IP:$PORT"

cd "$BACKEND"
[ -f vendor/autoload.php ] || { info "composer install"; composer install --no-interaction --prefer-dist; }

created=0
[ -f .env ] || { cp .env.example .env; created=1; info ".env creado desde .env.example"; }
set_env() { # key value
  if grep -qE "^#?[[:space:]]*$1=" .env; then
    sed -i.bak -E "s|^#?[[:space:]]*$1=.*|$1=$2|" .env && rm -f .env.bak
  else printf '%s=%s\n' "$1" "$2" >> .env; fi
}
set_env APP_URL "$APP_URL"
set_env CORS_ALLOWED_ORIGINS "$APP_URL,http://localhost:$PORT,http://10.0.2.2:$PORT"
[ "$created" = 1 ] && set_env APP_DEBUG true
grep -q '^APP_KEY=base64:' .env || php artisan key:generate --force >/dev/null
if grep -q '^DB_CONNECTION=sqlite' .env && [ ! -f database/database.sqlite ]; then touch database/database.sqlite; fi

stop_dev
if [ "$FRESH" = 1 ]; then info "demo:setup (BD nueva, destructivo)"; php artisan demo:setup
else info "migrate + demo:setup --no-fresh"; php artisan migrate --force; php artisan demo:setup --no-fresh; fi
php artisan storage:link >/dev/null 2>&1 || true

mkdir -p storage/logs
: > "$PIDFILE"
nohup php artisan serve --host=0.0.0.0 --port="$PORT" >storage/logs/dev-serve.out.log 2>&1 & echo $! >> "$PIDFILE"
if [ "$QUEUE" = 1 ]; then nohup php artisan queue:work --tries=1 >/dev/null 2>&1 & echo $! >> "$PIDFILE"; fi
if [ "$SIMULATE" = 1 ]; then nohup php artisan demo:simulate-buses >storage/logs/dev-simulate.out.log 2>&1 & echo $! >> "$PIDFILE"; fi

command -v ufw >/dev/null && echo "Si usas ufw: sudo ufw allow $PORT/tcp" || true
ok=0
for _ in $(seq 1 30); do
  if curl -fs "http://127.0.0.1:$PORT/api/v1/branding/$ORG" >/dev/null 2>&1; then ok=1; break; fi; sleep 1
done
[ "$ok" = 1 ] || fail "el servidor no respondio en /api/v1/branding/$ORG (ver storage/logs/dev-serve.out.log)"

cat <<OUT

================ BUS DEV LISTO (solo desarrollo) ================
 Servidor : $APP_URL   (health OK)
 API      : $APP_URL/api/v1
 Panel    : $APP_URL/admin  |  $APP_URL/empresa
 Orgs demo: metrobus, campus, logistica  (default: $ORG)
 Login    : admin.<slug>@demo.test / driver.<slug>@demo.test / pasajero.<slug>@demo.test  -  clave Demo12345!
 Super    : superadmin@demo.test (panel /admin; tenant en /empresa)  -  detalle: docs/DEMO.md
 Detener  : scripts/dev-up.sh --stop

 Build de la app (desde frontend/):
   ./gradlew :composeApp:installDebug -PapiBaseUrl=$APP_URL/api/v1 -PdefaultOrgSlug=$ORG
=================================================================
OUT
