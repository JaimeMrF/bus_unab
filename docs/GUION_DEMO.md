# Guion de la demo — BucaraTransit

Recorrido probado el 2026-10-25 en local (emulador Android + paneles `/admin` y `/empresa`).
Duración objetivo: 8–10 minutos. Cada ciclo dice qué mostrar y qué decir.

## Antes de empezar (10 min antes)

1. **Backend desplegado** con el último `main` (ver "Despliegue" abajo).
2. **Buses en movimiento.** En producción el simulador NO corre solo. Arráncalo para la demo:
   ```bash
   ssh -i bus_unab/vmbucaratransit_key.pem azureuser@bucaratransit.lat
   cd /opt/bus_unab/bus_unab && sudo docker compose --env-file .env.production exec -d app php artisan demo:simulate-buses --speed=30
   ```
   (Alternativa: el celular del conductor en "Modo conductor" comparte su GPS y mueve ese bus.)
3. **Dos celulares** con el APK v1.0.0 (o uno + el emulador): uno con el pasajero, otro con el conductor.
4. **Navegador** con dos pestañas: `/admin` (super admin) y `/empresa` (admin de BucaraTransit),
   ya con sesión iniciada. Las claves de producción están en `/opt/bus_unab/CREDENCIALES.txt` de la VM.
5. Cerrar sesión del pasajero si quedó un viaje pendiente de un ensayo ("Mis viajes" → Cancelar viaje).

## Ciclo 1 — Marca blanca (1 min)

- Abrir la app: login con logo y mascota de BucaraTransit.
- Decir: la app no trae marca en el código; la descarga del servidor según la organización.
- Opcional: en `/empresa` → **Marca de la app**, cambiar el color primario, guardar y reabrir la app.

## Ciclo 2 — Pasajero encuentra su bus (2 min)

1. Entrar como pasajero → mapa con **"N buses activos cerca"** y buses moviéndose.
2. Señalar el resplandor: verde = disponible, ámbar = llegando, rojo = lleno.
3. **Buscar rutas** → elegir **BucaraTransit Línea 1** → el mapa encuadra la ruta y sus 5 paradas.
4. Elegir parada → "llega en X min" → **Confirmar** → pantalla **Siguiendo bus** (distancia + ETA).

## Ciclo 3 — Conductor marca "Bus lleno" (1,5 min)

1. En el otro celular: **Acceso de conductores** → entrar como conductor → ruta BT401.
2. Activar **Bus lleno**.
3. Mostrar en el celular del pasajero y en `/admin` → **Mapa en Vivo** que BT401 pasa a **rojo, 100 %**.
4. Desactivarlo: vuelve al color normal.

## Ciclo 4 — Pago con QR (1,5 min)

1. Pasajero → **Wallet**: saldo, **Recargar $5.000** (recarga simulada de pruebas).
2. **Mostrar QR de pago** (dura 60 s; Android bloquea capturas de esa pantalla a propósito).
3. Conductor escanea el QR con la cámara → se descuenta el pasaje ($1.850) del saldo.
   - Esto necesita cámara real: probarlo antes con los dos celulares.

## Ciclo 5 — Empresa transportadora, panel `/empresa` (2 min)

1. **Escritorio**: buses activos, pasajeros, solicitudes activas, paradas (solo de su empresa).
2. **Mapa en Vivo**: solo su flota (BT401/BT402) con el mismo código de colores.
3. **Buses → Editar Ruta**: lista ordenada de paradas, **Vista previa por calles** (línea verde), **Guardar ruta**.
4. **Tarifas → Crear**: monto en pesos y ruta opcional.

## Ciclo 6 — Super admin, panel `/admin` (1,5 min)

1. **Transportadoras**: varias empresas en la misma plataforma (multi-tenant), plan y kill-switch.
2. **Wallets** → **Ajustar saldo** a un pasajero (+5000, motivo) → el saldo cambia en la app al refrescar.
3. **Paradas → Crear**: buscar "UNAB" en el mapa → llena nombre, dirección y coordenadas.

## Qué NO mostrar (o avisar)

- **Broadcast** (notificaciones push): producción no tiene credenciales de Firebase
  (`FIREBASE_CREDENTIALS_B64`). El panel ahora lo dice ("Push no configurado") en vez de fingir el envío.
- **Login con Google**: probarlo antes en un celular real; si falla, usar correo y clave.
- La recarga de saldo es **simulada** (no hay pasarela de pago real).

## Despliegue del backend

```bash
ssh -i bus_unab/vmbucaratransit_key.pem azureuser@bucaratransit.lat
cd /opt/bus_unab && git pull --ff-only && cd bus_unab
sudo docker compose --env-file .env.production build app worker scheduler
sudo docker compose --env-file .env.production up -d app worker scheduler
sudo docker compose --env-file .env.production exec -T app php artisan optimize
```

`build` necesita `sudo`: la carpeta `~/.docker/buildx` de `azureuser` da "permission denied".

## Cómo se probó

- `tests/Feature/Admin/PanelPagesSmokeTest.php` abre **todas** las pantallas de `/admin` y `/empresa`
  con los datos demo y falla si alguna no responde 200 (así se detectó el 500 de Wallets y el de Tarifas).
- Recorrido manual en el navegador (local): ajuste de saldo, editor de rutas (vista previa y guardado),
  búsqueda en el mapa de paradas y de puntos de interés, mapa en vivo de `/empresa`.
- App en emulador: inicio, wallet, QR, mis viajes, alertas, perfil.
