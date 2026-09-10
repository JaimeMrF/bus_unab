# 🚌 Bucaramanga Mobility - Revolucionando el Transporte Público en Bucaramanga

## 🌍 Nueva Filosofía y Visión
Bucaramanga se enfrenta a una crisis de movilidad sin precedentes. El exceso de vehículos particulares y la falta de un sistema de transporte masivo estructurado han generado trancones paralizantes. Aunque la ciudad cuenta con una amplia red de buses tradicionales e informales, la fragmentación, el desconocimiento de las rutas (especialmente en los jóvenes) y la dependencia del dinero en efectivo hacen que el sistema sea ineficiente y subutilizado.

**Nuestra misión es cambiar esto.** Vamos a unificar a todas las empresas transportadoras de la ciudad en una sola plataforma, fomentando el uso del transporte público masivo como la solución principal al tráfico y modernizando una industria olvidada. Haremos el transporte público a nuestra manera: ágil, digital y sin fricciones.

## 🚀 El Enfoque (Rebranding)
Dejamos de ser una herramienta exclusiva de rastreo universitario para convertirnos en el motor de movilidad de toda la ciudad. 

### Ventaja Competitiva y Modelo de Negocio
A diferencia de sistemas como los de Bogotá, que requieren inversiones masivas en estaciones y sufren altas tasas de evasión ("colados"), nuestro modelo aprovecha la infraestructura ya existente: el bus mismo. 
- **0 Gastos en infraestructura externa:** El control de acceso es directo al subir al vehículo.
- **Mínima evasión:** El pasajero paga directamente bajo la supervisión del conductor.
- **Modelo Prepago y Retención:** Los usuarios recargan saldo en la app por adelantado. Esto garantiza un flujo de caja constante y retiene el dinero dentro del ecosistema, abriendo la puerta a un modelo SaaS robusto para las transportadoras.

## 🛠️ Arquitectura y Características Principales

### 1. Ecosistema de Administración (Backend Multi-Tenant)
- **Panel por Transportadora:** Cada empresa de buses tendrá acceso a un entorno aislado (Tenant) para gestionar sus rutas, flota de vehículos, conductores y finanzas de manera independiente.
- **Panel Super Admin:** Un centro de control maestro para nosotros (el equipo Dev), permitiendo la gestión global del sistema, soporte técnico, auditorías y control del modelo SaaS.

### 2. App para Usuarios (Pasajeros)
- **Motor de Enrutamiento Inteligente:** Responde a la pregunta fundamental: *¿Dónde estás y a dónde vas?* La app te indica exactamente qué ruta tomar, a qué paradero ir y cuál es la tarifa.
- **Billetera Digital (Wallet):** Tarjeta virtual recargable integrada en la app. Adiós a buscar monedas o billetes pequeños.
- **Pagos Ágiles:** Pago rápido mediante la generación de un código QR dinámico, o a futuro, mediante tecnología NFC al abordar el bus.

### 3. App para Conductores / Hardware
- **Punto de Cobro (mPOS):** Interfaz ultra rápida, optimizada (usando acceso a la cámara nativa para mínimo lag) para que el conductor escanee el código QR del pasajero y deduzca la tarifa en milisegundos, manteniendo la fluidez en el abordaje.

## 🛣️ El Impacto
Con esta base técnica sólida, no solo optimizamos rutas; reducimos la huella de carbono al incentivar el transporte colectivo, digitalizamos una economía que mueve millones en efectivo a diario y le devolvemos a los bumangueses el tiempo que pierden en el tráfico.

---

## 📌 Estado del pivote

*Este proyecto está en pivote desde "Bus UNAB / VibraBus" hacia un SaaS de movilidad multi-tenant. El nombre visible "Bucaramanga Mobility" es un **nombre de trabajo**; la marca definitiva es decisión pendiente del usuario.*

- 📘 Concepto y pilares: [CONCEPTO.md](CONCEPTO.md) · 🎨 Marca: [BRAND.md](BRAND.md) · 📖 Glosario: [GLOSSARY.md](GLOSSARY.md) · 🏷️ Decisión de nombre: [NAMING_DECISION.md](NAMING_DECISION.md)
- 🗺️ Matriz archivo→cambio del rebranding: [MATRIZ_BRANDING.md](MATRIZ_BRANDING.md)
- ⚠️ El **package técnico de la app sigue siendo `com.vibra.*`** (identificador congelado; no es branding visible — ver §2 de MATRIZ_BRANDING.md).
