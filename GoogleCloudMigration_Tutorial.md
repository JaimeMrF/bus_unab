# 🗺️ Plan Maestro: Migración Total a Google Cloud (BUCARATRANSIT)

> **Proyecto en evolución a BUCARATRANSIT** — ver README de [`Bucaramanga_Mobility_Rebranding/`](Bucaramanga_Mobility_Rebranding/README.md). El flujo descrito corresponde al sistema Bus UNAB anterior; los nombres de recursos GCP citados son literales y están **pendientes de renombrar** (afectan OAuth/Firebase — tema de API keys pospuesto).

Esta es la guía definitiva para mover todo tu ecosistema (Backend, Base de Datos, Mapas y Auth) a Google Cloud desde cero.

---

## 🏁 Fase 1: El Cimiento (Google Cloud Console)
1.  **Crear Proyecto:** Entra a [GCP Console](https://console.cloud.google.com/) y crea `Bus-UNAB-Final`. *(nombre de recurso histórico; renombrar pendiente tras decisión de marca)*
2.  **Billing:** Vincula tu tarjeta en la sección **Facturación**. (Recuerda: 200 USD/mes gratis).
3.  **Habilitar APIs:** Busca y activa:
    *   `Maps SDK for Android`
    *   `Cloud SQL Admin API`
    *   `App Engine Admin API`
    *   `Identity Toolkit API`

---

## 🔑 Fase 2: Identidad y Seguridad (OAuth & SHA-1)
1.  **Pantalla de Consentimiento:** Configúrala como "Externa" con el nombre `Bus UNAB`. *(nombre OAuth histórico; renombrar pendiente — afecta cuentas ya conectadas)*
2.  **Credenciales:**
    *   **API Key:** Crea una "Clave de API" para los mapas. Restríngela a tu paquete `com.vibra.bus` y SHA-1: `CC:7B:3E:28:88:8F:70:5C:F2:1E:92:4C:BC:73:62:CE:4D:13:C6:90`.
    *   **ID Cliente OAuth (Web):** Crea uno para el **Backend**. Anota el ID.
    *   **ID Cliente OAuth (Android):** Crea uno para la **App**, usando el mismo SHA-1 y paquete.

---

## 🗄️ Fase 3: La Base de Datos (Cloud SQL)
*Nota: Para Laravel, usaremos Cloud SQL (MySQL) en lugar de Firestore para mantener la compatibilidad total de tu código sin reescribir nada.*

1.  Ve a **Cloud SQL** > **Crear instancia** > **MySQL**.
2.  Nombre: `vibra-db`. Pon una contraseña fuerte.
3.  En "Configuración", elige el nivel **Micro/Small** para ahorrar crédito.
4.  Una vez creada, crea una base de datos llamada `bus_unab_prod`.

---

## 🚀 Fase 4: Despliegue del Backend (Laravel)
En la carpeta raíz de tu backend, crearemos estos archivos clave:

### 1. Crear `app.yaml`
```yaml
runtime: php82
instance_class: F1
env_variables:
  APP_ENV: production
  APP_KEY: TU_APP_KEY_AQUI
  APP_DEBUG: false
  DB_CONNECTION: mysql
  DB_SOCKET: /cloudsql/TU_CONNECTION_NAME # Se obtiene de la consola de Cloud SQL
  DB_DATABASE: bus_unab_prod
  DB_USERNAME: root
  DB_PASSWORD: TU_PASSWORD
  GOOGLE_CLIENT_ID: TU_ID_CLIENTE_WEB
```

### 2. Comando de Vuelo:
```bash
gcloud app deploy
```
Al final, obtendrás tu URL: `https://tu-app.appspot.com`

---

## 📱 Fase 5: Conectar la App (Frontend)
1.  **build.gradle.kts:** Cambia `BASE_URL_ANDROID` por tu nueva URL de App Engine.
2.  **google-services.json:** 
    *   Ve a [Firebase Console](https://console.firebase.google.com/), vincula el proyecto de Google Cloud.
    *   Registra la app de Android con el SHA-1.
    *   Descarga el archivo y reemplázalo en `frontend/composeApp/`.

---

## ✅ Fase 6: Checkpoint Final (Para Mañana)
Desde una terminal local con el gcloud SDK:
1.  **Migraciones:** `gcloud app instances ssh ... --command="php artisan migrate --force"`
2.  **Seeders:** `gcloud app instances ssh ... --command="php artisan db:seed --class=DemoUserSeeder --force"`

---

**¡Felicidades!** Tu sistema estará 100% en la nube, con bases de datos reales, mapas funcionando y login de Google activado. 🦉🌐✨
