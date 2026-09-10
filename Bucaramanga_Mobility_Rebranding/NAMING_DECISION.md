# NAMING_DECISION — Registro de decisión de nombre de marca

> M1 · S1.1.3. **Estado: ABIERTA — decisión pendiente del usuario (dueño del proyecto).**

## 1. Decisión actual

| Campo | Valor |
|---|---|
| Nombre de trabajo (vigente) | **"Bucaramanga Mobility"** |
| Nombre comercial definitivo | ⏸ **PENDIENTE del usuario** |
| Motivo de la espera | Verificación de **trademark** (SIC Colombia), disponibilidad de dominio (.com/.co) y de nombre en Google Play / App Store; la marca define identidad gráfica y inversión de marketing. |
| Quién decide | Usuario/dueño del proyecto. Los agentes NO renombran la marca por su cuenta. |
| Impacto en la misión | **Ninguno bloqueante**: todo el rebranding visible de esta misión usa el working name; el rename final es un pase mecánico sobre la matriz (ver §4). |

## 2. Qué NO cambia mientras la decisión esté abierta (congelado)

Mientras no exista nombre final confirmado quedan congelados, por seguridad técnica (no por branding):

- `frontend/composeApp/google-services.json` — vinculado al proyecto Firebase/Google Cloud actual; renombrar sin migrar Console rompe el login Google de usuarios existentes.
- `frontend/vibra-bus.jks` — keystore de firma; cambiarlo rompe el flujo de updates en Play Store (misma clave de firma requerida).
- `applicationId`/`namespace` `com.vibra.bus` y paquetes Kotlin `com.vibra.*` — cambiar el package = nueva app a efectos de tiendas y credenciales OAuth (`GOOGLE_SERVER_CLIENT_ID` en `build.gradle.kts`).
- Contratos `/api/v1/*` — la app instalada debe seguir funcionando durante el pivote.

**Regla:** el rename técnico de estos identificadores es un proyecto APARTE (migración Firebase + Play + OAuth), solo tras confirmar la marca y decidir estrategia de "app nueva vs app renombrada".

## 3. Criterios para elegir el nombre final (checklist para el usuario)

- [ ] Registrable como marca en la clase 39 (transporte) ante la SIC — buscar en SIPA.
- [ ] Dominio libre (.com y/o .co) y handles de redes disponibles.
- [ ] No colisiona con Metrolínea/transmilenio ni nombres de transportadoras locales.
- [ ] Pronunciable/escribible en español bumangués; funciona en oralidad ("escanéalo en ___").
- [ ] No encorseta el alcance (no "Bus…" si habrá NFC/taxi/BRT; cuidado con "UNAB" si se busca público no universitario).
- [ ] Traducible/apto para Play Store y App Store.

## 4. Checklist de rename CUANDO el usuario confirme el nombre final

**Fase A — visible (1 pase, sin riesgos técnicos):**
- [ ] Reemplazar "Bucaramanga Mobility" en: `BRAND.md`, `README.md` (raíz y esta carpeta), `CONCEPTO.md`, `GLOSSARY.md`, `MATRIZ_BRANDING.md`.
- [ ] `android:label` en `AndroidManifest.xml` (valor visible, no el package).
- [ ] Strings visibles de UI ya actualizados (Splash/Login/Profile/MyQR — M4) → repasar con el nombre final.
- [ ] `brandName()` de ambos paneles Filament y `APP_NAME` en `.env.example`/deploy (`app.yaml`).

**Fase B — técnica (proyecto de migración, decidir con el usuario):**
- [ ] Evaluar: mantener `com.vibra.bus` (recomendado: solo el label visible cambia) vs migrar applicationId (implica app nueva en tiendas).
- [ ] Si se migra: nuevo proyecto Firebase + `google-services.json` renovado, OAuth `GOOGLE_SERVER_CLIENT_ID`, keystore/estrategia de firma, redirecciones de API documentadas.
- [ ] Actualizar esta matriz + `MATRIZ_BRANDING.md` como registro.

## 5. Historial

| Fecha | Evento |
|---|---|
| 2026-09-08 | Working name "Bucaramanga Mobility" adoptado en la misión de pivote; decisión final marcada como pendiente del usuario (esta hoja S1.1.3). |
