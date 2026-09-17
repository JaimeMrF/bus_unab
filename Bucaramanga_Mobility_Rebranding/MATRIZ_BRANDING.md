# MATRIZ_BRANDING — Archivo → Cambio (todo el repo)

> Generada con grep real sobre el repo (patrones `Vibra|VIBRA|VibraBus|Bus UNAB|UNAB`, case-sensitive). Marca definitiva confirmada: **"BUCARATRANSIT"** (anterior working name: "Bucaramanga Mobility").
> Fases según `.opencode/todo.md`: **M1** = docs de esta carpeta · **M4** = rebranding visible · **LÓGICA** = no es string, es regla de negocio (fuera de rebranding, va al pivote multi-tenant).

---

## 1. Matriz de cambios

### 1.1 Docs raíz y backend (markdown / config)

| Archivo | Cadena/elemento actual (línea) | Cambio propuesto / aplicado | Fase |
|---|---|---|---|
| `README.md` (raíz) | `# 🚌 Bus UNAB — VibraBus` (L1) | `# 🚌 BUCARATRANSIT` (nota "antes Bus UNAB — VibraBus") | M4 ✔ |
| `README.md` (raíz) | `APP_NAME="Bus UNAB"` en bloque .env (L44) | `APP_NAME="BUCARATRANSIT"` | M4 ✔ |
| `README.md` (raíz) | `Proyecto desarrollado por el equipo **VIBRA+** — Universidad Autónoma de Bucaramanga (UNAB).` (L220) | Reubicar como crédito histórico ("origen del proyecto: UNAB / equipo VIBRA+") sin marca visible activa | M4 ✔ |
| `bus_unab/README.md` | Sin cadenas de marca (es el README por defecto de Laravel — verificado por grep: 0 coincidencias) | Encabezado actualizado a BUCARATRANSIT | M4 ✔ |
| `bus_unab/PRESENTACION.md` | `# Script de Presentacion — Bus UNAB` (L1); `"Presentamos **Bus UNAB**…"` (L8); `"El proveedor GPS de UNAB…"` (L46) | **Mención histórica intencionada** (script académico de chaos engineering) — ver §3 | Mención histórica |
| `Demo_roadmap.md` | `# 🚀 Roadmap de Demostración: Bus UNAB Vibra+` (L1) | Título → "BUCARATRANSIT" | M4 ✔ |
| `Demo_roadmap.md` | `### Opción B: Credenciales UNAB` (L13); `El Corazón de VIBRA+` (L22); `**Mascota UNAB:** El búho…` (L61) | Etiquetas visibles → marca BUCARATRANSIT; cuentas seed se quedan (ver §3) | M4 |
| `GoogleCloudMigration_Tutorial.md` | `# 🗺️ Plan Maestro: Migración Total a Google Cloud (Bus UNAB VIBRA+)` (L1) | Título → "BUCARATRANSIT" | M4 ✔ |
| `GoogleCloudMigration_Tutorial.md` | `crea \`Bus-UNAB-Final\`` (L8); `con el nombre \`Bus UNAB\`` (pantalla de consentimiento, L19) | Nombres de recurso GCP → marca working **PENDIENTE**: afectan OAuth/Firebase (tema API keys pospuesto) | M4 + nota pendiente |
| `bus_unab/.env.example` | `APP_NAME=Laravel` (L1); `MAIL_FROM_NAME="${APP_NAME}"` (L60); `VITE_APP_NAME="${APP_NAME}"` (L68) — las dos últimas heredan | `APP_NAME="BUCARATRANSIT"` (L60/L68 no se tocan, ya heredan) | M4 ✔ |
| `bus_unab/config/app.php` | `'name' => env('APP_NAME', 'Laravel')` (L16) — sin marca hardcodeada | Sin cambio (se resuelve vía `.env`) | — |
| `bus_unab/app/Providers/Filament/AdminPanelProvider.php` | `->brandName('Bus UNAB — Admin')` (L32) | `->brandName('BUCARATRANSIT')` (coord. con panel Super Admin M3) | M4 ✔ |
| `bus_unab/routes/api.php` | Comentario `API Routes — Bus UNAB v1` (L15) | Comentario histórico; **rutas `/api/v1/*` NO se toman** | M4 (congelado) |
| `bus_unab/app/Filament/Resources/PointOfInterestResource.php` | Opciones `'campus' => 'Campus UNAB'` (L55, L98, L117) | Values de filtro demo → tenant-neutral (p.ej. nombre de transportadora) — es dato, no marca de app | M3/M4 |

### 1.2 Frontend (app KMP)

| Archivo | Cadena/elemento actual (línea) | Cambio propuesto / aplicado | Fase |
|---|---|---|---|
| `frontend/composeApp/src/androidMain/AndroidManifest.xml` | `android:label="VIBRA+ Bus UNAB"` (L18) | `android:label="BUCARATRANSIT"` (label visible del launcher) | M4 ✔ |
| `frontend/.../screens/SplashScreen.kt` | `text = "VIBRA+"` (L92); `text = "Bus UNAB"` (L99) | Textos visibles → marca BUCARATRANSIT | M4 |
| `frontend/.../screens/LoginScreen.kt` | `text = "Bus UNAB"` (L194); `text = "VIBRA+"` (L201); `text = "⭐  ESTUDIANTES UNAB"` (L253); `contentDescription = "Logo UNAB"` (L182); comentario (L164) | Actualizado a BUCARATRANSIT ("⭐  TODA BUCARAMANGA") | M4 ✔ |
| `frontend/.../screens/ProfileScreen.kt` | `QuickStat(value = "UNAB", label = "Universidad")` (L202); `Text("VibraBus v1.0.0", …)` (L332) | QuickStat → dato neutral; footer → `"BUCARATRANSIT v1.0.0"` (clase `VibraBusTheme` NO se toca) | M4 ✔ |
| `frontend/.../screens/MyQRScreen.kt` | `contentDescription = "Búho UNAB"` (L163) | `"Mascota BUCARATRANSIT"` / `"Avatar"` | M4 ✔ |
| `frontend/.../screens/WaitingBusScreen.kt` | Diálogo: `"…desactives la optimización de batería para VibraBus. "` (L95) | Solo el texto literal → marca BUCARATRANSIT (el identifier `VibraBusShapes` de L30/L200/L285/L310 NO se toca) | M4 |
| `frontend/.../res/values/strings.xml` | **No existe** (solo hay `themes.xml` en `res/values/`) | Opcional M4: crear `app_name` y referenciarlo desde `android:label` (buena práctica, no obligatoria) | M4 (opcional) |
| `frontend/iosApp/` | Sin `Info.plist` versionado (solo `ContentView.swift`, `iOSApp.swift`, `Podfile`; grep de marca: 0 coincidencias) | Nada que renombrar en el repo; el display name iOS se gestiona en el proyecto Xcode que se genere — nota pendiente | — |
| `frontend/composeApp/src/commonMain/composeResources/` | Assets: `logo_unab_blanco_transparente.webp`, `buhosaludologin.webp`, `buho_*.webp` (9 ilustraciones de mascota) + fuentes | Nombres de archivo son referenciados por código → **congelados**; el **arte** (logo UNAB, búho) queda **PENDIENTE** de rediseño de marca (mismo nombre de archivo o tarea futura de rename) | Pendiente (diseño) |
| `frontend/settings.gradle.kts` | `rootProject.name = "VibraBus"` (L1) | Identificador de proyecto Gradle (no visible al usuario) → congelado por ahora; rename opcional tras decisión de nombre | Congelado (ver §2) |

### 1.3 Backend — datos semilla y lógica (NO rebranding puro)

| Archivo | Cadena/elemento actual (línea) | Cambio propuesto / aplicado | Fase |
|---|---|---|---|
| `bus_unab/database/seeders/AdminSeeder.php` | `'Administrador UNAB'`, `admin@unab.edu.co` (L14, L16–17) | **Mención histórica intencionada** (§3): credenciales de desarrollo documentadas en README/context.md; cambiarlas rompería docs y scripts de demo | Mención histórica |
| `bus_unab/database/seeders/DemoUserSeeder.php` | `estudiante@unab.edu.co` (L15), `conductor@unab.edu.co` (L25) | Igual: credenciales demo congeladas hasta M3 (se re-sembrarán con tenants) | Mención histórica |
| `bus_unab/database/seeders/StopSeeder.php` / `Route2Seeder.php` | `'UNAB Campus Principal'` (Stop L13); paradas con "UNAB" en address (Route2 L17, L22, L29) | Datos de rutas universitarias reales del demo → se re-escriben con la semilla multi-tenant de M3 (TransportadoraSeeder) | M3 (datos) |
| `bus_unab/app/Http/Controllers/Api/V1/AuthController.php` | `if (! str_ends_with($email, '@unab.edu.co'))` + mensaje `"Solo se permiten cuentas institucionales @unab.edu.co"` (L36–37) | **No es branding: es regla de negocio** que bloquea el pivote (solo estudiantes UNAB pueden registrarse). Se elimina/replace con multi-tenant en M3 (roles/tenants). Registrar en auditoría, NO en el pase de strings | LÓGICA (M3) |
| `Bucaramanga_Mobility_Rebranding/README.md` | Título con `[Nombre de tu App]` | ✅ Cambiado a "BUCARATRANSIT" | M1 / Branding ✔ |

---

## 2. NO TOCAR (congelado — identifiers y credenciales técnicas)

Verificado por grep; **cero diff obligatorio** en toda la misión:

| Elemento | Ubicación (línea verificada) | Razón del congelamiento |
|---|---|---|
| Paquete/`namespace`/`applicationId` `com.vibra.bus` | `frontend/composeApp/build.gradle.kts` (L137, L141) y todos los `package com.vibra.*` / `import com.vibra.*` | Cambiar applicationId = otra app en Play Store (rompe updates de installs existentes) |
| `google-services.json` | `frontend/composeApp/google-services.json` | Vinculado al `package_name` `com.vibra.bus` + OAuth; renombrar rompe Firebase/Google Sign-In (API keys = tema pendiente del usuario) |
| `GOOGLE_SERVER_CLIENT_ID` | `frontend/composeApp/build.gradle.kts` (L149) | Client ID OAuth de Google literal; pertenece al proyecto GCP actual |
| Keystore `vibra-bus.jks` | `frontend/composeApp/build.gradle.kts` (L178, vía `KEYSTORE_FILE`) | Firma de la app publicada; renombrar invalida updates |
| Nombres de clase Kotlin | `VibraBusTheme`, `VibraBusShapes`, `VibraBusThemeUtils` (≈50 referencias en `presentation/screens`, `components`, `theme`), `VibraFirebaseMessagingService` (`AndroidManifest.xml` L34), `@style/Theme.VibraBus` (`AndroidManifest.xml` L20 + `res/values/themes.xml` L3), `rootProject.name = "VibraBus"` (`settings.gradle.kts` L1) | Son identificadores de código/estilo, no branding visible al usuario; renombrarlos es refactor masivo sin beneficio de marca |
| Rutas API `/api/v1/*` | `bus_unab/routes/api.php` (todas las rutas) | Contratos con app publicada; romperían clientes Android/iOS en el campo |
| Nombres de directorios `bus_unab/`, `frontend/` | raíz del repo | Rutas de CI, docker, App Engine (`bus_unab/app.yaml`) y scripts existentes |

## 3. Menciones históricas intencionadas (se conservan, con contexto)

Estas apariciones de "UNAB/VIBRA" **no son deuda de rebranding**: documentan el origen académico del proyecto y siguen siendo ciertas. Regla: mantenerlas re-enmarcadas como "origen del proyecto", nunca borrarlas (pierden trazabilidad).

- `bus_unab/PRESENTACION.md` (L1, L8, L46) — script de presentación académica (Arquitectura de Software / chaos engineering): habla de **Bus UNAB** porque *esa fue* la entrega universitaria.
- Credenciales seed `admin@unab.edu.co`, `estudiante@unab.edu.co`, `conductor@unab.edu.co` / `password123` — documentadas en `README.md`, `Demo_roadmap.md` (L13–18) y `.opencode/context.md`; son contratos de desarrollo/demo hasta que M3 las reemplace con semilla multi-tenant.
- Paradas del seed con nombre "UNAB" (`StopSeeder` L13, `Route2Seeder` L17/L22/L29) — geografía real del campus en la demo actual; se migran con TransportadoraSeeder (M3).
- Sección Firebase del `README.md` raíz que referencia `com.vibra` — instrucción técnica exacta, debe coincidir con el código congelado (§2).
- `frontend/disenio_unab/` (`UI_Style_Guide.md` L1/L21/L22/L29/L42/L362/L684; `components-guide.md` L1/L18/L31/L772) — archivo histórico de diseño del "Bus UNAB Vibra+" original: mantener como referencia de sistema visual (glassmorphism/silver) con encabezado "documento histórico del rediseño" en M4; el nuevo design system se derivará de aquí, no se borra.

---

### Evidencia grep usada (recuentos reales, 2026-09-08)

Patrón `Vibra|VIBRA|Bus UNAB|UNAB`: `README.md`=3 · `bus_unab/*.md`=3 (todos en PRESENTACION.md) · `Demo_roadmap.md`=4 · `GoogleCloudMigration_Tutorial.md`=3 · `frontend/disenio_unab/*.md`=11 · `presentation/screens/*.kt`=60 (≈45 identifiers + 10 strings visibles + 1 diálogo) · `androidMain/*.xml`=4 · `*.kts`=4 · `database/seeders`=9 · `app/**/*.php`=3 archivos (AuthController 2, POI 3, AdminPanelProvider 1) · `routes/api.php`=1 · `iosApp`=0 · `config/app.php`=0 (usa `env('APP_NAME')`) · `composeResources`=8 nombres de asset con `buho`/`unab`.
