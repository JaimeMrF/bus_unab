He preparado un archivo `.md` **mucho más completo y profesional**, pensado como un **Design System / UI Style Guide** para que tu proyecto **Bus UNAB Vibra+** mantenga consistencia visual ahora y en futuras pantallas.

Incluye:

* Identidad visual de la app
* Paleta de colores oficial
* Tipografía y jerarquías
* Espaciados y grid system
* Componentes reutilizables (botones, cards, inputs, navbar, sidebar)
* Patrones de mapas y rutas
* Estados de interacción
* Patrones de UX para conductor y estudiante
* Animaciones
* Reglas para nuevas pantallas
* Tokens listos para usar en código
* Convenciones para que Codex pueda escalar la UI manteniendo armonía

Aquí está el archivo:

````md
# Bus UNAB Vibra+ — Design System & UI Pattern Guide
> Guía maestra de diseño visual, componentes y patrones UX/UI para la aplicación **Bus UNAB Vibra+**  
> Objetivo: garantizar consistencia visual y escalabilidad para nuevas pantallas sin romper la armonía del producto.

---

# 1. IDENTIDAD VISUAL

La aplicación **Bus UNAB Vibra+** debe transmitir:

- Modernidad
- Cercanía
- Rapidez
- Seguridad
- Identidad universitaria
- Estética amigable y juvenil

Inspiración visual:

- Uber / Beat / Cabify para mapas y seguimiento
- Duolingo para cercanía amigable
- Branding UNAB + mascota “Búho Vibra+”

---

# 2. PRINCIPIOS DE DISEÑO

Toda nueva pantalla debe respetar estos principios:

## 2.1 Claridad
El usuario debe entender la acción principal en menos de 3 segundos.

## 2.2 Jerarquía Visual
Los elementos más importantes deben destacar por:
- tamaño
- contraste
- color
- posición

## 2.3 Consistencia
Botones, tarjetas, inputs y navegación deben comportarse igual en todas las pantallas.

## 2.4 Cercanía visual
Usar bordes redondeados, ilustraciones amigables y colores cálidos.

---

# 3. PALETA DE COLORES OFICIAL

## Primarios

### Morado principal
Uso: fondos principales, header, sidebar

```css
#5B2C8C
````

### Morado secundario

Uso: gradientes y acentos

```css
#8600DD
```

### Naranja institucional

Uso: CTA principales, rutas activas, indicadores

```css
#E9A427
```

---

## Neutros

### Blanco

```css
#FFFFFF
```

### Texto oscuro

```css
#1A1A1A
```

### Gris claro

```css
#EDEDED
```

### Gris texto secundario

```css
#8E8E8E
```

---

## Estados

### Éxito

```css
#2EBE6C
```

### Advertencia

```css
#FFB020
```

### Error

```css
#E5484D
```

---

# 4. GRADIENTES OFICIALES

## Fondo principal login

```css
linear-gradient(180deg, #5B2C8C 0%, #8600DD 100%)
```

## Botón principal

```css
linear-gradient(90deg, #E9A427 0%, #F2B53D 100%)
```

---

# 5. TIPOGRAFÍA

Fuente oficial:

```css
Poppins
```

Pesos:

* 400 Regular
* 500 Medium
* 600 SemiBold
* 700 Bold

---

## Escala tipográfica

### Título principal

```css
font-size: 28px;
font-weight: 700;
```

### Título secundario

```css
font-size: 22px;
font-weight: 600;
```

### Texto base

```css
font-size: 16px;
font-weight: 400;
```

### Texto auxiliar

```css
font-size: 13px;
font-weight: 400;
```

---

# 6. SISTEMA DE ESPACIADO

Base unit: **8px**

```txt
xs = 4
sm = 8
md = 16
lg = 24
xl = 32
xxl = 40
```

Regla:

* padding general pantalla: `16px`
* separación entre cards: `16px`
* separación interna card: `16px`

---

# 7. BORDER RADIUS

## Inputs

```css
12px
```

## Botones

```css
14px
```

## Cards

```css
18px
```

## Modales

```css
24px
```

---

# 8. SOMBRAS

## Cards principales

```css
0 8px 24px rgba(0,0,0,0.12)
```

## Elemento flotante

```css
0 12px 32px rgba(0,0,0,0.18)
```

---

# 9. COMPONENTES BASE

---

# 9.1 BOTÓN PRIMARIO

Uso:

* Iniciar sesión
* Confirmar parada
* Escanear QR

```css
height: 52px;
border-radius: 14px;
background: #E9A427;
color: white;
font-weight: 600;
```

Estados:

* normal
* pressed → 95% brightness
* disabled → 40% opacity

---

# 9.2 BOTÓN SECUNDARIO

```css
background: white;
border: 1px solid #ddd;
color: #1A1A1A;
```

---

# 9.3 INPUTS

```css
height: 50px;
background: white;
border-radius: 12px;
padding: 0 16px;
```

Estados:

* focus: borde morado
* error: borde rojo

---

# 9.4 CARDS DE RUTAS

Cada ruta debe mostrarse en card vertical:

Contenido:

* ícono bus
* nombre ruta
* color indicador
* estado

```css
border-radius: 18px;
padding: 16px;
background: white;
```

Distribución:

```txt
3 columnas
1 fila
```

---

# 10. PANTALLA DE LOGIN

Debe contener:

1. Mascota búho en parte superior
2. Logo Bus UNAB Vibra+
3. Inputs de acceso
4. CTA principal
5. Login con Google

Distribución vertical centrada:

```txt
Mascota
Logo
Inputs
Botón
Google
```

---

# 11. PANTALLA HOME MAPA

Patrón:

```txt
Header fijo
Mapa fondo
Bottom sheet flotante
```

---

## Header

Contiene:

* saludo
* menú hamburguesa

Altura:

```css
64px
```

---

## Mapa

Ocupa:

```css
60% de pantalla
```

---

## Bottom Sheet

Contiene rutas:

```css
border-radius: 24px 24px 0 0;
```

---

# 12. TARJETAS DE RUTA

Cada ruta:

* ícono bus isométrico
* nombre
* estado
* tiempo estimado

Estados:

### Disponible

Verde

### Próximo

Naranja

### Inactivo

Gris

---

# 13. FLUJO DE SELECCIÓN DE PARADA

Luego de elegir ruta:

Mostrar lista:

```txt
Parada 1
Parada 2
Parada 3
Parada 4
```

Cada parada debe mostrar:

* nombre
* personas esperando
* tiempo estimado

---

# 14. MAPA EN VIVO

Mostrar:

* ruta naranja
* marcador de parada
* bus en movimiento

Colores:

Ruta:

```css
#E9A427
```

Parada:

```css
#5B2C8C
```

Bus:

```css
#FF8A00
```

---

# 15. NOTIFICACIONES TIPO UBER

Formato:

### Header

* icono bus
* título
* tiempo

### Body

Mensaje breve

Ejemplos:

* “Tu bus está llegando”
* “El bus llegó a tu parada”

---

# 16. MENÚ LATERAL

Opciones:

1. Inicio
2. Mi QR
3. Mis viajes
4. Notificaciones
5. Perfil
6. Cerrar sesión

Diseño:

* fondo blanco
* header morado con avatar

---

# 17. PANTALLA QR

Elementos:

1. Avatar búho
2. Código QR
3. Nombre usuario
4. Código estudiante

Card centrada:

```css
border-radius: 20px;
padding: 24px;
```

---

# 18. PANEL CONDUCTOR

El conductor ve:

1. Ruta activa
2. Lista de paradas
3. Personas esperando
4. Escáner QR

---

## Card parada conductor

Debe mostrar:

* parada
* cantidad personas

Indicador visual:

```css
verde: baja demanda
naranja: media
rojo: alta
```

---

# 19. ANIMACIONES

Duración estándar:

```css
250ms
```

Tipos:

### Hover / press

scale 0.98

### Entrada cards

fade + translateY(8px)

### Actualización bus

smooth translate

---

# 20. REGLAS PARA NUEVAS PANTALLAS

Toda pantalla nueva debe cumplir:

## Layout:

* Header fijo
* Contenido principal
* Acción principal visible

## Colores:

* Morado para estructura
* Naranja para CTA

## Componentes:

Usar componentes existentes antes de crear nuevos.

---

# 21. DESIGN TOKENS PARA CÓDIGO

```ts
export const theme = {
  colors: {
    primary: "#5B2C8C",
    secondary: "#8600DD",
    accent: "#E9A427",
    white: "#FFFFFF",
    text: "#1A1A1A",
    success: "#2EBE6C",
    warning: "#FFB020",
    error: "#E5484D"
  },
  radius: {
    sm: 12,
    md: 18,
    lg: 24
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32
  }
}
```

---

# 22. REGLA MAESTRA DE CONSISTENCIA

Si se crea una pantalla nueva:

1. reutilizar paleta
2. reutilizar tipografía
3. reutilizar spacing
4. reutilizar cards
5. mantener CTA naranja
6. mantener jerarquía clara

**Toda nueva vista debe sentirse parte de la misma familia visual.**

---

# 23. VISIÓN DE EXPERIENCIA

La app debe sentirse:

* rápida como Uber
* amigable como Duolingo
* institucional como UNAB
* simple para estudiantes
* eficiente para conductores

---

# 24. DIRECTRIZ FINAL PARA CODEX

Cuando se genere una nueva pantalla:

1. usar `Poppins`
2. usar `#5B2C8C` como base
3. CTA en `#E9A427`
4. cards blancas con radius `18px`
5. sombras suaves
6. jerarquía visual clara
7. diseño mobile first
8. consistencia con mapas y rutas existentes

**Si un nuevo diseño rompe esta armonía, debe corregirse antes de implementarse.**

```

Si quieres, ahora puedo ayudarte a crear el siguiente archivo complementario:

**`components-guide.md`**, donde se define **cada componente reusable en Flutter/React Native** (RouteCard, StopCard, QRCard, DriverStopCard, NotificationCard, etc.) con props, estados y estructura visual para que Codex ya pueda **generar código automáticamente componente por componente** sin perder el diseño.
```
