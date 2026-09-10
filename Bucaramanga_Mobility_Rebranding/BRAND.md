# BRAND — Matriz de marca "Bucaramanga Mobility"

> M1 · S1.1.1. Canónico para branding visible. Fuentes: `CONCEPTO.md` (concepto), `MATRIZ_BRANDING.md` (matriz archivo→cambio), código real (paleta). Working name vigente — ver `NAMING_DECISION.md`.

## 1. Nombre

- **Working name (visible, en uso):** **Bucaramanga Mobility**
- **Origen del pivote:** "Bus UNAB" / "VibraBus" (app universitaria de rastreo).
- **Nombre definitivo:** PENDIENTE del usuario (trademark). Ningún rename de package/keystore hasta que se confirme.

## 2. Posicionamiento

> **"El SaaS de movilidad que unifica las transportadoras de Bucaramanga en una sola plataforma digital."**

Para pasajeros: motor de enrutamiento ("¿dónde estás → a dónde vas?") + wallet prepago + QR dinámico. Para transportadoras: panel propio (flota, rutas, conductores, recaudo digital conciliable). Para la plataforma: suscripción SaaS + fee opcional.

## 3. Pilares de marca (lo que prometemos)

1. **Multi-tenant real** — cada transportadora es dueña aislada de sus datos (tenant = `transportadoras`, ver `GLOSSARY.md`).
2. **Wallet prepago** — cero efectivo; flujo de caja anticipado; ledger auditable.
3. **QR dinámico hoy, NFC mañana** — token firmado de un solo uso; el teléfono del conductor es el punto de cobro (mPOS).
4. **Evasión ≈ 0** — el cobro es a bordo, sin molinetes ni estaciones; "colados" casi imposibles.
5. **Cero gasto de infraestructura** — la infraestructura ya existe: el bus.

## 4. Tono y voz

| Aspecto | Definición |
|---|---|
| Personalidad | Práctica, bumquesa, directa; moderna sin ser fría; orgullo local sin folclorismo. |
| Registro | Español (CO), tuteo cercano ("tu bus", "tu saldo"). Técnica interna en ES-EN según convención del repo. |
| En UI | Frases cortas, verbos de acción ("Escanea", "Aborda", "Recarga"); montos siempre en pesos, separador de miles local. |
| En errores | Sin culpar al usuario; causa + siguiente paso ("Saldo insuficiente — recarga para continuar"). |
| Nunca | "estudiante" (excluye), "UNAB" como marca de la plataforma (solo crédito institucional histórico), "monedero" (usar *wallet/billetera*). |
| Palabras marca | transportadora, pasajero, abordaje, pasaje, recarga, paradero, recaudo. |

## 5. Alcance del rebranding

### 5.1 SÍ cambia — branding visible
- Strings de UI de la app (Splash, Login, Perfil, MyQR: títulos, etiquetas, contentDescription decorativos).
- `android:label` del manifiesto Android.
- `brandName` del panel Filament y `APP_NAME` en `.env.example`.
- Documentación visible del repo (README raíz, `Demo_roadmap.md`, `GoogleCloudMigration_Tutorial.md`, esta carpeta).
- Detalle archivo por archivo: `MATRIZ_BRANDING.md`.

### 5.2 NO cambia — CONGELADO (identificadores, no branding)
- `namespace`/`applicationId` **`com.vibra.bus`** y paquetes Kotlin `com.vibra.*`.
- Nombres de clases de código: `VibraBusTheme`, `VibraBusShapes`, etc.
- `frontend/composeApp/google-services.json` (Firebase), `frontend/vibra-bus.jks` (keystore), `GOOGLE_SERVER_CLIENT_ID` en `build.gradle.kts`.
- Contratos de API `/api/v1/*` existentes (compatibilidad con la app instalada).
- Nombres de directorios `bus_unab/` y `frontend/`.
- Motivo: renombrar rompe login Google, firma de releases y usuarios existentes. Renombre solo tras decisión de marca final (`NAMING_DECISION.md`).

## 6. Paleta

| Rol | Provisional (working brand) | UNAB actual (evidencia) |
|---|---|---|
| Primario | Azul-ciudad profundo `#13315C` (propuesta) | Morado oscuro `#1D1B31` · `UnabPurple` (`presentation/theme/Color.kt:8`) |
| Secundario | Amarillo-té de carga `#F2B705` (propuesta) | Morado estándar `#5B2C8C` · `UnabPurpleLight` (`:9`); Naranja `#E9A427` · `UnabOrange` (`:10`) |
| Fondo/Superficie | Mantener neutros claros actuales (`#FFFBF5`/`#FFFFFF`) | `Color.kt`/`Theme` (claro) |
| Panel Filament | Alinear con brand final | `Color::Blue` (`AdminPanelProvider.php:33-35`) |

> Las propuestas "working brand" son **provisionales y no vinculantes** hasta el logo/identidad definitiva (nota PENDIENTE del plan). El crédito institucional UNAB que ya existe en UI queda como menciones históricas intencionadas (`MATRIZ_BRANDING.md §3`), no como paleta de marca.
