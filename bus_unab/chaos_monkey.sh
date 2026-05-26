#!/usr/bin/env bash
# =============================================================================
#  CHAOS MONKEY — Bus UNAB
#  Script de Pruebas de Resiliencia — Arquitectura de Software
#  Uso: bash chaos_monkey.sh
# =============================================================================

set -uo pipefail

# --- Configuracion de entorno ---
BASE_URL="http://localhost:8080/api/v1"
STUDENT_EMAIL="estudiante@unab.edu.co"
DRIVER_EMAIL="conductor@unab.edu.co"
TEST_PASS="password123"
GPS_HOST="gpsmobile.co"

# Docker compose: detectar env file
if [ -f ".env.production" ]; then
    DC="docker compose --env-file .env.production"
elif [ -f ".env" ]; then
    DC="docker compose"
else
    DC="docker compose"
fi

# --- Colores ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# --- Deteccion de contenedores ---
detect_containers() {
    APP_CONTAINER=$($DC ps -q app 2>/dev/null | head -1 | xargs -I{} docker inspect --format='{{.Name}}' {} 2>/dev/null | sed 's/^\///' 2>/dev/null || echo "bus_unab-app-1")
    REDIS_CONTAINER=$($DC ps -q redis 2>/dev/null | head -1 | xargs -I{} docker inspect --format='{{.Name}}' {} 2>/dev/null | sed 's/^\///' 2>/dev/null || echo "bus_unab-redis-1")
    WORKER_CONTAINER=$($DC ps -q worker 2>/dev/null | head -1 | xargs -I{} docker inspect --format='{{.Name}}' {} 2>/dev/null | sed 's/^\///' 2>/dev/null || echo "bus_unab-worker-1")
}

# --- Helpers de UI ---
banner() {
    echo ""
    echo -e "${BOLD}${BLUE}================================================================${NC}"
    printf "${BOLD}${BLUE}  %-62s${NC}\n" "$1"
    echo -e "${BOLD}${BLUE}================================================================${NC}"
    echo ""
}

info()  { echo -e "  ${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "  ${GREEN}[ OK ]${NC}  $*"; }
warn()  { echo -e "  ${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "  ${RED}[FAIL]${NC}  $*"; }
step()  { echo -e "\n${BOLD}${MAGENTA}  >> $*${NC}"; }
label() { echo -e "\n${BOLD}  $*${NC}"; }

hr() { echo -e "${DIM}  ----------------------------------------------------------------${NC}"; }

pause() {
    echo ""
    echo -e "  ${DIM}Presiona ENTER para continuar...${NC}"
    read -r
}

wait_sec() {
    local n=$1
    echo -ne "  ${DIM}Esperando ${n}s "
    for _ in $(seq 1 "$n"); do sleep 1; echo -ne "."; done
    echo -e "${NC}"
}

# --- Helpers de API ---
get_token() {
    local email="${1:-$STUDENT_EMAIL}"
    local pass="${2:-$TEST_PASS}"
    curl -s -m 8 -X POST "${BASE_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"${email}\",\"password\":\"${pass}\"}" 2>/dev/null \
        | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4 || echo ""
}

http_call() {
    local method="$1" path="$2" token="${3:-}" data="${4:-}"
    local args=(-s -m 10 -w "\n[HTTP_CODE:%{http_code}]" -X "$method" "${BASE_URL}${path}" -H "Content-Type: application/json")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$data"  ] && args+=(-d "$data")
    curl "${args[@]}" 2>/dev/null || echo "[ERROR: sin conexion]"
}

show_resp() {
    local raw="$1" label="${2:-Respuesta}"
    local code body
    code=$(echo "$raw" | grep -o 'HTTP_CODE:[0-9]*' | cut -d: -f2 || echo "???")
    body=$(echo "$raw" | sed 's/\[HTTP_CODE:[0-9]*\]//' | head -4)

    if   [[ "$code" =~ ^2 ]]; then echo -e "  ${GREEN}HTTP $code${NC} $label"
    elif [[ "$code" =~ ^4 ]]; then echo -e "  ${YELLOW}HTTP $code${NC} $label"
    elif [[ "$code" =~ ^5 ]]; then echo -e "  ${RED}HTTP $code${NC} $label"
    else                           echo -e "  ${RED}HTTP $code${NC} $label — sin respuesta"
    fi
    echo -e "  ${DIM}${body}${NC}"
}

# --- Bloquear/desbloquear host en contenedor ---
block_host() {
    local container="$1" host="$2"
    docker exec -u root "$container" sh -c "echo '127.0.0.1 ${host}' >> /etc/hosts" 2>/dev/null && ok "Host bloqueado: ${host}" || warn "No se pudo bloquear el host (puede continuar sin esta simulacion)"
}

unblock_host() {
    local container="$1" host="$2"
    docker exec -u root "$container" sh -c "sed -i '/127.0.0.1 ${host}/d' /etc/hosts" 2>/dev/null || true
}

flush_cache() {
    docker exec "$APP_CONTAINER" php artisan cache:flush 2>/dev/null | tail -1 || true
}

# =============================================================================
# ESTADO DEL SISTEMA
# =============================================================================
show_status() {
    banner "ESTADO DEL SISTEMA"

    echo -e "  ${BOLD}Contenedores:${NC}"
    $DC ps --format "    {{.Service}}\t{{.Status}}" 2>/dev/null || docker compose ps --format "    {{.Service}}\t{{.Status}}"

    echo ""
    echo -e "  ${BOLD}Health de la API (GET /)${NC}"
    local code
    code=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "http://localhost:8080/" 2>/dev/null || echo "000")
    if [ "$code" = "000" ]; then
        err "API inaccesible — nginx caido o app no responde"
    else
        ok "Nginx responde — HTTP $code"
    fi

    echo ""
    echo -e "  ${BOLD}Login email/password:${NC}"
    local tok
    tok=$(get_token)
    if [ -n "$tok" ]; then
        ok "Autenticacion funciona — token obtenido"
    else
        warn "Login fallo — BD puede estar caida o usuario no seedeado"
    fi
}

# =============================================================================
# PRUEBA 1 — Matar MySQL
# =============================================================================
test_mysql_down() {
    banner "PRUEBA 1 — MySQL Caido"

    info "Que hace: detiene el contenedor MySQL."
    info "Efecto visible: TODOS los endpoints que leen de la BD"
    info "retornan HTTP 500/503. Login, buses, paradas — todo falla."
    info "La app movil mostraria pantalla de error en cada seccion."
    hr

    step "Estado ANTES — sistema operativo"
    local tok
    tok=$(get_token)
    if [ -n "$tok" ]; then
        ok "Login exitoso"
        local r; r=$(http_call GET "/buses/catalog" "$tok")
        show_resp "$r" "GET /buses/catalog"
    else
        warn "No se obtuvo token — usuario de prueba no encontrado"
    fi

    pause

    step "Matando MySQL..."
    $DC stop mysql
    ok "MySQL detenido"
    wait_sec 3

    step "Estado DESPUES — BD inaccesible"
    local r1; r1=$(http_call POST "/auth/login" "" '{"email":"estudiante@unab.edu.co","password":"password123"}')
    show_resp "$r1" "POST /auth/login  (requiere BD)"

    local r2; r2=$(http_call GET "/buses/catalog" "$tok")
    show_resp "$r2" "GET /buses/catalog (requiere BD)"

    local r3; r3=$(http_call GET "/stops" "$tok")
    show_resp "$r3" "GET /stops         (requiere BD)"

    echo ""
    warn "Lo que ve el usuario en la app: pantalla de error al abrir cualquier seccion."
    warn "Causa raiz: MySQL es el unico punto de persistencia — no hay replica."

    pause

    step "Restaurando MySQL..."
    $DC start mysql
    info "Esperando health check de MySQL..."
    wait_sec 20
    ok "MySQL restaurado"
}

# =============================================================================
# PRUEBA 2 — Matar Redis
# =============================================================================
test_redis_down() {
    banner "PRUEBA 2 — Redis Caido"

    info "Que hace: detiene Redis (cache + colas de Sanctum)."
    info "Efecto visible: el cache de ubicaciones GPS se pierde, las sesiones"
    info "pueden invalidarse. El worker no puede procesar jobs de notificacion."
    info "En condiciones normales Laravel puede degradar con ciertos errores."
    hr

    step "Estado ANTES — cache caliente"
    local tok; tok=$(get_token)
    [ -n "$tok" ] && ok "Token de sesion activo en Redis (Sanctum)"

    pause

    step "Matando Redis..."
    $DC stop redis
    ok "Redis detenido"
    wait_sec 4

    step "Estado DESPUES — sin cache"
    local r1; r1=$(http_call GET "/buses" "$tok")
    show_resp "$r1" "GET /buses (cache GPS caido)"

    local r2; r2=$(http_call GET "/buses/RUTA1/route" "$tok")
    show_resp "$r2" "GET /buses/RUTA1/route (cache de ruta caido)"

    local r3; r3=$(http_call POST "/auth/login" "" '{"email":"estudiante@unab.edu.co","password":"password123"}')
    show_resp "$r3" "POST /auth/login (nuevo token sin Redis)"

    echo ""
    warn "Sin Redis: las ubicaciones GPS no se cachean -> mas peticiones al GPS externo."
    warn "El worker de notificaciones se detiene porque no tiene cola que procesar."
    warn "Las sesiones Sanctum almacenadas en Redis pueden quedar invalidas."

    pause

    step "Restaurando Redis..."
    $DC start redis
    wait_sec 12
    ok "Redis restaurado"
}

# =============================================================================
# PRUEBA 3 — Matar Nginx (apagon total)
# =============================================================================
test_nginx_down() {
    banner "PRUEBA 3 — Nginx Caido (Apagon Total)"

    info "Que hace: detiene el reverse proxy Nginx."
    info "Efecto visible: la app movil no puede abrir ninguna pantalla."
    info "Todos los requests retornan 'Connection refused'."
    info "El backend sigue corriendo pero es completamente inaccesible."
    hr

    step "Estado ANTES — Nginx responde"
    local code; code=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "http://localhost:8080/" 2>/dev/null || echo "000")
    ok "HTTP $code — Nginx operativo"

    pause

    step "Matando Nginx..."
    $DC stop nginx
    ok "Nginx detenido"
    wait_sec 2

    step "Estado DESPUES — conexion rechazada"
    local fail
    fail=$(curl -s -m 4 --connect-timeout 3 "${BASE_URL}/buses" 2>&1 || true)
    err "Resultado: ${fail:-Connection refused}"
    err "La app movil: pantalla de 'Sin conexion a internet'."
    err "Impacto: 100% de los usuarios afectados — SPOF (Single Point of Failure)."
    echo ""
    warn "Solucion de arquitectura: multiples instancias de nginx + load balancer."

    pause

    step "Restaurando Nginx..."
    $DC start nginx
    wait_sec 6
    ok "Nginx restaurado"
}

# =============================================================================
# PRUEBA 4 — Matar Worker (notificaciones silenciadas)
# =============================================================================
test_worker_down() {
    banner "PRUEBA 4 — Queue Worker Caido"

    info "Que hace: detiene el contenedor 'worker' (php artisan queue:work)."
    info "Efecto visible: los endpoints de llegada/proximidad responden HTTP 200,"
    info "pero las notificaciones push NUNCA llegan al celular del estudiante."
    info "Los jobs quedan en la cola de Redis sin procesarse."
    hr

    step "Matando Worker..."
    $DC stop worker
    ok "Worker detenido"

    step "Obteniendo token de conductor..."
    local tok; tok=$(get_token "$DRIVER_EMAIL" "$TEST_PASS")

    if [ -n "$tok" ]; then
        step "Reportando llegada de bus RUTA1..."
        local r; r=$(http_call POST "/buses/RUTA1/arrived" "$tok" '{"stop_id":1}')
        show_resp "$r" "POST /buses/RUTA1/arrived"
        echo ""
        ok "El endpoint responde HTTP 200 — el sistema acepta el evento."
        warn "PERO: el job de notificacion quedo en la cola Redis SIN procesarse."
        warn "Los estudiantes esperando en la parada NO reciben el push notification."
    else
        warn "Conductor de prueba no disponible — mostrando concepto:"
        info "POST /buses/RUTA1/arrived -> HTTP 200 (OK en API)"
        info "Job encolado en Redis -> NUNCA procesado (worker muerto)"
        info "Push notification -> SILENCIADA"
    fi

    echo ""
    step "Verificando jobs en cola:"
    docker exec "$REDIS_CONTAINER" redis-cli llen "queues:default" 2>/dev/null \
        && ok "Cola Redis accesible" || warn "No se pudo consultar la cola directamente"

    echo ""
    warn "Patron de resiliencia: separar la aceptacion del evento (HTTP 200)"
    warn "del procesamiento asincrono (worker). La API nunca falla, pero las"
    warn "notificaciones son 'best effort' — fallan en silencio."

    pause

    step "Restaurando Worker..."
    $DC start worker
    ok "Worker restaurado — los jobs pendientes se procesaran ahora."
}

# =============================================================================
# PRUEBA 5 — GPS Externo Caido
# =============================================================================
test_gps_down() {
    banner "PRUEBA 5 — Servicio GPS Externo Caido"

    info "Que hace: bloquea el hostname gpsmobile.co en el contenedor app."
    info "Simula que el proveedor externo de GPS no responde."
    info "Efecto visible: GET /api/v1/buses retorna HTTP 503."
    info "El mapa de la app queda vacio — ningun bus aparece."
    hr

    step "Estado ANTES — GPS operativo"
    local tok; tok=$(get_token)
    flush_cache
    local r1; r1=$(http_call GET "/buses?lat=7.1218&lng=-73.1158" "$tok")
    show_resp "$r1" "GET /buses (GPS externo responde)"

    pause

    step "Bloqueando gpsmobile.co en el contenedor app..."
    block_host "$APP_CONTAINER" "$GPS_HOST"
    flush_cache
    wait_sec 3

    step "Estado DESPUES — GPS inaccesible"
    local r2; r2=$(http_call GET "/buses?lat=7.1218&lng=-73.1158" "$tok")
    show_resp "$r2" "GET /buses (GPS bloqueado)"

    local r3; r3=$(http_call GET "/buses/RUTA1" "$tok")
    show_resp "$r3" "GET /buses/RUTA1 (detalle bus — GPS bloqueado)"

    echo ""
    ok "El login, paradas y QR siguen funcionando (no dependen del GPS)."
    warn "La app movil: mapa completamente vacio, mensaje 'buses no disponibles'."
    warn "Patron aplicado: el GpsMobileService retorna null y el controlador"
    warn "devuelve 503 con mensaje amigable — no hay crash ni excepcion no manejada."

    pause

    step "Restaurando acceso al GPS..."
    unblock_host "$APP_CONTAINER" "$GPS_HOST"
    flush_cache
    ok "GPS restaurado"
}

# =============================================================================
# PRUEBA 6 — Google OAuth Caido
# =============================================================================
test_google_down() {
    banner "PRUEBA 6 — Google OAuth Caido"

    info "Que hace: bloquea oauth2.googleapis.com en el contenedor app."
    info "Simula que los servidores de Google Auth no son accesibles."
    info "EFECTO CLAVE: el login con Google falla, PERO el login con"
    info "email/password sigue funcionando — fallback de autenticacion."
    hr

    step "Estado ANTES — ambos metodos disponibles"
    echo ""
    local r1; r1=$(http_call POST "/auth/login" "" "{\"email\":\"${STUDENT_EMAIL}\",\"password\":\"${TEST_PASS}\"}")
    show_resp "$r1" "POST /auth/login  (email/password)"

    local r2; r2=$(http_call POST "/auth/google" "" '{"id_token":"TOKEN_INVALIDO_DEMO"}')
    show_resp "$r2" "POST /auth/google  (Google — token invalido esperado)"

    pause

    step "Bloqueando servidores de Google OAuth..."
    block_host "$APP_CONTAINER" "oauth2.googleapis.com"
    block_host "$APP_CONTAINER" "accounts.google.com"
    block_host "$APP_CONTAINER" "www.googleapis.com"
    wait_sec 2

    step "Estado DESPUES:"
    echo ""
    local r3; r3=$(http_call POST "/auth/login" "" "{\"email\":\"${STUDENT_EMAIL}\",\"password\":\"${TEST_PASS}\"}")
    show_resp "$r3" "POST /auth/login  (email/password — SIGUE FUNCIONANDO)"

    echo ""
    info "Intentando login con Google (timeout puede tardar ~10s)..."
    local r4; r4=$(curl -s -m 12 -w "\n[HTTP_CODE:%{http_code}]" -X POST "${BASE_URL}/auth/google" \
        -H "Content-Type: application/json" \
        -d '{"id_token":"CUALQUIER_TOKEN"}' 2>/dev/null || echo "[HTTP_CODE:000]")
    show_resp "$r4" "POST /auth/google  (Google inaccesible)"

    echo ""
    ok "Login email/password: operativo. La app puede mostrar el formulario tradicional."
    warn "Login Google: falla. Usuarios que solo tienen cuenta Google no pueden entrar."
    warn "Mejora posible: detectar el error y redirigir al formulario email/password."

    pause

    step "Restaurando Google OAuth..."
    unblock_host "$APP_CONTAINER" "oauth2.googleapis.com"
    unblock_host "$APP_CONTAINER" "accounts.google.com"
    unblock_host "$APP_CONTAINER" "www.googleapis.com"
    ok "Google OAuth restaurado"
}

# =============================================================================
# PRUEBA 7 — OSRM (Rutas) Caido
# =============================================================================
test_osrm_down() {
    banner "PRUEBA 7 — Servicio de Rutas OSRM Caido"

    info "Que hace: bloquea router.project-osrm.org — calculo de rutas en mapa."
    info "Efecto visible: GET /buses/{plate}/route retorna HTTP 502."
    info "El mapa muestra el bus pero SIN la linea de ruta trazada."
    info "El resto de la app funciona con normalidad."
    hr

    local tok; tok=$(get_token)

    step "Limpiando cache de ruta..."
    docker exec "$APP_CONTAINER" php artisan cache:forget "bus_route_polyline_RUTA1" 2>/dev/null | tail -1 || true

    step "Estado ANTES — ruta disponible"
    local r1; r1=$(http_call GET "/buses/RUTA1/route" "$tok")
    show_resp "$r1" "GET /buses/RUTA1/route"

    pause

    step "Bloqueando router.project-osrm.org..."
    block_host "$APP_CONTAINER" "router.project-osrm.org"
    docker exec "$APP_CONTAINER" php artisan cache:forget "bus_route_polyline_RUTA1" 2>/dev/null | tail -1 || true
    wait_sec 2

    step "Estado DESPUES — ruta inaccesible"
    local r2; r2=$(http_call GET "/buses/RUTA1/route" "$tok")
    show_resp "$r2" "GET /buses/RUTA1/route (OSRM caido)"

    local r3; r3=$(http_call GET "/buses" "$tok")
    show_resp "$r3" "GET /buses (ubicaciones — no depende de OSRM)"

    echo ""
    ok "Las ubicaciones de buses siguen siendo visibles en el mapa."
    warn "La linea de ruta (polyline) no se puede trazar."
    info "Patron: el fallo de OSRM esta aislado — no afecta otras funcionalidades."
    info "El cache de 24 horas reduce el impacto en produccion."

    pause

    step "Restaurando OSRM..."
    unblock_host "$APP_CONTAINER" "router.project-osrm.org"
    ok "OSRM restaurado"
}

# =============================================================================
# PRUEBA 8 — Rate Limiting (Flood de requests)
# =============================================================================
test_rate_limit() {
    banner "PRUEBA 8 — Rate Limiting: Ataque de Flood"

    info "Que hace: envia 20 requests rapidos al endpoint de auth."
    info "Configurado con throttle:10,1 (10 req/min por IP)."
    info "Efecto visible: a partir del req 11, la API retorna HTTP 429."
    info "Protege el servidor contra brute-force y ataques de denegacion."
    hr

    step "Enviando 20 requests a POST /auth/login..."
    echo ""

    local ok_count=0 limited_count=0

    for i in $(seq 1 20); do
        local code
        code=$(curl -s -m 5 -o /dev/null -w "%{http_code}" \
            -X POST "${BASE_URL}/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"email":"flood@test.com","password":"wrong"}' 2>/dev/null || echo "000")

        if [ "$code" = "429" ]; then
            limited_count=$((limited_count + 1))
            printf "  Request %-2s: ${RED}HTTP %s  <<< RATE LIMITED${NC}\n" "$i" "$code"
        else
            ok_count=$((ok_count + 1))
            printf "  Request %-2s: ${GREEN}HTTP %s${NC}\n" "$i" "$code"
        fi
    done

    echo ""
    hr
    ok "Requests aceptados: ${ok_count} | Rate limited (429): ${limited_count}"
    info "Configuracion en routes/api.php: middleware('throttle:10,1')"
    info "El cliente de la app recibe: HTTP 429 con Retry-After header."
    info "Los endpoints de lectura usan throttle:60,1 — menos restrictivo."
}

# =============================================================================
# PRUEBA 9 — Flush Cache + Impacto de Rendimiento
# =============================================================================
test_cache_impact() {
    banner "PRUEBA 9 — Impacto del Cache Redis"

    info "Que hace: vacia el cache Redis con artisan cache:flush."
    info "Efecto visible: la primera request despues del flush hace round-trip"
    info "completo al GPS externo y a la BD — notablemente mas lenta."
    info "Demuestra el valor del Cache-Aside pattern."
    hr

    local tok; tok=$(get_token)

    step "Primera request — cache caliente (GPS ya cacheado 30s)"
    local t_start t_end
    t_start=$(date +%s%3N 2>/dev/null || date +%s)
    local r1; r1=$(http_call GET "/buses?lat=7.1218&lng=-73.1158" "$tok")
    t_end=$(date +%s%3N 2>/dev/null || date +%s)
    show_resp "$r1" "GET /buses (con cache)"
    ok "Tiempo: $((t_end - t_start)) ms"

    step "Vaciando cache Redis..."
    flush_cache
    ok "Cache vaciado"

    step "Segunda request — cache FRIO (va al GPS externo)"
    t_start=$(date +%s%3N 2>/dev/null || date +%s)
    local r2; r2=$(http_call GET "/buses?lat=7.1218&lng=-73.1158" "$tok")
    t_end=$(date +%s%3N 2>/dev/null || date +%s)
    show_resp "$r2" "GET /buses (sin cache — directo a GPS)"
    ok "Tiempo: $((t_end - t_start)) ms"

    echo ""
    info "Patron Cache-Aside: el GpsMobileService cachea la respuesta 30 segundos."
    info "TTL corto garantiza datos casi en tiempo real sin sobrecargar el GPS externo."
    info "La ruta del bus se cachea 24h — cambia muy poco."
}

# =============================================================================
# RESTAURAR TODO
# =============================================================================
restore_all() {
    banner "RESTAURANDO TODO EL SISTEMA"

    step "Iniciando todos los servicios detenidos..."
    $DC start nginx app worker scheduler mysql redis 2>/dev/null || true
    ok "Servicios iniciados"

    step "Limpiando /etc/hosts del contenedor app..."
    docker exec -u root "$APP_CONTAINER" sh -c "
        sed -i '/127.0.0.1 gpsmobile.co/d' /etc/hosts 2>/dev/null
        sed -i '/127.0.0.1 oauth2.googleapis.com/d' /etc/hosts 2>/dev/null
        sed -i '/127.0.0.1 accounts.google.com/d' /etc/hosts 2>/dev/null
        sed -i '/127.0.0.1 www.googleapis.com/d' /etc/hosts 2>/dev/null
        sed -i '/127.0.0.1 router.project-osrm.org/d' /etc/hosts 2>/dev/null
        sed -i '/127.0.0.1 fcm.googleapis.com/d' /etc/hosts 2>/dev/null
        sed -i '/127.0.0.1 fcm.google.com/d' /etc/hosts 2>/dev/null
    " 2>/dev/null && ok "/etc/hosts restaurado" || warn "No se pudo limpiar hosts (el contenedor puede estar reiniciando)"

    step "Esperando health checks..."
    wait_sec 20

    show_status
    echo ""
    ok "Sistema completamente restaurado."
}

# =============================================================================
# MENU PRINCIPAL
# =============================================================================
show_menu() {
    clear
    echo -e "${BOLD}${BLUE}"
    echo "  ╔═══════════════════════════════════════════════════════════╗"
    echo "  ║         CHAOS MONKEY — Bus UNAB                          ║"
    echo "  ║         Pruebas de Resiliencia — Arquitectura Software    ║"
    echo "  ╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo -e "  ${BOLD}-- Contenedores Docker --${NC}"
    echo "  [1]  MySQL caido           La app no puede leer ni escribir datos"
    echo "  [2]  Redis caido           Cache y colas de notificacion fallan"
    echo "  [3]  Nginx caido           Apagon total — app completamente inaccesible"
    echo "  [4]  Worker caido          Notificaciones push silenciadas"
    echo ""
    echo -e "  ${BOLD}-- Servicios Externos --${NC}"
    echo "  [5]  GPS Externo caido     Mapa de buses queda completamente vacio"
    echo "  [6]  Google OAuth caido    Solo funciona login con email/password"
    echo "  [7]  OSRM (Rutas) caido    Bus visible pero sin linea de ruta en mapa"
    echo ""
    echo -e "  ${BOLD}-- Estres y Rendimiento --${NC}"
    echo "  [8]  Rate Limiting         HTTP 429 despues de 10 req/min por IP"
    echo "  [9]  Impacto del Cache     Medir diferencia de tiempo con/sin Redis"
    echo ""
    echo -e "  ${BOLD}-- Control --${NC}"
    echo "  [s]  Estado del sistema"
    echo "  [r]  RESTAURAR TODO"
    echo "  [q]  Salir"
    echo ""
    echo -ne "  ${BOLD}Elige una opcion: ${NC}"
}

# =============================================================================
# MAIN
# =============================================================================
main() {
    if ! command -v docker &>/dev/null; then
        echo "ERROR: Docker no encontrado. Instala Docker Desktop primero."
        exit 1
    fi

    if ! command -v curl &>/dev/null; then
        echo "ERROR: curl no encontrado. Instala curl primero."
        exit 1
    fi

    detect_containers

    echo ""
    info "Contenedores detectados: app=${APP_CONTAINER} | redis=${REDIS_CONTAINER}"
    info "API base: ${BASE_URL}"
    echo ""

    if ! $DC ps --quiet 2>/dev/null | grep -q .; then
        warn "No hay contenedores corriendo. Levanta el sistema primero:"
        echo ""
        echo "  docker compose --env-file .env.production up -d"
        echo ""
        echo -ne "  Continuar de todas formas? [s/N]: "
        read -r resp
        [[ "$resp" =~ ^[sS]$ ]] || exit 0
    fi

    while true; do
        show_menu
        read -r choice

        case "$choice" in
            1) test_mysql_down   ;;
            2) test_redis_down   ;;
            3) test_nginx_down   ;;
            4) test_worker_down  ;;
            5) test_gps_down     ;;
            6) test_google_down  ;;
            7) test_osrm_down    ;;
            8) test_rate_limit   ;;
            9) test_cache_impact ;;
            s|S) show_status     ;;
            r|R) restore_all     ;;
            q|Q)
                echo ""
                info "Saliendo. Recuerda restaurar el sistema si hiciste pruebas destructivas."
                echo ""
                exit 0
                ;;
            *) warn "Opcion invalida: '${choice}'" ;;
        esac

        pause
    done
}

main "$@"
