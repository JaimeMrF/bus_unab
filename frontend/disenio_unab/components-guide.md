Perfecto. Aquí tienes el segundo archivo: **`components-guide.md`**, pensado para que **Codex pueda construir la UI componente por componente**, manteniendo la armonía visual de **Bus UNAB Vibra+**.

Este documento define:

* Componentes reutilizables
* Props recomendadas
* Estados visuales
* Jerarquía interna
* Reglas de diseño por componente
* Comportamiento esperado
* Convenciones de nombres

Esto hará que cuando generes nuevas vistas, Codex reutilice la misma lógica visual.

---

````md id="a7d92f"
# Bus UNAB Vibra+ — Components Guide
> Guía de componentes reutilizables para mantener consistencia visual y estructural en toda la app.

---

# 1. FILOSOFÍA DE COMPONENTES

Todos los componentes deben ser:

- Reutilizables
- Modulares
- Consistentes
- Responsivos
- Visualmente alineados al branding Vibra+

Cada componente debe respetar:

1. colores del design system
2. tipografía Poppins
3. radios definidos
4. spacing uniforme
5. estados claros

---

# 2. ESTRUCTURA GENERAL

Cada componente debe definirse así:

```ts
ComponentName
Props
Variants
States
Styles
Usage
````

---

# 3. BUTTON COMPONENTS

---

# 3.1 PrimaryButton

Botón principal para acciones críticas:

* iniciar sesión
* confirmar parada
* escanear QR
* continuar

---

## Props

```ts
{
  label: string
  onPress: () => void
  disabled?: boolean
  loading?: boolean
  icon?: ReactNode
}
```

---

## Estilos base

```ts
height: 52
borderRadius: 14
backgroundColor: accent
paddingHorizontal: 16
```

---

## Estados

### Normal

Fondo naranja

### Disabled

40% opacity

### Loading

Spinner blanco centrado

### Pressed

Scale 0.98

---

---

# 3.2 SecondaryButton

Acciones secundarias:

* cancelar
* volver
* editar

---

## Props

```ts
{
  label: string
  onPress: () => void
}
```

---

## Estilos

```ts
backgroundColor: white
borderWidth: 1
borderColor: neutral
```

---

# 4. INPUT COMPONENTS

---

# 4.1 TextInputField

Campo reutilizable para login y formularios.

---

## Props

```ts
{
  placeholder: string
  value: string
  onChange: (value:string)=>void
  secure?: boolean
  iconLeft?: ReactNode
  iconRight?: ReactNode
  error?: string
}
```

---

## Estados

### Normal

Borde gris

### Focus

Borde morado

### Error

Borde rojo + mensaje

---

# 5. CARDS

---

# 5.1 RouteCard

Representa una ruta de bus.

---

## Props

```ts
{
  routeName: string
  eta: string
  status: "available" | "incoming" | "inactive"
  busIcon: string
  onSelect: () => void
}
```

---

## Layout

```txt
[icono bus]
Ruta 1
Llegada en 5 min
estado
```

---

## Estados visuales

### available

Indicador verde

### incoming

Indicador naranja

### inactive

Indicador gris

---

---

# 5.2 StopCard

Representa una parada del usuario.

---

## Props

```ts
{
  stopName: string
  waitingPeople: number
  eta: string
  selected: boolean
}
```

---

## Layout

```txt
Nombre parada
Personas esperando
Tiempo estimado
```

---

## Estado seleccionado

* borde morado
* fondo claro

---

---

# 5.3 DriverStopCard

Parada vista desde conductor.

---

## Props

```ts
{
  stopName: string
  waitingPeople: number
  priority: "low" | "medium" | "high"
}
```

---

## Colores prioridad

### low

Verde

### medium

Naranja

### high

Rojo

---

# 6. MAP COMPONENTS

---

# 6.1 LiveMapView

Mapa en tiempo real.

---

## Props

```ts
{
  routePolyline: Coordinates[]
  busPosition: Coordinates
  stops: Stop[]
}
```

---

## Elementos visuales

1. Ruta naranja
2. Paradas moradas
3. Bus animado

---

## Animación

Movimiento suave:

```ts
duration: 1000ms
```

---

# 6.2 RouteSelectorGrid

Grid de rutas disponible.

---

## Props

```ts
{
  routes: Route[]
}
```

---

## Layout

```txt
3 columnas
1 fila
```

Cada item usa `RouteCard`

---

# 7. NAVIGATION COMPONENTS

---

# 7.1 SideMenuDrawer

Menú lateral.

---

## Props

```ts
{
  user: User
  onNavigate: (screen)=>void
}
```

---

## Opciones

* Inicio
* Mi QR
* Mis viajes
* Notificaciones
* Perfil
* Cerrar sesión

---

## Layout

Header morado con avatar + lista blanca

---

# 7.2 BottomTabBar

Barra inferior.

---

## Tabs

* Inicio
* QR
* Viajes
* Perfil

---

## Estado activo

Ícono naranja

---

# 8. QR COMPONENTS

---

# 8.1 QRCard

Muestra QR del estudiante.

---

## Props

```ts
{
  qrData: string
  studentName: string
  studentCode: string
}
```

---

## Layout

```txt
Avatar
QR
Nombre
Código
```

---

---

# 8.2 QRScannerPanel

Vista conductor para escaneo.

---

## Props

```ts
{
  onScan: (data)=>void
}
```

---

## Estados

### Idle

Marco escáner visible

### Scanned

Feedback éxito

### Error

Feedback rojo

---

# 9. NOTIFICATIONS

---

# 9.1 NotificationCard

Notificaciones tipo Uber.

---

## Props

```ts
{
  title: string
  message: string
  time: string
  type: "info" | "arrival" | "warning"
}
```

---

## Variantes

### arrival

icono bus + naranja

### info

azul suave

### warning

amarillo

---

# 10. HEADER COMPONENTS

---

# 10.1 AppHeader

Header reutilizable.

---

## Props

```ts
{
  title: string
  showMenu?: boolean
  showBack?: boolean
}
```

---

## Layout

```txt
[menu/back]   title
```

---

# 11. FEEDBACK COMPONENTS

---

# 11.1 StatusBadge

Indicador pequeño.

---

## Props

```ts
{
  status: string
  color: string
}
```

---

## Uso

* En ruta
* Llegando
* Completo

---

# 12. LAYOUT COMPONENTS

---

# 12.1 BottomSheetPanel

Panel flotante inferior.

---

## Props

```ts
{
  children: ReactNode
}
```

---

## Estilos

```ts
borderTopLeftRadius: 24
borderTopRightRadius: 24
padding: 16
shadow: elevated
```

---

# 13. SCREEN COMPOSITIONS

---

# 13.1 Home Screen

Composición:

```txt
AppHeader
LiveMapView
BottomSheetPanel
RouteSelectorGrid
BottomTabBar
```

---

# 13.2 Route Detail Screen

```txt
AppHeader
LiveMapView
StopCard List
PrimaryButton
```

---

# 13.3 Driver Screen

```txt
AppHeader
RouteCard
DriverStopCard List
QRScannerPanel
BottomTabBar
```

---

# 14. NAMING CONVENTIONS

Componentes:

```txt
PascalCase
```

Ejemplo:

```txt
RouteCard.tsx
StopCard.tsx
PrimaryButton.tsx
```

Props:

```txt
camelCase
```

---

# 15. RESPONSIVE RULES

Diseñar mobile-first:

```txt
base width: 375px
```

Reglas:

* cards flexibles
* paddings proporcionales
* evitar tamaños fijos innecesarios

---

# 16. ANIMATION RULES

Todos los componentes interactivos deben tener feedback visual.

### Press

```ts
scale: 0.98
```

### Enter

```ts
fade + slideUp
```

### Map updates

```ts
smooth transition
```

---

# 17. ACCESSIBILITY

Todo componente debe tener:

* contraste correcto
* labels accesibles
* targets mínimos de 44px

---

# 18. GOLDEN RULE

Si una nueva pantalla necesita UI nueva:

1. reutilizar componentes existentes
2. si no existe, crear nuevo componente siguiendo esta guía
3. nunca romper colores, spacing ni jerarquía visual

---

# 19. RECOMENDACIÓN FINAL PARA CODEX

Antes de crear una nueva vista:

1. identificar componentes reutilizables
2. reutilizar `RouteCard`, `StopCard`, `PrimaryButton`, `AppHeader`
3. respetar theme tokens
4. mantener experiencia Uber-like + identidad UNAB

La interfaz debe sentirse:

* rápida
* institucional
* amigable
* coherente

**Cada pantalla nueva debe parecer diseñada dentro del mismo sistema.**

```

---

Con esto ya tienes:

1. **`design-system.md`** → reglas visuales globales  
2. **`components-guide.md`** → componentes reutilizables

El siguiente archivo ideal sería:

### **`app-flow.md`**
para documentar **todos los flujos funcionales y navegación**:

- Login → Home → Ruta → Parada → Espera → Abordaje
- Flujo conductor
- Estados de notificación
- Casos vacíos
- Errores
- Permisos
- Roles

Ese archivo le permite a Codex **entender cómo se conecta toda la app**, no solo cómo se ve.

Si quieres, te genero ahora ese **:contentReference[oaicite:0]{index=0}** para dejar listo el sistema de diseño + componentes + flujo de navegación para desarrollo.
```
