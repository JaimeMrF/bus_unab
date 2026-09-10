# Prompt de Rebranding y Reestructuración Arquitectónica

**Instrucciones de Uso:** *Copia todo el texto a continuación y pégalo en el modelo de IA que tiene acceso a tu repositorio de código.*

---

**Contexto del Proyecto y Nuevo Enfoque:**
Actúa como un Tech Lead y Arquitecto de Software Senior. Tienes acceso a todo el código base de nuestro proyecto. Inicialmente, esta aplicación fue diseñada como un sistema para rastrear buses universitarios (un modelo similar a InDrive). Sin embargo, hemos decidido hacer un pivot completo y un rebranding profundo.

**La Problemática:**
Bucaramanga atraviesa una crisis de tráfico severa. La ciudad no cuenta con un sistema de transporte masivo unificado (Metrolínea está inoperante), pero tiene una gran cantidad de buses de empresas tradicionales. El problema principal es la fragmentación: la gente joven desconoce las rutas y el sistema entero funciona exclusivamente con dinero en efectivo, lo que es ineficiente.

**La Nueva Visión:**
Queremos unificar a todas las transportadoras en una sola aplicación para fomentar el uso masivo del bus y disminuir los trancones. Haremos un sistema SaaS de movilidad. A diferencia de otras ciudades, aquí no hay grandes estaciones físicas; la única forma de subir es por el bus directo, lo que reduce la tasa de colados casi a cero y nos evita gastos de infraestructura externa. Todo va directo al grano: movilizarse.

**Requerimientos Técnicos y Arquitectónicos a Analizar:**
Necesito que analices todo el repositorio actual y me indiques qué falta, qué refactorizar y cómo implementar la siguiente lógica:

1. **Estructura Multi-Tenant y Paneles Administrativos (Backend):**
   - Transición a un modelo SaaS. Necesitamos convertir el backend para soportar múltiples inquilinos (Transportadoras). Cada empresa debe tener su propio panel administrativo para gestionar su flota y ganancias.
   - Creación de un panel "Super Admin" exclusivo para los desarrolladores, donde podamos controlar todo el ecosistema.

2. **Experiencia del Usuario (App Pasajeros):**
   - Mantener el motor de enrutamiento: "dónde estás a dónde quieres ir", recomendación de mejores rutas y paradas.
   - **Sistema de Pagos (Wallet):** Implementar una billetera digital donde el usuario recargue saldo (modelo prepago).
   - Generación de un Código QR único/dinámico en pantalla, o soporte para vinculación de tecnología NFC para pagar el pasaje de la cuenta.

3. **App Conductor / Módulo de Cobro:**
   - Crear una interfaz para el conductor que actúe como terminal de cobro. Debe poder escanear el QR del pasajero (usando librerías de cámara optimizadas) o recibir el toque NFC para descontar el saldo inmediatamente.

**Tu Tarea:**
1. Realiza una auditoría del código actual basándote en esta nueva filosofía.
2. Diseña y explícame la nueva estructura de base de datos (colecciones, relaciones o tablas) necesaria para aislar los datos por transportadora y manejar saldos de billetera de forma segura.
3. Define un Roadmap técnico paso a paso para migrar la base sólida que ya tenemos a este nuevo modelo de transporte público para Bucaramanga.
4. Identifica posibles cuellos de botella (ej. reglas de seguridad en la base de datos, latencia en las lecturas de QR/NFC, o sincronización de saldos) y dame tu recomendación técnica para solucionarlos.
