ENV_FILE := .env.production
DC       := docker compose --env-file $(ENV_FILE)

# Dominio para SSL (sobreescribir: make ssl-init DOMAIN=bus.unab.edu.co EMAIL=admin@unab.edu.co)
DOMAIN   ?= bus.unab.edu.co
EMAIL    ?= admin@unab.edu.co

.DEFAULT_GOAL := help

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ── Ciclo de vida ─────────────────────────────────────────────────────────────
up: ## Iniciar todos los contenedores
	$(DC) up -d

down: ## Detener todos los contenedores
	$(DC) down

build: ## Rebuildar imágenes sin cache
	$(DC) build --no-cache

restart: ## Reiniciar todos los contenedores
	$(DC) restart

ps: ## Estado de los contenedores
	$(DC) ps

# ── Logs ──────────────────────────────────────────────────────────────────────
logs: ## Seguir todos los logs (make logs s=app para uno solo)
	$(DC) logs -f $(s)

logs-app: ## Logs del contenedor app
	$(DC) logs -f app

logs-worker: ## Logs del worker de colas
	$(DC) logs -f worker

logs-nginx: ## Logs de nginx
	$(DC) logs -f nginx

# ── Artisan / DB ──────────────────────────────────────────────────────────────
shell: ## Shell en el contenedor app
	$(DC) exec app bash

migrate: ## Ejecutar migraciones
	$(DC) exec app php artisan migrate --force

migrate-fresh: ## Drop + migrar + seed (¡destructivo!)
	$(DC) exec app php artisan migrate:fresh --seed --force

seed: ## Ejecutar seeders
	$(DC) exec app php artisan db:seed --force

tinker: ## Abrir Laravel Tinker
	$(DC) exec app php artisan tinker

clear-cache: ## Limpiar todas las caches
	$(DC) exec app php artisan cache:clear
	$(DC) exec app php artisan config:clear
	$(DC) exec app php artisan route:clear
	$(DC) exec app php artisan view:clear

cache: ## Re-cachear config / routes / views
	$(DC) exec app php artisan config:cache
	$(DC) exec app php artisan route:cache
	$(DC) exec app php artisan view:cache
	$(DC) exec app php artisan event:cache

# ── Backup ────────────────────────────────────────────────────────────────────
backup-db: ## Hacer dump de la base de datos
	$(DC) exec mysql sh -c \
		'mysqldump -u$$MYSQL_USER -p$$MYSQL_PASSWORD $$MYSQL_DATABASE' \
		> backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "Backup guardado."

# ── SSL / Certbot ─────────────────────────────────────────────────────────────
ssl-init: ## Obtener certificado Let's Encrypt por primera vez
	$(DC) run --rm certbot certonly --webroot \
		--webroot-path=/var/www/certbot \
		-d $(DOMAIN) \
		--email $(EMAIL) \
		--agree-tos \
		--no-eff-email

ssl-renew: ## Renovar certificados y recargar nginx
	$(DC) run --rm certbot renew
	$(DC) exec nginx nginx -s reload

# ── Deploy ────────────────────────────────────────────────────────────────────
deploy: ## Pull + rebuild + restart (zero-downtime básico)
	git pull origin main
	$(DC) build app worker scheduler
	$(DC) up -d --no-deps app worker scheduler nginx
