#!/bin/bash
set -e

# ── Ensure mounted volumes have the required directory layout ──────────────────
mkdir -p \
    /var/www/html/storage/framework/sessions \
    /var/www/html/storage/framework/views \
    /var/www/html/storage/framework/cache/data \
    /var/www/html/storage/logs \
    /var/www/html/bootstrap/cache

chown -R www-data:www-data \
    /var/www/html/storage \
    /var/www/html/bootstrap/cache 2>/dev/null || true

# ── Wait for MySQL ─────────────────────────────────────────────────────────────
echo "[entrypoint] Waiting for MySQL at ${DB_HOST}:${DB_PORT:-3306}..."
until php -r '
$h = getenv("DB_HOST");
$p = getenv("DB_PORT") ?: "3306";
$d = getenv("DB_DATABASE");
$u = getenv("DB_USERNAME");
$w = getenv("DB_PASSWORD");
try {
    new PDO("mysql:host=$h;port=$p;dbname=$d", $u, $w);
    exit(0);
} catch (Exception $e) {
    exit(1);
}
' 2>/dev/null; do
    printf '.'
    sleep 2
done
echo ""
echo "[entrypoint] MySQL ready."

# ── Wait for Redis ─────────────────────────────────────────────────────────────
if [ -n "${REDIS_HOST}" ]; then
    echo "[entrypoint] Waiting for Redis at ${REDIS_HOST}:${REDIS_PORT:-6379}..."
    until php -r '
$h = getenv("REDIS_HOST");
$p = (int)(getenv("REDIS_PORT") ?: 6379);
$w = getenv("REDIS_PASSWORD");
try {
    $r = new Redis();
    $r->connect($h, $p, 2);
    if ($w !== "" && $w !== false) $r->auth($w);
    exit(0);
} catch (Exception $e) {
    exit(1);
}
' 2>/dev/null; do
        printf '.'
        sleep 2
    done
    echo ""
    echo "[entrypoint] Redis ready."
fi

# ── Setup steps (only for the main php-fpm process) ───────────────────────────
if [ "$1" = "php-fpm" ]; then
    echo "[entrypoint] Running migrations..."
    php artisan migrate --force --no-interaction

    echo "[entrypoint] Caching config / routes / views..."
    php artisan config:cache
    php artisan route:cache
    php artisan view:cache
    php artisan event:cache

    php artisan storage:link --force 2>/dev/null || true

    # Signal that the app is ready (workers depend on this)
    touch /var/www/html/storage/.ready
    echo "[entrypoint] Application ready."
fi

exec "$@"
