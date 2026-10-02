# Onboarding de un tenant nuevo (white-label)

Un tenant = una `Transportadora` (organización). Cada una tiene su perfil de branding que la app carga en runtime vía `GET /api/v1/branding/{slug}`.

## 1. Prerrequisitos
- Stack desplegado (`docker compose up -d`) y migraciones aplicadas (`php artisan migrate --force`).
- Acceso como super admin al panel Filament (`/admin`).

## 2. Crear la Transportadora
1. Panel super admin → **Transportadoras → Crear**.
2. Completar nombre, NIT y un **slug** único en minúsculas (`[a-z0-9-]`, ej. `metrolinea`). El slug identifica al tenant en la app y en la API; no se puede reutilizar.
3. Marcar la transportadora como **activa** (si está inactiva, la API de branding devuelve 404).
4. Crear el usuario administrador del tenant y asignarlo a la transportadora. Debe acceder solo a su panel (`/tenant`).

## 3. Configurar branding
Con el admin del tenant (o super admin) → **Branding**:

| Campo | Regla |
|---|---|
| `app_name`, `tagline`, `support_email` | texto; email válido |
| Colores light/dark (11 claves) | hex `#RRGGBB`; contraste `on_primary`/`primary` ≥ 4.5:1 |
| Logo, logo oscuro, ícono, mascota | png/webp/svg sanitizado, ≤ 1 MB |
| `font_family` | `poppins`, `inter` o `system` |
| `corner_radius` | `sm`, `md`, `lg` |
| Features | `qr_payments`, `wallet`, `driver_mode` |

Cada guardado incrementa `version`; los clientes cachean por versión.

## 4. Dominio
1. Crear un registro DNS `A`/`CNAME` del dominio del tenant hacia el servidor.
2. Añadir el dominio a `server_name` en `docker/nginx/default.conf` y emitir certificado:
   `docker compose run --rm certbot certonly --webroot -w /var/www/certbot -d tenant.example.com`
3. Recargar nginx: `docker compose exec nginx nginx -s reload`.
4. Añadir el origen a `CORS_ALLOWED_ORIGINS` si el cliente web lo consume.

## 5. Verificación
```
curl -si https://api.example.com/api/v1/branding/<slug>   # 200, ETag, Cache-Control: public, max-age=300
curl -si https://api.example.com/api/v1/branding/no-existe # 404 genérico
```
- La app, con el código de organización `<slug>`, muestra nombre, colores y logo del tenant.
- Un admin de otro tenant NO puede ver ni editar este branding (403/404).

## 6. Rollback
Desactivar la Transportadora (la API devuelve 404 y la app usa el último branding cacheado o el tema neutro). No borrar datos sin confirmación.
