# Script de Presentacion — Bus UNAB
## Arquitectura de Software — Pruebas de Resiliencia (Chaos Engineering)

> 📜 **Documento histórico**: guion de la entrega universitaria original ("Bus UNAB", equipo VIBRA+ — UNAB). El proyecto hoy está en pivote a **Bucaramanga Mobility** (SaaS multi-tenant) — ver [`../Bucaramanga_Mobility_Rebranding/README.md`](../Bucaramanga_Mobility_Rebranding/README.md). Se conserva íntegro por trazabilidad.

---

## 1. APERTURA (1-2 min)

> "Presentamos **Bus UNAB**: un sistema de rastreo en tiempo real para los buses universitarios.
> El problema es simple: los estudiantes no saben donde esta el bus ni cuando llega a su parada.
> Nuestra solucion es una API REST containerizada que conecta el GPS real de los buses
> con la app movil, con notificaciones push cuando el bus se acerca."

---

## 2. ARQUITECTURA (3 min)

Mostrar: `docker compose --env-file .env.production ps`

> "La arquitectura esta compuesta por **6 contenedores Docker** con responsabilidades separadas:"

| Servicio   | Rol                                           |
|------------|-----------------------------------------------|
| nginx      | Reverse proxy — unico punto de entrada HTTP   |
| app        | API Laravel (PHP-FPM) — logica de negocio     |
| worker     | Procesador de colas — notificaciones push     |
| scheduler  | Tareas programadas (cron de Laravel)          |
| mysql      | Persistencia — usuarios, buses, paradas       |
| redis      | Cache de GPS + cola de notificaciones         |

> "Cada servicio tiene una sola responsabilidad. Ahora vamos a ver que pasa
> cuando cada uno falla — esto se llama **Chaos Engineering**."

---

## 3. DEMO: CHAOS MONKEY (15 min)

Ejecutar: `bash chaos_monkey.sh`

### Pruebas recomendadas para la presentacion:

**[3]  Nginx caido**
> "Aqui vemos el caso mas critico: sin el reverse proxy, la app es completamente inaccesible.
> Ninguna request llega al backend. Solucion de arquitectura: multiples instancias + load balancer."

**[5]  GPS Externo caido**
> "El proveedor GPS de UNAB no responde. Observen que el endpoint retorna 503
> con un mensaje amigable — no un crash. El GpsMobileService detecta el fallo
> y retorna null, que el controlador transforma en una respuesta HTTP correcta.
> Lo llaman **Graceful Degradation**."

**[6]  Google OAuth caido**
> "Esto es lo mas interesante. Google no responde, pero la app NO muere completamente.
> El login con email y contrasena sigue funcionando. Este es un patron de **fallback**:
> si el servicio primario falla, el secundario toma el relevo. El usuario puede entrar
> igual, solo que sin el boton de Google."

**[8]  Rate Limiting**
> "Enviamos 20 requests seguidos al endpoint de login — que normalmente seria un
> ataque de fuerza bruta. A partir del request 11, el servidor retorna HTTP 429.
> Esto esta configurado en el middleware de Laravel: throttle:10,1.
> Protege sin necesitar un firewall externo."

---

## 4. PATRONES DE ARQUITECTURA DEMOSTRADOS

> "Resumiendo los patrones que acabamos de ver en vivo:"

- **Graceful Degradation** — GPS cae, el endpoint retorna 503 amigable (no crash)
- **Fallback Pattern** — Google cae, email/password actua como respaldo
- **Bulkhead** — contenedores aislados, el fallo de MySQL no mata Nginx
- **Cache-Aside** — Redis cachea GPS 30s, rutas 24h; sin redis la app degrada, no explota
- **Async Processing** — el worker procesa notificaciones asincrono; si cae, la API sigue
- **Rate Limiting** — throttle middleware protege cada endpoint con limites distintos

---

## 5. CIERRE (1 min)

> "La arquitectura containerizada nos permite testear el comportamiento ante fallos
> antes de que ocurran en produccion. Cada contenedor es reemplazable e independiente.
> Con `docker compose restart <servicio>` recuperamos cualquier falla en segundos.
> La resiliencia no es un feature — es una decision de diseno desde el dia uno."

---

## Comandos utiles para la demo

```bash
# Levantar el sistema
docker compose --env-file .env.production up -d

# Ver estado de todos los servicios
docker compose --env-file .env.production ps

# Lanzar el Chaos Monkey
bash chaos_monkey.sh

# Ver logs en tiempo real de un servicio
docker compose --env-file .env.production logs -f app

# Ver logs del worker (notificaciones)
docker compose --env-file .env.production logs -f worker

# Restaurar todo si algo queda roto
docker compose --env-file .env.production start nginx app worker scheduler mysql redis
```
