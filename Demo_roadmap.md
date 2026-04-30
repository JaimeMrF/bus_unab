# 🚀 Roadmap de Demostración: Bus UNAB Vibra+

Este documento detalla los pasos exactos para realizar una demostración impecable del prototipo mañana.

## 1. 🔑 Acceso y Login
Para que el login funcione sin fricciones, tienes dos caminos:

### Opción A: Ingreso con Google (Recomendado)
*   **Qué usar:** Haz clic en el botón **"Ingresa con Google"**.
*   **Backend:** Asegúrate de que el servidor Laravel esté corriendo, ya que la app enviará el `idToken` al endpoint `/api/v1/auth/google`.
*   **Ventaja:** Muestra la integración moderna con el Credential Manager de Android.

### Opción B: Credenciales UNAB
*   **Qué usar:** Introduce correo y contraseña.
*   **Credenciales de Prueba:**
    *   **Estudiante:** `estudiante@unab.edu.co` / `password123`
    *   **Conductor:** `conductor@unab.edu.co` / `password123`
*   *Nota: Asegúrate de haber ejecutado los seeders en el backend Laravel (`php artisan db:seed`).*

---

## 2. 🗺️ Flujo del Estudiante (El Corazón de VIBRA+)
El objetivo es mostrar cómo un estudiante encuentra su ruta y ve el bus llegar.

1.  **Home:** Al entrar, verás el mapa con estilo "Silver Premium".
2.  **Selección de Parada:** Desliza la lista de paradas y selecciona una (ej. "Parada Campus Central").
3.  **Búsqueda de Buses:** La app mostrará los buses cercanos a esa parada. Selecciona uno.
4.  **Seguimiento (Wait Screen):**
    *   Verás la **Polilínea** punteada entre el bus y la parada.
    *   El **ETA** (minutos) y la **distancia** se actualizarán cada 3 segundos.
    *   **Hito Demo:** Cuando el bus esté "cerca", aparecerá la alerta: *"¡Tu bus está llegando!"*.

---

## 3. 🚌 Flujo del Conductor (Modo Operativo)
Para mostrar esto, debes iniciar sesión con el rol de `driver`.

1.  **Entrada:** En el Home, aparecerá el botón **"Entrar a Modo Conductor"**.
2.  **Dashboard:**
    *   Muestra la placa asignada.
    *   Activa el switch de **"Bus Lleno"**: Verás cómo cambia el color de la UI para indicar que no se recogerán más pasajeros.
3.  **Validación QR:**
    *   Simula que llega un estudiante. Haz clic en el icono de **Escáner**.
    *   Abre la cámara y escanea el QR generado por otro teléfono (en el modo estudiante).

---

## 4. 🛠️ Checkpoint Técnico (Antes de la Presentación)

> [!IMPORTANT]
> **Pasos Obligatorios mañana temprano:**
> 1. **Backend:** Inicia el servidor Laravel (`php artisan serve --host=0.0.0.0`).
> 2. **IP del Servidor:** Verifica que la IP en `build.gradle.kts` (`BASE_URL_ANDROID`) coincida con la IP de tu computadora (si usas el emulador `10.0.2.2` está bien, si usas un celular real usa la IP de tu red WiFi).
> 3. **Google Services:** No toques el `google-services.json`, ya está sincronizado con el `API_KEY` del mapa.

---

## 5. 🎨 El Factor "WOW"
Destaque estos puntos durante la charla:
*   **Glassmorphism:** Los paneles flotantes que permiten ver el mapa detrás.
*   **Mascota UNAB:** El búho que da la bienvenida en el login.
*   **Estética Uber:** El mapa minimalista que no satura al usuario.
*   **Animaciones:** Todo entra con transiciones suaves, nada aparece de golpe.

---

¡Mucho éxito mañana! La app está en su punto más alto de estabilidad. 🦉🚍✨
