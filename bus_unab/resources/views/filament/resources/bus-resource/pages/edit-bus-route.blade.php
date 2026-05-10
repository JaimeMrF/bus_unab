@php
    // Todas las paradas activas
    $allStops = \App\Models\Stop::active()
        ->whereNotNull('latitude')
        ->whereNotNull('longitude')
        ->orderBy('name')
        ->get(['id','name','latitude','longitude'])
        ->map(fn ($s) => [
            'id'   => $s->id,
            'name' => $s->name,
            'lat'  => (float) $s->latitude,
            'lng'  => (float) $s->longitude,
        ])
        ->values();

    // Paradas ya asignadas a este bus, en orden (filtra stops eliminados)
    $assignedStops = $record->routeStops
        ->sortBy('order')
        ->filter(fn ($rs) => $rs->stop !== null && $rs->stop->latitude !== null && $rs->stop->longitude !== null)
        ->map(fn ($rs) => [
            'stop_id'           => $rs->stop_id,
            'name'              => $rs->stop->name,
            'lat'               => (float) $rs->stop->latitude,
            'lng'               => (float) $rs->stop->longitude,
            'order'             => (int) $rs->order,
            'estimated_minutes' => (int) $rs->estimated_minutes,
        ])
        ->values();

    // Waypoints existentes
    $existingWaypoints = $record->routeWaypoints->map(fn ($w) => [
        'lat'   => (float) $w->latitude,
        'lng'   => (float) $w->longitude,
        'order' => (int)   $w->order,
        'label' => $w->label ?? '',
    ])->values();
@endphp

<x-filament-panels::page>

<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />

<style>
    #route-map .leaflet-pane,
    #route-map .leaflet-tile         { z-index: 1 !important; }
    #route-map .leaflet-overlay-pane { z-index: 2 !important; }
    #route-map .leaflet-shadow-pane  { z-index: 3 !important; }
    #route-map .leaflet-marker-pane  { z-index: 4 !important; }
    #route-map .leaflet-tooltip-pane { z-index: 5 !important; }
    #route-map .leaflet-popup-pane   { z-index: 6 !important; }
    #route-map .leaflet-top,
    #route-map .leaflet-bottom       { z-index: 7 !important; }

    .stop-list-item { transition: background .15s; }
    .stop-list-item:hover { background: rgba(99,102,241,.07); }
    .stop-list-item.dragging { opacity: .5; }
</style>

<div
    wire:ignore
    x-data="busRouteEditor(@js($allStops), @js($assignedStops), @js($existingWaypoints))"
    class="space-y-3"
>

    {{-- ══ TOOLBAR ══════════════════════════════════════════════════════════ --}}
    <div class="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 px-4 py-2.5 shadow-sm">

        {{-- Modos --}}
        <div class="flex items-center gap-2">
            <span class="text-xs font-semibold text-gray-400 uppercase tracking-wide mr-1">Modo</span>

            <button @click="mode = 'stop'"
                :class="mode === 'stop' ? 'bg-indigo-600 text-white ring-indigo-600' : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 ring-gray-200 dark:ring-gray-600'"
                class="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium ring-1 transition-all">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/>
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z"/>
                </svg>
                Paradas
            </button>

            <button @click="mode = 'waypoint'"
                :class="mode === 'waypoint' ? 'bg-orange-500 text-white ring-orange-500' : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 ring-gray-200 dark:ring-gray-600'"
                class="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium ring-1 transition-all">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                </svg>
                Waypoints
            </button>

            <button @click="mode = 'delete'"
                :class="mode === 'delete' ? 'bg-red-600 text-white ring-red-600' : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 ring-gray-200 dark:ring-gray-600'"
                class="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium ring-1 transition-all">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/>
                </svg>
                Eliminar
            </button>
        </div>

        {{-- Hint según modo --}}
        <div class="hidden sm:flex items-center text-xs text-gray-400 dark:text-gray-500 italic">
            <span x-show="mode === 'stop'" style="display:none">Clic en parada gris → agregar a ruta &nbsp;·&nbsp; Clic en parada numerada → quitar</span>
            <span x-show="mode === 'waypoint'" style="display:none">Clic en el mapa → añadir punto intermedio entre paradas</span>
            <span x-show="mode === 'delete'" style="display:none">Clic en waypoint naranja → eliminarlo</span>
        </div>

        {{-- Acciones --}}
        <div class="flex items-center gap-2">
            <button @click="loadOsrmPreview()" :disabled="previewLoading || route.length < 2"
                class="inline-flex items-center gap-1.5 rounded-lg bg-gray-50 dark:bg-gray-700 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-200 ring-1 ring-gray-200 dark:ring-gray-600 transition hover:bg-gray-100 disabled:opacity-40">
                <svg x-show="!previewLoading" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 6.75V15m6-6v8.25m.503 3.498 4.875-2.437c.381-.19.622-.58.622-1.006V4.82c0-.836-.88-1.38-1.628-1.006l-3.869 1.934c-.317.159-.69.159-1.006 0L9.503 3.252a1.125 1.125 0 0 0-1.006 0L3.622 5.689C3.24 5.88 3 6.27 3 6.695V19.18c0 .836.88 1.38 1.628 1.006l3.869-1.934c.317-.159.69-.159 1.006 0l4.994 2.497c.317.158.69.158 1.006 0Z"/>
                </svg>
                <svg x-show="previewLoading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                Vista previa (OSRM)
            </button>

            <button @click="clearWaypoints()"
                class="inline-flex items-center gap-1.5 rounded-lg bg-gray-50 dark:bg-gray-700 px-3 py-1.5 text-sm font-medium text-red-500 ring-1 ring-gray-200 dark:ring-gray-600 hover:bg-red-50 dark:hover:bg-red-950 transition">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0"/>
                </svg>
                Limpiar waypoints
            </button>

            <button @click="saveAll()" :disabled="saving"
                class="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 hover:bg-primary-700 px-4 py-1.5 text-sm font-semibold text-white transition disabled:opacity-50">
                <svg x-show="!saving" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
                </svg>
                <svg x-show="saving" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                Guardar ruta
            </button>
        </div>
    </div>

    {{-- ══ CUERPO: lista de paradas + mapa ════════════════════════════════ --}}
    <div class="flex gap-3" style="height: 600px;">

        {{-- ── Panel lateral: paradas en orden ── --}}
        <div class="w-64 flex-shrink-0 flex flex-col rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden">
            <div class="px-3 py-2.5 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
                <span class="text-sm font-semibold text-gray-700 dark:text-gray-200">
                    Orden de paradas
                </span>
                <span class="text-xs text-gray-400 bg-gray-100 dark:bg-gray-700 rounded-full px-2 py-0.5"
                    x-text="route.length + ' / ' + allStops.length"></span>
            </div>

            {{-- Lista ordenada --}}
            <div class="flex-1 overflow-y-auto divide-y divide-gray-100 dark:divide-gray-700/50" id="stop-list">
                <template x-if="route.length === 0">
                    <div class="flex flex-col items-center justify-center h-full text-center px-4 py-8 text-gray-400">
                        <svg class="w-10 h-10 mb-2 opacity-40" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/>
                            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z"/>
                        </svg>
                        <p class="text-xs">Selecciona paradas en el mapa para armar la ruta</p>
                    </div>
                </template>

                <template x-for="(stop, idx) in route" :key="stop.stop_id">
                    <div class="stop-list-item flex items-center gap-2 px-3 py-2">
                        {{-- Número --}}
                        <span class="flex-shrink-0 w-6 h-6 rounded-full bg-indigo-600 text-white text-xs font-bold flex items-center justify-center"
                            x-text="idx + 1"></span>

                        {{-- Nombre --}}
                        <span class="flex-1 text-sm text-gray-700 dark:text-gray-200 truncate" x-text="stop.name"></span>

                        {{-- Subir / bajar --}}
                        <div class="flex flex-col gap-0.5">
                            <button @click="moveStop(idx, -1)" :disabled="idx === 0"
                                class="text-gray-300 hover:text-gray-600 disabled:opacity-20 transition">
                                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 15.75 7.5-7.5 7.5 7.5"/>
                                </svg>
                            </button>
                            <button @click="moveStop(idx, 1)" :disabled="idx === route.length - 1"
                                class="text-gray-300 hover:text-gray-600 disabled:opacity-20 transition">
                                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5"/>
                                </svg>
                            </button>
                        </div>

                        {{-- Quitar --}}
                        <button @click="removeStop(idx)"
                            class="flex-shrink-0 text-red-300 hover:text-red-500 transition">
                            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/>
                            </svg>
                        </button>
                    </div>
                </template>
            </div>

            {{-- Leyenda --}}
            <div class="px-3 py-2 border-t border-gray-100 dark:border-gray-700 space-y-1 text-xs text-gray-400">
                <div class="flex items-center gap-1.5">
                    <span class="w-3 h-3 rounded-full bg-indigo-600 flex-shrink-0"></span> Parada en ruta
                </div>
                <div class="flex items-center gap-1.5">
                    <span class="w-3 h-3 rounded-full bg-gray-400 flex-shrink-0"></span> Parada disponible
                </div>
                <div class="flex items-center gap-1.5">
                    <span class="w-3 h-3 rounded-full bg-orange-500 flex-shrink-0"></span> Waypoint intermedio
                </div>
            </div>
        </div>

        {{-- ── Mapa ── --}}
        <div class="flex-1 rounded-xl ring-1 ring-gray-200 dark:ring-gray-700 shadow-sm overflow-hidden">
            <div id="route-map" style="width:100%;height:100%;"></div>
        </div>
    </div>

    {{-- Stats rápidos --}}
    <div class="flex items-center gap-4 text-xs text-gray-400 pl-1">
        <span><span class="font-semibold text-indigo-500" x-text="route.length"></span> paradas</span>
        <span>·</span>
        <span><span class="font-semibold text-orange-500" x-text="waypoints.length"></span> waypoints</span>
        <span x-show="osrmLines.length > 0" style="display:none">
            · <span class="text-green-500 font-semibold">Ruta OSRM activa</span>
        </span>
    </div>

</div>

<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

<script>
function busRouteEditor(allStops, assignedStops, existingWaypoints) {
    return {
        allStops,
        route:           [],   // [{stop_id, name, lat, lng, estimated_minutes}]
        waypoints:       [],   // [{lat, lng, label, _seg, _t}] – siempre ordenado por _seg luego _t
        waypointMarkers: [],   // array paralelo a waypoints: instancias Leaflet marker
        stopMarkers:     {},   // stop_id → leaflet marker
        guideLine:       null,
        osrmLines:       [],
        map:             null,
        mode:            'stop',
        saving:          false,
        previewLoading:  false,

        init() {
            assignedStops.forEach(s => this.route.push({
                stop_id:           s.stop_id,
                name:              s.name,
                lat:               s.lat,
                lng:               s.lng,
                estimated_minutes: s.estimated_minutes ?? 0,
            }));

            this.waitForLeaflet(() => this.setupMap());

            this.$watch('route', () => {
                this.refreshStopMarkers();
                this.refreshWaypointSegments();
                this.drawGuideLine();
            });
        },

        waitForLeaflet(cb) {
            if (window.L) { cb(); return; }
            const t = setInterval(() => { if (window.L) { clearInterval(t); cb(); } }, 50);
        },

        setupMap() {
            const center = this.route.length
                ? [this.route[0].lat, this.route[0].lng]
                : (allStops.length ? [allStops[0].lat, allStops[0].lng] : [7.1193, -73.1227]);

            this.map = L.map('route-map', { center, zoom: 14 });

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
                maxZoom: 19,
            }).addTo(this.map);

            allStops.forEach(s => this.createStopMarker(s));
            this.refreshStopMarkers();

            existingWaypoints.forEach(wp => this.addWaypointAt(wp.lat, wp.lng, wp.label || ''));
            this.drawGuideLine();

            const pts = allStops.map(s => [s.lat, s.lng]);
            if (pts.length > 1) this.map.fitBounds(L.latLngBounds(pts), { padding: [40, 40] });

            this.map.on('click', e => {
                if (this.mode === 'waypoint') {
                    this.addWaypointAt(e.latlng.lat, e.latlng.lng, '');
                    this.drawGuideLine();
                }
            });
        },

        // ── Marcadores de paradas ────────────────────────────────────────────
        createStopMarker(stop) {
            const mkr = L.marker([stop.lat, stop.lng], {
                icon: this.grayIcon(),
                zIndexOffset: 100,
            }).addTo(this.map);
            mkr.bindTooltip(this.esc(stop.name), { direction: 'top', opacity: .9 });
            mkr.on('click', () => { if (this.mode === 'stop') this.toggleStop(stop); });
            this.stopMarkers[stop.id] = mkr;
        },

        refreshStopMarkers() {
            allStops.forEach(stop => {
                const mkr = this.stopMarkers[stop.id];
                if (!mkr) return;
                const idx = this.route.findIndex(r => r.stop_id === stop.id);
                if (idx >= 0) {
                    mkr.setIcon(this.numberedIcon(idx + 1));
                    mkr.setZIndexOffset(500 + idx);
                } else {
                    mkr.setIcon(this.grayIcon());
                    mkr.setZIndexOffset(100);
                }
            });
        },

        toggleStop(stop) {
            const idx = this.route.findIndex(r => r.stop_id === stop.id);
            if (idx >= 0) {
                this.route.splice(idx, 1);
            } else {
                this.route.push({
                    stop_id: stop.id, name: stop.name,
                    lat: stop.lat, lng: stop.lng, estimated_minutes: 0,
                });
            }
        },

        removeStop(idx) { this.route.splice(idx, 1); },

        moveStop(idx, dir) {
            const newIdx = idx + dir;
            if (newIdx < 0 || newIdx >= this.route.length) return;
            [this.route[idx], this.route[newIdx]] = [this.route[newIdx], this.route[idx]];
            this.route = [...this.route];
        },

        // ── Waypoints ─────────────────────────────────────────────────────────

        // Devuelve el segmento más cercano {seg, t} para un punto dado.
        findBestSegment(lat, lng) {
            const N = this.route.length;
            if (N < 2) return { seg: 0, t: 0 };
            let best = { seg: 0, dist: Infinity, t: 0 };
            for (let i = 0; i < N - 1; i++) {
                const { dist, t } = this.ptSegDist(
                    lat, lng,
                    this.route[i].lat,   this.route[i].lng,
                    this.route[i+1].lat, this.route[i+1].lng,
                );
                if (dist < best.dist) best = { seg: i, dist, t };
            }
            return best;
        },

        // Índice de inserción para mantener waypoints ordenados por _seg, _t.
        findInsertIndex(seg, t) {
            for (let i = 0; i < this.waypoints.length; i++) {
                const w = this.waypoints[i];
                if (w._seg > seg || (w._seg === seg && w._t > t)) return i;
            }
            return this.waypoints.length;
        },

        // Crea un waypoint y lo inserta en la posición correcta de la secuencia.
        addWaypointAt(lat, lng, label) {
            const { seg, t } = this.findBestSegment(lat, lng);
            const insertIdx  = this.findInsertIndex(seg, t);

            const mkr = L.marker([lat, lng], {
                draggable: true,
                icon: this.waypointIcon(),
                zIndexOffset: 200,
            }).addTo(this.map);

            this.waypoints.splice(insertIdx, 0, { lat, lng, label, _seg: seg, _t: t });
            this.waypointMarkers.splice(insertIdx, 0, mkr);

            mkr.on('dragend', e => {
                const i = this.waypointMarkers.indexOf(mkr);
                if (i === -1) return;
                const p = e.target.getLatLng();
                const { seg: newSeg, t: newT } = this.findBestSegment(p.lat, p.lng);
                const savedLabel = this.waypoints[i].label;
                // Quitar de posición actual
                this.waypoints.splice(i, 1);
                this.waypointMarkers.splice(i, 1);
                // Insertar en nueva posición ordenada
                const newIdx = this.findInsertIndex(newSeg, newT);
                this.waypoints.splice(newIdx, 0, { lat: p.lat, lng: p.lng, label: savedLabel, _seg: newSeg, _t: newT });
                this.waypointMarkers.splice(newIdx, 0, mkr);
                this.drawGuideLine();
            });

            mkr.on('click', () => {
                if (this.mode === 'delete') {
                    const i = this.waypointMarkers.indexOf(mkr);
                    if (i !== -1) {
                        mkr.remove();
                        this.waypoints.splice(i, 1);
                        this.waypointMarkers.splice(i, 1);
                        this.drawGuideLine();
                    }
                }
            });
        },

        // Recomputa _seg y _t de todos los waypoints cuando cambian las paradas.
        refreshWaypointSegments() {
            if (this.waypoints.length === 0) return;
            this.waypoints.forEach(wp => {
                const { seg, t } = this.findBestSegment(wp.lat, wp.lng);
                wp._seg = seg;
                wp._t   = t;
            });
            // Ordenar ambos arrays usando la misma permutación de índices.
            const indices = Array.from({ length: this.waypoints.length }, (_, i) => i);
            indices.sort((a, b) => {
                const wa = this.waypoints[a], wb = this.waypoints[b];
                return wa._seg !== wb._seg ? wa._seg - wb._seg : wa._t - wb._t;
            });
            this.waypoints       = indices.map(i => this.waypoints[i]);
            this.waypointMarkers = indices.map(i => this.waypointMarkers[i]);
        },

        clearWaypoints() {
            this.waypointMarkers.forEach(m => m.remove());
            this.waypoints       = [];
            this.waypointMarkers = [];
            this.osrmLines.forEach(l => l.remove());
            this.osrmLines = [];
            this.drawGuideLine();
        },

        // ── Línea guía ────────────────────────────────────────────────────────
        drawGuideLine() {
            if (this.guideLine) { this.guideLine.remove(); this.guideLine = null; }
            const path = this.buildOrderedPath();
            if (path.length < 2) return;
            this.guideLine = L.polyline(path.map(p => [p.lat, p.lng]), {
                color: '#6366f1', weight: 3, opacity: 0.5, dashArray: '7 6',
            }).addTo(this.map);
        },

        // Ruta completa en orden: stops intercalados con sus waypoints.
        buildOrderedPath() {
            const N = this.route.length;
            if (N === 0) return this.waypoints.map(w => ({ lat: w.lat, lng: w.lng }));
            if (N === 1) return [this.route[0], ...this.waypoints];

            // waypoints ya están ordenados por _seg luego _t
            const path = [];
            let wi = 0;
            for (let i = 0; i < N; i++) {
                path.push(this.route[i]);
                while (wi < this.waypoints.length && this.waypoints[wi]._seg === i) {
                    path.push(this.waypoints[wi]);
                    wi++;
                }
            }
            return path;
        },

        // ── OSRM ─────────────────────────────────────────────────────────────
        async loadOsrmPreview() {
            if (this.route.length < 2 || this.previewLoading) return;
            this.previewLoading = true;
            const path = this.buildOrderedPath();
            const coords = path.map(p => `${p.lng},${p.lat}`).join(';');
            try {
                const res  = await fetch(`https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=polyline`);
                const data = await res.json();
                if (data.code !== 'Ok' || !data.routes?.[0]?.geometry) throw new Error();
                const pts = this.decodePolyline(data.routes[0].geometry);
                this.osrmLines.forEach(l => l.remove()); this.osrmLines = [];
                this.osrmLines.push(
                    L.polyline(pts, { color: '#22c55e', weight: 10, opacity: 0.18 }).addTo(this.map),
                    L.polyline(pts, { color: '#22c55e', weight:  5, opacity: 0.9  }).addTo(this.map),
                );
            } catch {
                alert('No se pudo obtener la ruta de OSRM.');
            } finally {
                this.previewLoading = false;
            }
        },

        // ── Guardar ───────────────────────────────────────────────────────────
        async saveAll() {
            if (this.saving) return;
            this.saving = true;
            try {
                const routeStops = this.route.map((s, i) => ({
                    stop_id:           s.stop_id,
                    order:             (i + 1) * 10,
                    estimated_minutes: s.estimated_minutes ?? 0,
                }));
                const waypoints = this.prepareWaypoints(routeStops);
                await this.$wire.saveAll(routeStops, waypoints);
            } finally {
                this.saving = false;
            }
        },

        // Asigna órdenes numéricos a los waypoints respetando el orden de segmentos.
        prepareWaypoints(routeStops) {
            if (this.waypoints.length === 0 || routeStops.length < 2) return [];

            const result = [];
            let i = 0;
            while (i < this.waypoints.length) {
                const seg = this.waypoints[i]._seg;
                if (seg >= routeStops.length - 1) { i++; continue; }

                const group = [];
                while (i < this.waypoints.length && this.waypoints[i]._seg === seg) {
                    group.push(this.waypoints[i++]);
                }

                const orderA = routeStops[seg].order;
                const orderB = routeStops[seg + 1].order;
                const step   = (orderB - orderA) / (group.length + 1);

                group.forEach((wp, j) => result.push({
                    lat:   wp.lat,
                    lng:   wp.lng,
                    label: wp.label || '',
                    order: Math.round(orderA + step * (j + 1)),
                }));
            }
            return result;
        },

        // ── Geometría ─────────────────────────────────────────────────────────
        ptSegDist(px, py, ax, ay, bx, by) {
            const dx = bx - ax, dy = by - ay;
            const lenSq = dx*dx + dy*dy;
            const t = lenSq > 0 ? Math.max(0, Math.min(1, ((px-ax)*dx + (py-ay)*dy) / lenSq)) : 0;
            return { dist: Math.hypot(px - ax - t*dx, py - ay - t*dy), t };
        },

        decodePolyline(enc) {
            const pts = []; let idx = 0, lat = 0, lng = 0;
            while (idx < enc.length) {
                let b, s = 0, r = 0;
                do { b = enc.charCodeAt(idx++) - 63; r |= (b & 0x1f) << s; s += 5; } while (b >= 0x20);
                lat += (r & 1) ? ~(r >> 1) : r >> 1; s = r = 0;
                do { b = enc.charCodeAt(idx++) - 63; r |= (b & 0x1f) << s; s += 5; } while (b >= 0x20);
                lng += (r & 1) ? ~(r >> 1) : r >> 1;
                pts.push([lat / 1e5, lng / 1e5]);
            }
            return pts;
        },

        // ── Iconos ──────────────────────────────────────────────────────────────
        grayIcon() {
            return L.divIcon({
                className: '',
                html: `<div style="width:22px;height:22px;background:#9ca3af;border:2.5px solid white;border-radius:50%;box-shadow:0 2px 5px rgba(0,0,0,.3);cursor:pointer;"></div>`,
                iconSize: [22,22], iconAnchor: [11,11],
            });
        },

        numberedIcon(n) {
            return L.divIcon({
                className: '',
                html: `<div style="width:28px;height:28px;background:#4f46e5;border:2.5px solid white;border-radius:50%;box-shadow:0 2px 7px rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:white;font-family:system-ui,sans-serif;cursor:pointer;">${n}</div>`,
                iconSize: [28,28], iconAnchor: [14,14],
            });
        },

        waypointIcon() {
            return L.divIcon({
                className: '',
                html: `<div style="width:20px;height:20px;background:#f97316;border:2.5px solid white;border-radius:50%;box-shadow:0 2px 5px rgba(0,0,0,.3);cursor:grab;"></div>`,
                iconSize: [20,20], iconAnchor: [10,10],
            });
        },

        esc(s) {
            return s ? String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;') : '';
        },
    };
}
</script>

</x-filament-panels::page>
