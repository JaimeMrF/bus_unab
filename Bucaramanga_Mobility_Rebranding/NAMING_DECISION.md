# NAMING_DECISION — Registro de decisión de nombre de marca

> M1 · S1.1.3. **Estado: CERRADA — decisión confirmada por el usuario: "BUCARATRANSIT".**

## 1. Decisión actual

| Campo | Valor |
|---|---|
| Nombre de trabajo anterior | "Bucaramanga Mobility" |
| Nombre comercial definitivo | ✅ **BUCARATRANSIT** |
| Decidido por | Usuario / dueño del proyecto (2026-09-16). |
| Estado del rebranding visible | ✅ **Completado en Fase A** (UI, Android manifest, paneles Filament, .env, docs). |

## 2. Qué NO cambia mientras la decisión técnica esté congelada

Por seguridad técnica (no por branding):

- `frontend/composeApp/google-services.json` — vinculado al proyecto Firebase/Google Cloud actual; renombrar sin migrar Console rompe el login Google de usuarios existentes.
- `frontend/vibra-bus.jks` — keystore de firma; cambiarlo rompe el flujo de updates en Play Store (misma clave de firma requerida).
- `applicationId`/`namespace` `com.vibra.bus` y paquetes Kotlin `com.vibra.*` — cambiar el package = nueva app a efectos de tiendas y credenciales OAuth (`GOOGLE_SERVER_CLIENT_ID` en `build.gradle.kts`).
- Contratos `/api/v1/*` — la app instalada debe seguir funcionando durante el pivote.

**Regla:** el rename técnico de estos identificadores es un proyecto APARTE (migración Firebase + Play + OAuth), si se decide estrategia de "app nueva vs app renombrada".

## 3. Criterios para elegir el nombre final (checklist para el usuario)

- [x] Registrable como marca en la clase 39 (transporte) ante la SIC — buscar en SIPA.
- [x] Dominio libre (.com y/o .co) y handles de redes disponibles.
- [x] No colisiona con Metrolínea/transmilenio ni nombres de transportadoras locales.
- [x] Pronunciable/escribible en español bumangués; funciona en oralidad ("escanéalo en BucaraTransit").
- [x] No encorseta el alcance (apto para bus, NFC, taxi, BRT).
- [x] Traducible/apto para Play Store y App Store.

## 4. Checklist de rename CUANDO el usuario confirme el nombre final

**Fase A — visible (1 pase, sin riesgos técnicos):**
- [x] Reemplazar "Bucaramanga Mobility" en: `BRAND.md`, `README.md` (raíz y esta carpeta), `CONCEPTO.md`, `GLOSSARY.md`, `MATRIZ_BRANDING.md`.
- [x] `android:label` en `AndroidManifest.xml` (valor visible, no el package) → `BUCARATRANSIT`.
- [x] Strings visibles de UI ya actualizados (Splash/Login/Profile/MyQR) → repasar con el nombre final `BUCARATRANSIT`.
- [x] `brandName()` de ambos paneles Filament y `APP_NAME` en `.env.example`/deploy (`app.yaml`).

**Fase B — técnica (proyecto de migración, decidir con el usuario):**
- [ ] Evaluar: mantener `com.vibra.bus` (recomendado: solo el label visible cambia) vs migrar applicationId (implica app nueva en tiendas).
- [ ] Si se migra: nuevo proyecto Firebase + `google-services.json` renovado, OAuth `GOOGLE_SERVER_CLIENT_ID`, keystore/estrategia de firma, redirecciones de API documentadas.
- [ ] Actualizar esta matriz + `MATRIZ_BRANDING.md` como registro.

## 5. Historial

| Fecha | Evento |
|---|---|
| 2026-09-08 | Working name "Bucaramanga Mobility" adoptado en la misión de pivote; decisión final marcada como pendiente del usuario. |
| 2026-09-16 | **Nombre definitivo confirmado por el usuario: "BUCARATRANSIT".** Fase A de rebranding visible ejecutada en todo el repositorio. |
