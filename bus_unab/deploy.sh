#!/bin/bash
# Deploy script — run on the VPS as root or a user with sudo/docker access
set -euo pipefail

APP_DIR="/opt/bus_unab"
REPO_URL="https://github.com/JaimeMrF/bus_unab.git"
ENV_FILE=".env.production"
DC="docker compose --env-file ${ENV_FILE}"
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'

info()  { echo -e "${GREEN}[deploy] $*${NC}"; }
warn()  { echo -e "${YELLOW}[deploy] $*${NC}"; }
error() { echo -e "${RED}[deploy] $*${NC}"; exit 1; }

# ── 1. Docker ──────────────────────────────────────────────────────────────────
if ! command -v docker &>/dev/null; then
    warn "Docker not found — installing..."
    curl -fsSL https://get.docker.com | bash
    systemctl enable --now docker
fi

docker compose version &>/dev/null || error "Docker Compose plugin missing. Run: apt install docker-compose-plugin"

# ── 2. Repo ────────────────────────────────────────────────────────────────────
if [ -d "$APP_DIR/.git" ]; then
    info "Pulling latest code..."
    git -C "$APP_DIR" pull --ff-only
else
    info "Cloning repository to $APP_DIR..."
    git clone "$REPO_URL" "$APP_DIR"
fi

cd "$APP_DIR"

# ── 3. .env.production ─────────────────────────────────────────────────────────
if [ ! -f "${ENV_FILE}" ]; then
    error ".env.production not found!\nCopy it to $APP_DIR/.env.production and fill in the passwords."
fi

# Basic sanity check: APP_KEY must be set
if grep -qE '^APP_KEY=[[:space:]]*$' "${ENV_FILE}"; then
    info "Generating APP_KEY..."
    KEY="base64:$(openssl rand -base64 32)"
    sed -i "s|^APP_KEY=.*|APP_KEY=${KEY}|" "${ENV_FILE}"
    info "APP_KEY written."
fi

# ── 4. Build & start ───────────────────────────────────────────────────────────
info "Building images and starting services..."
${DC} pull mysql redis 2>/dev/null || true
${DC} up -d --build --remove-orphans

# ── 5. Wait and show status ────────────────────────────────────────────────────
info "Waiting for app to become healthy (up to 120 s)..."
for i in $(seq 1 24); do
    CONTAINER_ID=$(${DC} ps -q app 2>/dev/null || true)
    if [ -n "${CONTAINER_ID}" ]; then
        STATUS=$(docker inspect --format='{{.State.Health.Status}}' "${CONTAINER_ID}" 2>/dev/null || echo "unknown")
        if [ "${STATUS}" = "healthy" ]; then
            info "App is healthy."
            break
        fi
    fi
    printf "  [%d/24] waiting (status: %s)…\n" "$i" "${STATUS:-starting}"
    sleep 5
done

echo ""
${DC} ps
echo ""
info "Deployment complete — http://79.143.89.188"
