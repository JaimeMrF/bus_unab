# Rendimiento y QR: cambios y pendientes de medir

## Aplicado (por inspección)
- **Arranque:** el splash ya no espera a la red. La marca cacheada se pinta de inmediato y se refresca en segundo plano (`BrandRepository.refreshAsync`, ámbito propio). Con token guardado se entra directo a Home y la sesión se valida en segundo plano (el 401 lo detecta el polling de Home).
- **Polling de buses:** cada 5 s, secuencial (sin solapes), ocupación en paralelo cada 3 ciclos (antes: N peticiones en serie cada 30 s), avisos de red solo al cambiar de estado, pausado con la app en segundo plano y al salir de Home.
- **Listas:** `key` estable y `contentType` en buses, paradas, notificaciones, viajes y movimientos; `remember` en el mapeo de paradas de Home.
- **Animaciones:** solo `graphicsLayer`/draw; aurora solo en splash, login y código de organización (sin mapa ni listas); confeti ≤36 partículas; shimmer, aurora y ondas estáticos con reducir movimiento o gama baja.
- **Mapa:** estilo y capas se crean una vez por tema; los datos solo actualizan las fuentes GeoJSON.
- **Release:** R8 + `shrinkResources` activados (`proguard-rules.pro`). Debug solo registra líneas de petición, sin cuerpos.
- **Cámara:** se libera la cámara y el analizador al salir del escáner.

## QR
- QR de pago del wallet: se renueva 5 s antes de expirar, sin dejar de mostrar el vigente; si la emisión falla, espera exponencial 5/10/20/40/60 s; cuenta regresiva con reloj monotónico.
- QR de viaje: mismo esquema (60 s de vigencia, renovación a los 55 s), serialización fuera del hilo principal. El escáner tolera 15 s de desfase de reloj (75 s de vigencia máx., 30 s de adelanto).
- Render del QR en hilo de fondo (Android).
- Pantalla con QR: brillo máximo, sin apagarse y `FLAG_SECURE` en Android. iOS no puede bloquear capturas: solo sube el brillo y evita el apagado.
- Escáner: permiso de cámara con ruta a Ajustes, linterna, el mismo código no se reprocesa en 10 s, QR ajeno/malformado con mensaje claro, sin red → aviso persistente con Reintentar/Cancelar, y el cobro se reintenta con `Idempotency-Key` derivada del selector del QR.

## Pendiente de medir en dispositivo
- Tiempo a primer frame (arranque en frío) con y sin caché de marca; Baseline Profile (requiere medición real, no incluido).
- 60 fps de la aurora en gama media/baja y consumo de batería con el login abierto.
- Caché de disco de Coil (comprobar que el logo no se vuelve a descargar) y tamaño de decodificación de imágenes.
- Build release con R8: probar login, mapa, QR, notificaciones y Google Sign-In (las reglas no se han ejecutado).
- Backend: `/qr/pay` debería tratar la misma `Idempotency-Key` como el mismo cobro y devolver el resultado original en vez de "replay".

## T18/T19: mascota, bus dinámico y gama media
### Aplicado
- **Mascota por poses** (`BrandMascot(pose)`): Coil con tamaño fijo en px (no decodifica de más), crossfade, caché de memoria y disco, shimmer como placeholder; sin pose ni mascota única no emite nada (sin hueco). Flotación de 2.5 dp solo con movimiento ambiental.
- **Bus del mapa**: el sprite se dibuja una vez por color, estado, forma y tamaño (`BusSpriteCache`, LRU de 16) y se registra 3 veces (disponible, lleno, llegando). Las capas no se recrean: solo cambia la fuente GeoJSON. El movimiento (1 s lineal, rumbo por el camino corto) se calcula a ~30 fps solo mientras dura la interpolación, y sin movimiento ambiental salta directo a la posición.
- **Tier medio** (`MotionEnv.midTier`, menos de 4 GB): sin confeti ni parallax, aurora de 2 manchas (la mascota y el resto de transiciones se mantienen). `rich` agrupa confeti y parallax.
- **ETA**: del backend si existe (`/buses/{plate}/eta?stop_id`), con respaldo local por velocidad suavizada; si el endpoint devuelve 404 no se vuelve a pedir.
- **Listas**: filtros con `remember(lista, consulta)`, favoritas ordenadas una sola vez, `contentType` en todas.
- **Hilo principal**: el JSON de marca, la serialización de QR y el render del QR corren en `Dispatchers.Default`. El sprite del bus pesa 96x96 px.
- **Baseline Profile**: `src/main/baseline-prof.txt` escrito a mano (arranque, tema, Home, mapa). NO generado con Macrobenchmark.

### Revisión de recomposición (por lectura)
Compose compiler 2.1 usa strong skipping: las lambdas y los `List` estables por referencia no fuerzan recomposición. Ahí donde un valor cambia a cada frame (aurora, shimmer, contador, ondas, sprites de bus) se lee en `graphicsLayer` o dibujo. Home recalcula `routeStops`, `filteredBuses` y `busStates` con `remember` y claves explícitas.

### Pendiente de medir en dispositivo
- Fluidez de Home con mapa + hoja de rutas en gama media (perfilar con GPU rendering y Layout Inspector: recomposiciones por segundo durante el polling a 5 s).
- Que `baseline-prof.txt` lo recoja AGP con el layout KMP (`src/main`); si no, mover a un módulo `baselineprofile` y generarlo. La instalación fuera de Play requiere `androidx.profileinstaller` (no añadida: sin librerías nuevas).
- Costo del dibujo de 36 partículas y de la aurora en gama baja/media; consumo de batería.
- Tamaño real de las poses descargadas (1 MB máx. por imagen según contrato) y tasa de aciertos de la caché de disco.
- Icono de bus por `bus_style.icon_url` (imagen tintada): aún no consumido, solo la forma vectorial (`classic|modern|minibus`).
