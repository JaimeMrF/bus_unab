<div>
<x-filament-panels::page>
<div>
    {{-- ── Leaflet CSS — inline para garantizar que cargue en Filament/Livewire ── --}}
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />

    <style>
        /* Corregir z-index dentro del layout de Filament */
        .leaflet-pane         { z-index: 1 !important; }
        .leaflet-tile         { z-index: 1 !important; }
        .leaflet-overlay-pane { z-index: 2 !important; }
        .leaflet-shadow-pane  { z-index: 3 !important; }
        .leaflet-marker-pane  { z-index: 4 !important; }
        .leaflet-tooltip-pane { z-index: 5 !important; }
        .leaflet-popup-pane   { z-index: 6 !important; }
        .leaflet-top,
        .leaflet-bottom       { z-index: 7 !important; }

        .bus-marker {
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            border: 3px solid rgba(255,255,255,0.9);
            box-shadow: 0 2px 8px rgba(0,0,0,.35);
        }
        .bus-pulse::after {
            content: '';
            position: absolute;
            width: 100%;
            height: 100%;
            border-radius: 50%;
            border: 2px solid var(--pulse-color, #22c55e);
            animation: pulse-ring 2s ease-out infinite;
            opacity: 0;
        }
        @keyframes pulse-ring {
            0%   { transform: scale(1);   opacity: .6; }
            100% { transform: scale(1.8); opacity: 0;  }
        }
        .leaflet-popup-content-wrapper {
            border-radius: 12px !important;
            box-shadow: 0 4px 20px rgba(0,0,0,.15) !important;
            padding: 0 !important;
            overflow: hidden;
        }
        .leaflet-popup-content { margin: 0 !important; }
        .leaflet-popup-tip-container { display: none; }
    </style>

    {{-- ── Barra de control ─────────────────────────────────────────────────── --}}
    <div class="flex flex-wrap items-center justify-between gap-3 mb-4">

        <div class="flex gap-3 flex-wrap">
            <span class="inline-flex items-center gap-1.5 rounded-lg bg-green-50 dark:bg-green-950 px-3 py-1.5 text-sm font-medium text-green-700 dark:text-green-300 ring-1 ring-green-200 dark:ring-green-800">
                <span class="h-2 w-2 rounded-full bg-green-500 animate-pulse"></span>
                <span id="stat-buses">{{ $busCount }}</span> buses activos
            </span>
            <span class="inline-flex items-center gap-1.5 rounded-lg bg-blue-50 dark:bg-blue-950 px-3 py-1.5 text-sm font-medium text-blue-700 dark:text-blue-300 ring-1 ring-blue-200 dark:ring-blue-800">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z"/></svg>
                {{ $stopCount }} paradas
            </span>
        </div>

        <div class="flex items-center gap-3">
            <span class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 dark:bg-gray-800 px-3 py-1.5 text-sm text-gray-500 dark:text-gray-400">
                <span class="h-2 w-2 rounded-full bg-gray-400" id="gps-dot"></span>
                <span id="gps-label">Conectando...</span>
            </span>
            <span class="inline-flex items-center gap-1.5 rounded-lg bg-gray-100 dark:bg-gray-800 px-3 py-1.5 text-sm text-gray-600 dark:text-gray-400">
                🔄 <span id="countdown">30</span>s
            </span>
            <button
                onclick="window.busMap && window.busMap.fetchData()"
                class="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 hover:bg-primary-700 px-3 py-1.5 text-sm font-medium text-white transition-colors"
            >
                Actualizar ahora
            </button>
        </div>
    </div>

    {{-- ── Contenedor del mapa ──────────────────────────────────────────────── --}}
    <div class="rounded-xl overflow-hidden ring-1 ring-gray-200 dark:ring-gray-700 shadow-sm"
         style="height: 640px; position: relative;">

        <div id="bus-map" style="height: 100%; width: 100%; z-index: 1;"></div>

        {{-- Leyenda flotante --}}
        <div class="absolute bottom-5 left-4 z-10 rounded-xl bg-white dark:bg-gray-900 shadow-lg ring-1 ring-gray-200 dark:ring-gray-700 p-3 text-xs space-y-1.5 min-w-[140px]" style="z-index:800;">
            <p class="font-semibold text-gray-700 dark:text-gray-200 mb-2">Aforo</p>
            <div class="flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-green-500 flex-shrink-0"></span><span class="text-gray-600 dark:text-gray-300">Bajo (&lt;30%)</span></div>
            <div class="flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-yellow-400 flex-shrink-0"></span><span class="text-gray-600 dark:text-gray-300">Medio (30–60%)</span></div>
            <div class="flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-orange-500 flex-shrink-0"></span><span class="text-gray-600 dark:text-gray-300">Alto (60–90%)</span></div>
            <div class="flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-red-500 flex-shrink-0"></span><span class="text-gray-600 dark:text-gray-300">Lleno (&gt;90%)</span></div>
            <div class="flex items-center gap-2"><span class="h-3 w-3 rounded-full bg-gray-400 flex-shrink-0"></span><span class="text-gray-600 dark:text-gray-300">Sin GPS</span></div>
            <hr class="border-gray-200 dark:border-gray-700 my-1">
            <div class="flex items-center gap-2">
                <svg class="h-3 w-3 text-blue-500 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M9.69 18.933l.003.001C9.89 19.02 10 19 10 19s.11.02.308-.066l.002-.001.006-.003.018-.008a5.741 5.741 0 00.281-.14c.186-.096.446-.24.757-.433.62-.384 1.445-.966 2.274-1.765C15.302 14.988 17 12.493 17 9A7 7 0 103 9c0 3.492 1.698 5.988 3.355 7.584a13.731 13.731 0 002.273 1.765 11.842 11.842 0 00.976.544l.062.029.018.008.006.003zM10 11.25a2.25 2.25 0 100-4.5 2.25 2.25 0 000 4.5z" clip-rule="evenodd"/></svg>
                <span class="text-gray-600 dark:text-gray-300">Parada</span>
            </div>
        </div>

        {{-- Panel lateral de telemetría --}}
        <div id="telemetry-panel"
             class="absolute top-4 right-4 rounded-xl bg-white dark:bg-gray-900 shadow-xl ring-1 ring-gray-200 dark:ring-gray-700 p-4 w-64"
             style="z-index:800; display:none;">
            <div class="flex items-start justify-between mb-3">
                <div>
                    <h3 id="tel-name"  class="font-bold text-gray-900 dark:text-white text-sm">—</h3>
                    <p  id="tel-plate" class="text-xs text-gray-500 dark:text-gray-400">—</p>
                </div>
                <button onclick="window.busMap && window.busMap.closeTelemetry()"
                        class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 transition-colors">
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
                </button>
            </div>

            <div class="mb-3">
                <div class="flex justify-between text-xs text-gray-500 dark:text-gray-400 mb-1">
                    <span>Aforo</span>
                    <span id="tel-occ-text">—</span>
                </div>
                <div class="h-2 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                    <div id="tel-occ-bar" class="h-full rounded-full transition-all duration-500" style="width:0%; background:#22c55e;"></div>
                </div>
            </div>

            <div class="grid grid-cols-2 gap-2 text-xs">
                <div class="rounded-lg bg-gray-50 dark:bg-gray-800 p-2">
                    <p class="text-gray-400 dark:text-gray-500">Velocidad</p>
                    <p id="tel-speed" class="font-bold text-gray-900 dark:text-white mt-0.5">—</p>
                </div>
                <div class="rounded-lg bg-gray-50 dark:bg-gray-800 p-2">
                    <p class="text-gray-400 dark:text-gray-500">Rumbo</p>
                    <p id="tel-heading" class="font-bold text-gray-900 dark:text-white mt-0.5">—</p>
                </div>
                <div class="rounded-lg bg-gray-50 dark:bg-gray-800 p-2 col-span-2">
                    <p class="text-gray-400 dark:text-gray-500">Conductor</p>
                    <p id="tel-driver" class="font-bold text-gray-900 dark:text-white mt-0.5 truncate">—</p>
                </div>
            </div>

            <div class="mt-2 rounded-lg bg-gray-50 dark:bg-gray-800 p-2 text-xs">
                <p class="text-gray-400 dark:text-gray-500">Ubicación</p>
                <p id="tel-address" class="text-gray-700 dark:text-gray-300 mt-0.5 leading-snug">—</p>
            </div>

            <p id="tel-updated" class="text-center text-xs text-gray-400 dark:text-gray-600 mt-2">—</p>
        </div>
    </div>

    {{--
    ── Leaflet JS — SIN integrity ni defer para garantizar carga síncrona ────
    El script se coloca DESPUÉS del div#bus-map para que el elemento ya exista
    cuando se ejecute el código de inicialización.
    --}}
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

    <script>
    (function () {
        'use strict';

        // ── Config ────────────────────────────────────────────────────────────
        const MAP_DATA_URL = @json($mapDataUrl);
        const REFRESH_SECS = 30;
        const CENTER       = [7.1193, -73.1227]; // Bucaramanga

        const LEVEL_COLORS = {
            low:    '#22c55e',
            medium: '#eab308',
            high:   '#f97316',
            full:   '#ef4444',
            offline:'#9ca3af',
        };

        // ── Estado interno ────────────────────────────────────────────────────
        let leafletMap;
        let busMarkers    = {};
        let stopMarkers   = {};
        let selectedBusId = null;
        let countdown     = REFRESH_SECS;
        let countdownTimer;

        // ── Inicializar ───────────────────────────────────────────────────────
        function init() {
            leafletMap = L.map('bus-map', {
                center: CENTER,
                zoom: 13,
                zoomControl: true,
            });

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
                maxZoom: 19,
            }).addTo(leafletMap);

            fetchData();
            startCountdown();
        }

        // ── Fetch ─────────────────────────────────────────────────────────────
        async function fetchData() {
            setStatus('loading');
            try {
                const res = await fetch(MAP_DATA_URL, {
                    credentials: 'same-origin',
                    headers: { 'Accept': 'application/json' },
                });
                if (!res.ok) { setStatus('error'); return; }

                const data = await res.json();
                renderBuses(data.buses  ?? []);
                renderStops(data.stops  ?? []);
                setStatus('ok', data.fetched_at);
            } catch (err) {
                console.error('[BusMap] fetch error:', err);
                setStatus('error');
            }
        }

        // ── Buses ─────────────────────────────────────────────────────────────
        function renderBuses(buses) {
            const seen = new Set();

            buses.forEach(bus => {
                seen.add(bus.id);
                const gps    = bus.gps ?? {};
                const online = gps.online === true;

                if (!online || !gps.latitude || !gps.longitude) {
                    removeBusMarker(bus.id);
                    return;
                }

                const level  = bus.occupancy?.level ?? 'low';
                const color  = LEVEL_COLORS[level] ?? LEVEL_COLORS.low;
                const icon   = makeBusIcon(bus.name, color, gps.heading ?? 0);
                const latlng = [gps.latitude, gps.longitude];

                if (busMarkers[bus.id]) {
                    busMarkers[bus.id].setLatLng(latlng);
                    busMarkers[bus.id].setIcon(icon);
                    busMarkers[bus.id]._busData = bus;
                } else {
                    const marker = L.marker(latlng, { icon }).addTo(leafletMap);
                    marker._busData = bus;
                    marker.on('click', () => openTelemetry(marker._busData));
                    busMarkers[bus.id] = marker;
                }

                if (selectedBusId === bus.id) fillTelemetry(bus);
            });

            // Limpiar buses que desaparecieron
            Object.keys(busMarkers).forEach(id => {
                if (!seen.has(Number(id))) removeBusMarker(Number(id));
            });
        }

        function removeBusMarker(id) {
            if (busMarkers[id]) { busMarkers[id].remove(); delete busMarkers[id]; }
        }

        // ── Paradas ───────────────────────────────────────────────────────────
        function renderStops(stops) {
            stops.forEach(stop => {
                if (stopMarkers[stop.id]) return;

                const icon = L.divIcon({
                    className: '',
                    html: `<div title="${esc(stop.name)}" style="
                        width:26px; height:26px;
                        background:#3b82f6;
                        border:3px solid white;
                        border-radius:50% 50% 50% 0;
                        transform:rotate(-45deg);
                        box-shadow:0 2px 6px rgba(0,0,0,.3);
                    "></div>`,
                    iconSize: [26, 26],
                    iconAnchor: [13, 26],
                    popupAnchor: [0, -30],
                });

                stopMarkers[stop.id] = L.marker([stop.latitude, stop.longitude], { icon })
                    .bindPopup(`
                        <div style="padding:12px; min-width:170px; font-family:system-ui,sans-serif;">
                            <div style="font-weight:700; font-size:13px; color:#1e40af; margin-bottom:4px;">
                                📍 ${esc(stop.name)}
                            </div>
                            <div style="font-size:12px; color:#64748b; line-height:1.4;">${esc(stop.address ?? '')}</div>
                            <div style="font-size:11px; color:#94a3b8; margin-top:6px;">Radio: ${stop.radius_meters} m</div>
                        </div>`)
                    .addTo(leafletMap);
            });
        }

        // ── Panel telemetría ──────────────────────────────────────────────────
        function openTelemetry(bus) {
            selectedBusId = bus.id;
            fillTelemetry(bus);
            document.getElementById('telemetry-panel').style.display = 'block';
            if (bus.gps?.online) leafletMap.panTo([bus.gps.latitude, bus.gps.longitude], { animate: true });
        }

        function closeTelemetry() {
            selectedBusId = null;
            document.getElementById('telemetry-panel').style.display = 'none';
        }

        function fillTelemetry(bus) {
            const gps  = bus.gps  ?? {};
            const occ  = bus.occupancy ?? {};
            const pct  = occ.percentage ?? 0;
            const color = LEVEL_COLORS[occ.level ?? 'low'];

            setText('tel-name',     bus.name);
            setText('tel-plate',    bus.plate);
            setText('tel-occ-text', `${occ.count ?? 0} / ${bus.capacity ?? '?'} (${pct}%)`);
            setText('tel-speed',    gps.online ? `${gps.speed_kmh ?? 0} km/h` : 'Sin GPS');
            setText('tel-heading',  gps.online ? headingLabel(gps.heading ?? 0) : '—');
            setText('tel-driver',   gps.driver   || 'No disponible');
            setText('tel-address',  gps.address  || 'Sin dirección');
            setText('tel-updated',  gps.last_updated_at ? `GPS: ${gps.last_updated_at}` : 'Sin actualización');

            const bar = document.getElementById('tel-occ-bar');
            bar.style.width           = Math.min(pct, 100) + '%';
            bar.style.backgroundColor = color;
        }

        // ── Ícono bus ─────────────────────────────────────────────────────────
        function makeBusIcon(name, color, heading) {
            const size = 42;
            return L.divIcon({
                className: '',
                html: `
                <div class="bus-marker bus-pulse"
                     style="width:${size}px; height:${size}px; background:${color}; position:relative; --pulse-color:${color};">
                    <!-- Flecha de dirección -->
                    <div style="
                        position:absolute; top:-12px; left:50%;
                        transform: translateX(-50%) rotate(${heading}deg);
                        transform-origin: 50% 100%;
                        pointer-events:none;">
                        <svg width="10" height="12" viewBox="0 0 10 12" fill="${color}" xmlns="http://www.w3.org/2000/svg">
                            <path d="M5 0L10 12H0Z"/>
                        </svg>
                    </div>
                    <!-- Ícono de bus -->
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="white"
                         style="position:absolute; top:50%; left:50%; transform:translate(-50%,-50%);">
                        <path d="M4 16c0 .88.39 1.67 1 2.22V20a1 1 0 001 1h1a1 1 0 001-1v-1h8v1a1 1 0 001 1h1a1 1 0 001-1v-1.78c.61-.55 1-1.34 1-2.22V6c0-3.5-3.58-4-8-4s-8 .5-8 4v10zm3.5 1c-.83 0-1.5-.67-1.5-1.5S6.67 14 7.5 14s1.5.67 1.5 1.5S8.33 17 7.5 17zm9 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm1.5-6H6V6h12v5z"/>
                    </svg>
                    <!-- Etiqueta -->
                    <div style="
                        position:absolute; bottom:-18px; left:50%;
                        transform:translateX(-50%);
                        background:${color}; color:white;
                        font-size:9px; font-weight:700; font-family:system-ui,sans-serif;
                        padding:1px 5px; border-radius:4px;
                        white-space:nowrap; box-shadow:0 1px 3px rgba(0,0,0,.3);
                        pointer-events:none;">
                        ${esc(name)}
                    </div>
                </div>`,
                iconSize:    [size, size],
                iconAnchor:  [size / 2, size / 2],
                popupAnchor: [0, -size / 2 - 10],
            });
        }

        // ── Countdown ─────────────────────────────────────────────────────────
        function startCountdown() {
            clearInterval(countdownTimer);
            countdownTimer = setInterval(() => {
                countdown--;
                const el = document.getElementById('countdown');
                if (el) el.textContent = countdown;
                if (countdown <= 0) { countdown = REFRESH_SECS; fetchData(); }
            }, 1000);
        }

        // ── Estado GPS ────────────────────────────────────────────────────────
        function setStatus(state, fetchedAt) {
            const dot   = document.getElementById('gps-dot');
            const label = document.getElementById('gps-label');
            if (!dot || !label) return;

            if (state === 'ok') {
                dot.className     = 'h-2 w-2 rounded-full bg-green-500';
                const t           = fetchedAt ? new Date(fetchedAt).toLocaleTimeString('es-CO') : '';
                label.textContent = `GPS activo${t ? ' · ' + t : ''}`;
                countdown         = REFRESH_SECS;
            } else if (state === 'error') {
                dot.className     = 'h-2 w-2 rounded-full bg-red-500';
                label.textContent = 'GPS no disponible';
            } else {
                dot.className     = 'h-2 w-2 rounded-full bg-yellow-400 animate-pulse';
                label.textContent = 'Actualizando...';
            }
        }

        // ── Helpers ───────────────────────────────────────────────────────────
        function setText(id, text) {
            const el = document.getElementById(id);
            if (el) el.textContent = text ?? '—';
        }

        function headingLabel(deg) {
            const dirs = ['N','NE','E','SE','S','SO','O','NO'];
            return dirs[Math.round(deg / 45) % 8] + ` (${deg}°)`;
        }

        function esc(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g,'&amp;').replace(/</g,'&lt;')
                .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
        }

        // ── Exponer API pública (para botones en la plantilla Blade) ──────────
        window.busMap = { fetchData, closeTelemetry };

        // ── Arrancar cuando el DOM esté listo ─────────────────────────────────
        // Leaflet ya está cargado porque su <script> viene antes de éste.
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', init);
        } else {
            init(); // El DOM ya está listo (Livewire lo puede haber parseado ya)
        }

    })();
    </script>
</div>
</x-filament-panels::page>
</div>
