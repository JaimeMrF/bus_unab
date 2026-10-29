<!DOCTYPE html>
<html lang="es-CO">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>BUCARATRANSIT · Muévete por Bucaramanga</title>
    <meta name="description" content="Plataforma de movilidad urbana para Bucaramanga: buses en vivo, wallet prepago y pago con QR dinámico firmado. Una sola app para todas las transportadoras.">
    <meta name="theme-color" content="#02132f">
    <meta property="og:title" content="BUCARATRANSIT · Muévete por Bucaramanga">
    <meta property="og:description" content="Buses en vivo, wallet prepago y QR de pago seguro. SaaS multi-tenant para transportadoras.">
    <meta property="og:image" content="{{ asset('landing/img/leo-saludo.webp') }}">
    <meta property="og:type" content="website">
    <link rel="icon" type="image/png" href="{{ asset('landing/img/icon.png') }}">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:ital,wght@0,400;0,500;0,600;0,700;0,800;1,700;1,800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="{{ asset('landing/landing.css') }}">
</head>
<body>
<a class="skip" href="#contenido">Saltar al contenido</a>

<header class="nav" id="nav">
    <a class="nav__brand" href="#top" aria-label="BUCARATRANSIT, inicio">
        <img src="{{ asset('landing/img/logo.webp') }}" alt="BucaraTransit" width="120" height="80">
    </a>
    <nav class="nav__links" aria-label="Principal">
        <a href="#como">Cómo funciona</a>
        <a href="#app">La app</a>
        <a href="#empresas">Empresas</a>
        <a href="#seguridad">Seguridad</a>
        <a href="#equipo">Equipo</a>
        <a href="#descargar">Descargar</a>
    </nav>
    <a class="btn btn--sm" href="#descargar">Descargar app</a>
</header>

<main id="contenido">

    {{-- ───────────────────────── HERO ───────────────────────── --}}
    <section class="hero" id="top">
        <svg class="hero__route" viewBox="0 0 1200 700" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
            <defs>
                <linearGradient id="rg" x1="0" x2="1">
                    <stop offset="0" stop-color="#FCBB02" stop-opacity="0"/>
                    <stop offset=".5" stop-color="#FCBB02" stop-opacity=".9"/>
                    <stop offset="1" stop-color="#FCBB02" stop-opacity="0"/>
                </linearGradient>
            </defs>
            <path id="ruta" d="M-40 560 C 180 520, 260 300, 470 330 S 760 560, 900 380 S 1120 120, 1260 170" fill="none" stroke="url(#rg)" stroke-width="3" stroke-dasharray="2 12" stroke-linecap="round"/>
            <g class="hero__stops" fill="#02132f" stroke="#FCBB02" stroke-width="3">
                <circle cx="470" cy="330" r="9"/><circle cx="900" cy="380" r="9"/><circle cx="190" cy="500" r="9"/><circle cx="1090" cy="190" r="9"/>
            </g>
            <circle r="9" fill="#FCBB02" class="hero__dot"><animateMotion dur="14s" repeatCount="indefinite" rotate="auto"><mpath href="#ruta"/></animateMotion></circle>
        </svg>

        <div class="wrap hero__grid">
            <div class="hero__copy">
                <p class="eyebrow reveal"><span class="pulse"></span> En vivo · Bucaramanga, Santander</p>
                <h1 class="reveal">Muévete por <em>Bucaramanga</em> sin efectivo ni esperas.</h1>
                <p class="lead reveal">Todas las transportadoras en una sola app: mira dónde va tu bus, recarga tu wallet y paga con un QR que solo funciona una vez, durante 60 segundos.</p>
                <div class="hero__cta reveal">
                    <a class="btn" href="/descargas/BucaraTransit.apk" download>
                        <svg class="ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12m0 0l-4-4m4 4l4-4M5 20h14"/></svg> Descargar APK
                    </a>
                    <a class="btn btn--ghost" href="#como">Ver cómo funciona</a>
                </div>
                <ul class="hero__facts reveal" aria-label="Cifras clave">
                    <li><b>60 s</b><span>vida del QR</span></li>
                    <li><b>1 uso</b><span>por código</span></li>
                    <li><b>304</b><span>pruebas verdes</span></li>
                </ul>
            </div>

            <div class="hero__stage reveal" aria-hidden="false">
                <div class="glow"></div>
                <div class="phone phone--main">
                    <img src="{{ asset('landing/img/app-inicio.webp') }}" alt="App BucaraTransit: mapa con buses en vivo" width="420" height="933">
                </div>
                <div class="phone phone--back">
                    <img src="{{ asset('landing/img/app-wallet.webp') }}" alt="Wallet con saldo y botón de pagar a bordo" width="420" height="933" loading="lazy">
                </div>
                <img class="hero__leo" src="{{ asset('landing/img/leo-saludo.webp') }}" alt="Leo, el leopardo mascota de BucaraTransit" width="320" height="320">
                <div class="chip chip--a"><i class="dot dot--green"></i> BT401 · llega en 6 min</div>
                <div class="chip chip--b"><i class="dot dot--gold"></i> QR firmado · 60 s</div>
            </div>
        </div>
    </section>

    {{-- ───────────────────────── TIRA ───────────────────────── --}}
    <section class="strip" aria-label="Qué resuelve">
        <div class="wrap strip__row">
            <p><b>Sin efectivo.</b> El conductor cobra con el celular.</p>
            <p><b>Sin fugas.</b> Cada peso queda en un ledger auditable.</p>
            <p><b>Sin silos.</b> Cada transportadora ve solo lo suyo.</p>
        </div>
    </section>

    {{-- ───────────────────────── CÓMO ───────────────────────── --}}
    <section class="sec" id="como">
        <div class="wrap">
            <header class="sec__head reveal">
                <p class="kicker">Para pasajeros</p>
                <h2>Del paradero al pasaje, en tres pasos.</h2>
            </header>
            <ol class="steps">
                <li class="step reveal">
                    <img src="{{ asset('landing/img/leo-mapa.webp') }}" alt="" width="200" height="200" loading="lazy">
                    <span class="step__n">01</span>
                    <h3>Encuentra tu bus</h3>
                    <p>Mapa con buses en movimiento y color por ocupación: verde disponible, ámbar llegando, rojo lleno. Elige tu parada y recibe el tiempo estimado de llegada.</p>
                </li>
                <li class="step reveal">
                    <img src="{{ asset('landing/img/leo-celular.webp') }}" alt="" width="200" height="200" loading="lazy">
                    <span class="step__n">02</span>
                    <h3>Recarga tu wallet</h3>
                    <p>Un solo saldo prepago sirve para todas las transportadoras. Cada movimiento queda registrado en un libro contable de doble entrada.</p>
                </li>
                <li class="step reveal">
                    <img src="{{ asset('landing/img/leo-ok.webp') }}" alt="" width="200" height="200" loading="lazy">
                    <span class="step__n">03</span>
                    <h3>Muestra tu QR</h3>
                    <p>El conductor lo escanea y el pasaje se descuenta al instante. El código rota, vence en 60 segundos y no se puede reutilizar.</p>
                </li>
            </ol>
        </div>
    </section>

    {{-- ───────────────────────── FEATURES ───────────────────────── --}}
    <section class="sec sec--tint" id="plataforma">
        <div class="wrap">
            <header class="sec__head reveal">
                <p class="kicker">La plataforma</p>
                <h2>Pensada como infraestructura, no como un experimento.</h2>
                <p class="sub">Movilidad con dinero de por medio exige rigor. Por eso el sistema trata el saldo, la identidad y la disponibilidad como piezas críticas desde el diseño.</p>
            </header>
            <div class="grid">
                <article class="card reveal">
                    <div class="card__ic" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 21s-7-6.2-7-11a7 7 0 0114 0c0 4.8-7 11-7 11z"/><circle cx="12" cy="10" r="2.6"/></svg></div>
                    <h3>Rastreo y ETA en vivo</h3>
                    <p>GPS de la flota, llegada estimada por parada, seguimiento del bus y avisos push cuando está cerca.</p>
                </article>
                <article class="card reveal">
                    <div class="card__ic" aria-hidden="true"><svg viewBox="0 0 24 24"><rect x="3" y="6" width="18" height="13" rx="3"/><path d="M3 10h18M16 15h2"/></svg></div>
                    <h3>Wallet con ledger</h3>
                    <p>Saldo prepago con contabilidad de doble entrada, bloqueos e idempotencia: un cobro repetido jamás duplica el débito.</p>
                </article>
                <article class="card reveal">
                    <div class="card__ic" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 14h2v6h-4M14 18h2"/></svg></div>
                    <h3>QR dinámico firmado</h3>
                    <p>Firma HMAC-SHA256, expiración de 60 s y consumo atómico de un solo uso. Capturarlo no sirve de nada.</p>
                </article>
                <article class="card reveal">
                    <div class="card__ic" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 3l8 4v5c0 5-3.5 8-8 9-4.5-1-8-4-8-9V7z"/><path d="M9 12l2 2 4-4"/></svg></div>
                    <h3>Multi-tenant real</h3>
                    <p>Tres capas de aislamiento. Un recurso de otra empresa responde 404 silencioso: ni siquiera revela que existe.</p>
                </article>
                <article class="card reveal">
                    <div class="card__ic" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 3v18M3 12h18"/><circle cx="12" cy="12" r="8"/></svg></div>
                    <h3>Conductor = mPOS</h3>
                    <p>El teléfono del conductor es la terminal de cobro: escanea, cobra en milisegundos y reporta ocupación del bus.</p>
                </article>
                <article class="card reveal">
                    <div class="card__ic" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 100 18 4 4 0 004-4c0-2-2-2-2-4s2-2 4-2a9 9 0 00-6-8z"/><circle cx="8" cy="10" r="1"/><circle cx="12" cy="7" r="1"/><circle cx="16" cy="10" r="1"/></svg></div>
                    <h3>Marca blanca</h3>
                    <p>Cada organización descarga su marca (logo, colores, mascota) en tiempo real. Cero marca fija en el código de la app.</p>
                </article>
            </div>
        </div>
    </section>

    {{-- ───────────────────────── APP ───────────────────────── --}}
    <section class="sec" id="app">
        <div class="wrap">
            <header class="sec__head reveal">
                <p class="kicker">La app</p>
                <h2>Una experiencia que se entiende en la calle.</h2>
                <p class="sub">Kotlin Multiplatform y Compose: una base de código para Android e iOS, con tema oscuro, mapa minimalista y animaciones que respetan «reducir movimiento».</p>
            </header>
        </div>
        <div class="rail" tabindex="0" aria-label="Pantallas de la app (desliza)">
            <figure class="shot"><div class="phone"><img src="{{ asset('landing/img/app-login.webp') }}" alt="Pantalla de bienvenida" width="420" height="933" loading="lazy"></div><figcaption>Bienvenida y marca</figcaption></figure>
            <figure class="shot"><div class="phone"><img src="{{ asset('landing/img/app-parada.webp') }}" alt="Selección de parada" width="420" height="933" loading="lazy"></div><figcaption>Elige tu parada</figcaption></figure>
            <figure class="shot"><div class="phone"><img src="{{ asset('landing/img/app-rutas.webp') }}" alt="Rutas disponibles" width="420" height="933" loading="lazy"></div><figcaption>Rutas disponibles</figcaption></figure>
            <figure class="shot"><div class="phone"><img src="{{ asset('landing/img/app-siguiendo-bus.webp') }}" alt="Siguiendo el bus en el mapa" width="420" height="933" loading="lazy"></div><figcaption>Siguiendo tu bus</figcaption></figure>
            <figure class="shot"><div class="phone"><img src="{{ asset('landing/img/app-wallet.webp') }}" alt="Wallet y QR de pago" width="420" height="933" loading="lazy"></div><figcaption>Wallet y QR</figcaption></figure>
            <figure class="shot"><div class="phone"><img src="{{ asset('landing/img/app-conductor.webp') }}" alt="Modo conductor" width="420" height="933" loading="lazy"></div><figcaption>Modo conductor</figcaption></figure>
        </div>
    </section>

    {{-- ───────────────────────── EMPRESAS ───────────────────────── --}}
    <section class="sec sec--light" id="empresas">
        <div class="wrap">
            <header class="sec__head reveal">
                <p class="kicker kicker--dark">Para transportadoras</p>
                <h2>Cada empresa, su propio panel. Toda la ciudad, una sola plataforma.</h2>
                <p class="sub">El panel <b>/empresa</b> gestiona flota, rutas, tarifas y conductores de un solo tenant. El panel <b>/admin</b> da al equipo de plataforma una vista global de transportadoras y wallets.</p>
            </header>

            <div class="tabs reveal" role="tablist" aria-label="Vistas de los paneles">
                <button role="tab" aria-selected="true"  data-img="panel-empresa-escritorio" data-cap="Escritorio de la transportadora: solo su flota, sus pasajeros y sus paradas.">Escritorio</button>
                <button role="tab" aria-selected="false" data-img="panel-empresa-mapa"       data-cap="Mapa en vivo filtrado: la empresa solo ve sus propios buses.">Mapa en vivo</button>
                <button role="tab" aria-selected="false" data-img="panel-admin-editor-ruta"  data-cap="Editor de rutas: orden de paradas y vista previa por calles.">Editor de rutas</button>
                <button role="tab" aria-selected="false" data-img="panel-admin-transportadoras" data-cap="Super admin: varias transportadoras, planes y kill-switch por tenant.">Transportadoras</button>
                <button role="tab" aria-selected="false" data-img="panel-admin-wallets"      data-cap="Wallets de pasajeros con ajustes de saldo auditados.">Wallets</button>
            </div>
            <div class="browser reveal">
                <div class="browser__bar"><i></i><i></i><i></i><span>bucaratransit.lat/empresa</span></div>
                <img id="panelImg" src="{{ asset('landing/img/panel-empresa-escritorio.webp') }}" alt="Panel de empresa" width="1280" height="800" loading="lazy">
            </div>
            <p class="tabs__cap" id="panelCap">Escritorio de la transportadora: solo su flota, sus pasajeros y sus paradas.</p>
        </div>
    </section>

    {{-- ───────────────────────── SEGURIDAD ───────────────────────── --}}
    <section class="sec" id="seguridad">
        <div class="wrap">
            <header class="sec__head reveal">
                <p class="kicker">Seguridad y resiliencia</p>
                <h2>Un pago que no se puede copiar, un sistema que se cae bien.</h2>
            </header>

            <div class="sec2">
                <div class="seq reveal">
                    <h3>El ciclo de vida de un QR</h3>
                    <ol>
                        <li><b>Emisión.</b> El servidor genera un selector aleatorio de 16 bytes, guarda solo su hash SHA-256 y congela el monto.</li>
                        <li><b>Firma.</b> El QR visible es <code>selector.HMAC</code>: HMAC-SHA256 con clave del servidor.</li>
                        <li><b>Escaneo.</b> El conductor lo envía; el servidor recalcula la firma y compara con <code>hash_equals</code>.</li>
                        <li><b>Cobro atómico.</b> Vigencia ≤ 60 s y primera marca gana: débito al pasajero, crédito a la transportadora.</li>
                    </ol>
                </div>
                <div class="chaos reveal">
                    <h3>Chaos Engineering</h3>
                    <p class="muted">Apagamos servicios a propósito para comprobar que el sistema degrada, no explota.</p>
                    <table>
                        <thead><tr><th>Falla inyectada</th><th>Patrón</th><th>Resultado</th></tr></thead>
                        <tbody>
                            <tr><td>GPS externo caído</td><td>Degradación elegante</td><td><span class="ok">503 amigable</span></td></tr>
                            <tr><td>Google OAuth caído</td><td>Fallback</td><td><span class="ok">Login por correo</span></td></tr>
                            <tr><td>MySQL / Redis caídos</td><td>Bulkhead · cache-aside</td><td><span class="ok">Contenedores aislados</span></td></tr>
                            <tr><td>Fuerza bruta en login</td><td>Rate limiting</td><td><span class="ok">429 desde la 11.ª</span></td></tr>
                            <tr><td>Worker de colas caído</td><td>Proceso asíncrono</td><td><span class="ok">La API sigue</span></td></tr>
                            <tr><td>Nginx caído</td><td>—</td><td><span class="warn">Punto único: pide balanceador</span></td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <ul class="stack reveal" aria-label="Tecnologías">
                <li>Laravel 12</li><li>Filament</li><li>Sanctum</li><li>MySQL 8</li><li>Redis</li><li>Docker</li><li>Nginx + TLS</li><li>Kotlin Multiplatform</li><li>Compose</li><li>MapLibre</li><li>Firebase FCM</li>
            </ul>
        </div>
    </section>

    {{-- ───────────────────────── EQUIPO ───────────────────────── --}}
    <section class="sec sec--tint" id="equipo">
        <div class="wrap">
            <header class="sec__head reveal">
                <p class="kicker">Equipo</p>
                <h2>Tres ingenieros, un mismo repositorio.</h2>
            </header>
            <div class="team">
                <article class="person reveal"><span class="av">JT</span><h3>José Alejandro Téllez Prada</h3><p>Diseño del pivote, arquitectura multi-tenant y documentación técnica.</p></article>
                <article class="person reveal"><span class="av">MV</span><h3>Marcos Valera Daza</h3><p>Backend Laravel: wallet y ledger, tokens QR, paneles y pruebas de aislamiento.</p></article>
                <article class="person reveal"><span class="av">JV</span><h3>Jaime Alejandro Vega Barbosa</h3><p>App móvil Kotlin Multiplatform: pantallas, escáner QR y modo conductor.</p></article>
            </div>
        </div>
    </section>

    {{-- ───────────────────────── DESCARGA ───────────────────────── --}}
    <section class="sec dl" id="descargar">
        <div class="wrap dl__in reveal">
            <div class="dl__copy">
                <p class="kicker">Descarga</p>
                <h2>Llévate BucaraTransit en el bolsillo.</h2>
                <p class="sub">App para Android (versión 1.1.0). Instálala, entra con Google o con tu correo y empieza a seguir los buses de Bucaramanga.</p>
                <div class="hero__cta">
                    <a class="btn" href="/descargas/BucaraTransit.apk" download>
                        <svg class="ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12m0 0l-4-4m4 4l4-4M5 20h14"/></svg> Descargar APK
                    </a>
                </div>
                <ol class="dl__steps">
                    <li>Descarga el archivo <code>BucaraTransit.apk</code>.</li>
                    <li>Ábrelo y, si Android lo pide, permite <b>instalar apps de esta fuente</b>.</li>
                    <li>Abre la app y entra. Si ya tenías la versión anterior, se actualiza sola.</li>
                </ol>
                <p class="dl__note">Es un APK de pruebas fuera de Play Store: Android puede mostrar un aviso de seguridad. Solo para Android 8 o superior.</p>
            </div>
            <div class="dl__art">
                <div class="phone"><img src="{{ asset('landing/img/app-login.webp') }}" alt="Pantalla de bienvenida de la app" width="420" height="933" loading="lazy"></div>
                <img class="dl__leo" src="{{ asset('landing/img/leo-celular.webp') }}" alt="" width="220" height="220" loading="lazy">
            </div>
        </div>
    </section>

    {{-- ───────────────────────── CTA ───────────────────────── --}}
    <section class="cta">
        <div class="wrap cta__in reveal">
            <img src="{{ asset('landing/img/leo-celebrando.webp') }}" alt="" width="260" height="260" loading="lazy">
            <div>
                <h2>Bucaramanga se mueve mejor cuando todos se mueven juntos.</h2>
                <p>Conoce el sistema completo en la presentación o entra al panel de tu empresa.</p>
                <div class="hero__cta">
                    <a class="btn" href="/presentacion/">Ver la presentación</a>
                    <a class="btn btn--ghost" href="/empresa">Panel de empresa</a>
                </div>
            </div>
        </div>
    </section>
</main>

<footer class="foot">
    <div class="wrap foot__row">
        <img src="{{ asset('landing/img/logo.webp') }}" alt="BucaraTransit" width="96" height="64">
        <p>© 2026 BUCARATRANSIT · Proyecto académico de Backend, UNAB · Bucaramanga, Colombia</p>
        <nav aria-label="Enlaces">
            <a href="/admin">Super admin</a>
            <a href="/empresa">Empresa</a>
            <a href="/presentacion/">Presentación</a>
            <a href="https://github.com/JaimeMrF/bus_unab" rel="noopener">GitHub</a>
        </nav>
    </div>
</footer>

<script src="{{ asset('landing/landing.js') }}" defer></script>
</body>
</html>
