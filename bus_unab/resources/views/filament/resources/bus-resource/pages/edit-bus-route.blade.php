@php
    $stops = $record->stops->map(fn ($s) => [
        'id'         => $s->id,
        'name'       => $s->name,
        'lat'        => (float) $s->latitude,
        'lng'        => (float) $s->longitude,
        'pivotOrder' => (int)   $s->pivot->order,
    ])->values();

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
</style>

<div
    wire:ignore
    x-data="busRouteEditor(@js($stops), @js($existingWaypoints))"
    class="space-y-4"
>
    {{-- ── Toolbar ── --}}
    <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-2">
            <button @click="deleteMode = false"
                :class="!deleteMode ? 'bg-primary-600 text-white ring-primary-600' : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 ring-gray-200 dark:ring-gray-700'"
                class="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium ring-1 transition-colors">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                </svg>
                Añadir punto
            </button>

            <button @click="deleteMode = true"
                :class="deleteMode ? 'bg-red-600 text-white ring-red-600' : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 ring-gray-200 dark:ring-gray-700'"
                class="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium ring-1 transition-colors">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12"/>
                </svg>
                Eliminar punto
            </button>

            <span class="text-sm text-gray-500 dark:text-gray-400 pl-2">
                <span x-text="waypoints.length"></span> waypoints
            </span>
        </div>

        <div class="flex items-center gap-2">
            <button @click="loadOsrmPreview()" :disabled="previewLoading || stops.length < 2"
                class="inline-flex items-center gap-1.5 rounded-lg bg-white dark:bg-gray-800 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-200 ring-1 ring-gray-200 dark:ring-gray-700 transition-colors hover:bg-gray-50 disabled:opacity-40">
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
                class="inline-flex items-center gap-1.5 rounded-lg bg-white dark:bg-gray-800 px-3 py-1.5 text-sm font-medium text-red-600 ring-1 ring-red-200 dark:ring-red-800 hover:bg-red-50 dark:hover:bg-red-950 transition-colors">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0"/>
                </svg>
                Limpiar todo
            </button>

            <button @click="saveRouteAction()" :disabled="saving"
                class="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 hover:bg-primary-700 px-4 py-1.5 text-sm font-semibold text-white transition-colors disabled:opacity-50">
                <svg x-show="!saving" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
                </svg>
                <svg x-show="saving" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                Guardar Ruta
            </button>
        </div>
    </div>

    {{-- ── Leyenda ── --}}
    <div class="flex flex-wrap items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
        <div class="flex items-center gap-1.5">
            <span class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-blue-600 text-white text-[10px] font-bold leading-none">1</span>
            Parada (fija)
        </div>
        <div class="flex items-center gap-1.5">
            <span class="w-3.5 h-3.5 rounded-full bg-orange-500 border-2 border-white shadow"></span>
            Waypoint (arrastrable)
        </div>
        <div class="flex items-center gap-1.5">
            <span class="inline-block w-8 border-t-2 border-indigo-400 border-dashed" style="vertical-align:middle;"></span>
            Línea guía
        </div>
        <div class="flex items-center gap-1.5">
            <span class="inline-block w-8 border-t-2 border-green-500" style="vertical-align:middle;"></span>
            Ruta real (OSRM)
        </div>
        <div class="ml-auto" x-show="deleteMode" style="display:none;">
            <span class="text-red-500 font-medium">Modo eliminar — haz clic en un waypoint naranja para borrarlo</span>
        </div>
        <div class="ml-auto" x-show="!deleteMode">
            Haz clic en el mapa para añadir waypoints entre paradas
        </div>
    </div>

    {{-- ── Mapa ── --}}
    <div id="route-map"
         class="w-full rounded-xl ring-1 ring-gray-200 dark:ring-gray-700 shadow-sm overflow-hidden"
         style="height: 560px;">
    </div>

    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
</div>

<script>
function busRouteEditor(stops, existingWaypoints) {
    return {
        stops,
        waypoints:       [],
        waypointMarkers: [],
        guideLine:       null,
        osrmLines:       [],
        map:             null,
        deleteMode:      false,
        saving:          false,
        previewLoading:  false,

        // ── Bootstrap ────────────────────────────────────────────────────
        init() {
            this.waitForLeaflet(() => this.setupMap());
        },

        waitForLeaflet(cb) {
            if (window.L) { cb(); return; }
            const t = setInterval(() => { if (window.L) { clearInterval(t); cb(); } }, 50);
        },

        setupMap() {
            const center = stops.length ? [stops[0].lat, stops[0].lng] : [7.1193, -73.1227];
            this.map = L.map('route-map', { center, zoom: 14 });

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
                maxZoom: 19,
            }).addTo(this.map);

            // Marcadores de paradas (numerados, fijos)
            stops.forEach((s, i) => {
                L.marker([s.lat, s.lng], {
                    icon: L.divIcon({
                        className: '',
                        html: `<div style="width:28px;height:28px;background:#2563eb;border:3px solid white;border-radius:50%;box-shadow:0 2px 6px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:white;font-family:system-ui,sans-serif;">${i+1}</div>`,
                        iconSize: [28, 28], iconAnchor: [14, 14],
                    })
                }).bindTooltip(`<b>${this.esc(s.name)}</b>`, { direction: 'top' }).addTo(this.map);
            });

            if (stops.length > 1) {
                const bounds = L.latLngBounds(stops.map(s => [s.lat, s.lng]));
                this.map.fitBounds(bounds, { padding: [50, 50] });
            }

            // Cargar waypoints existentes
            existingWaypoints.forEach(wp => this.addWaypointAt(wp.lat, wp.lng, wp.label || ''));
            this.drawGuideLine();

            // Clic en el mapa → añadir waypoint
            this.map.on('click', e => {
                if (!this.deleteMode) {
                    this.addWaypointAt(e.latlng.lat, e.latlng.lng, '');
                    this.drawGuideLine();
                }
            });
        },

        // ── Gestión de waypoints ─────────────────────────────────────────
        addWaypointAt(lat, lng, label) {
            const wpIndex = this.waypoints.length;
            this.waypoints.push({ lat, lng, label });

            const mkr = L.marker([lat, lng], {
                draggable: true,
                icon: L.divIcon({
                    className: '',
                    html: '<div style="width:22px;height:22px;background:#f97316;border:3px solid white;border-radius:50%;box-shadow:0 2px 6px rgba(0,0,0,.4);cursor:grab;"></div>',
                    iconSize: [22, 22], iconAnchor: [11, 11],
                }),
            }).addTo(this.map);

            mkr.on('dragend', e => {
                const i = this.waypointMarkers.indexOf(mkr);
                if (i !== -1) {
                    const p = e.target.getLatLng();
                    this.waypoints[i].lat = p.lat;
                    this.waypoints[i].lng = p.lng;
                    this.drawGuideLine();
                }
            });

            mkr.on('click', () => {
                if (this.deleteMode) {
                    const i = this.waypointMarkers.indexOf(mkr);
                    if (i !== -1) { this.removeAt(i); this.drawGuideLine(); }
                }
            });

            this.waypointMarkers.push(mkr);
        },

        removeAt(i) {
            this.waypointMarkers[i].remove();
            this.waypoints.splice(i, 1);
            this.waypointMarkers.splice(i, 1);
        },

        clearWaypoints() {
            this.waypointMarkers.forEach(m => m.remove());
            this.waypoints = []; this.waypointMarkers = [];
            this.drawGuideLine();
            this.osrmLines.forEach(l => l.remove()); this.osrmLines = [];
        },

        // ── Línea guía (recta, instantánea) ─────────────────────────────
        drawGuideLine() {
            if (this.guideLine) { this.guideLine.remove(); this.guideLine = null; }
            const path = this.buildOrderedPath();
            if (path.length < 2) return;
            this.guideLine = L.polyline(path.map(p => [p.lat, p.lng]), {
                color: '#6366f1', weight: 3, opacity: 0.55, dashArray: '7 7',
            }).addTo(this.map);
        },

        // ── Vista previa OSRM ─────────────────────────────────────────────
        async loadOsrmPreview() {
            if (stops.length < 2 || this.previewLoading) return;
            this.previewLoading = true;
            const path = this.buildOrderedPath();
            const coordStr = path.map(p => `${p.lng},${p.lat}`).join(';');
            try {
                const res = await fetch(
                    `https://router.project-osrm.org/route/v1/driving/${coordStr}?overview=full&geometries=polyline`
                );
                if (!res.ok) throw new Error();
                const data = await res.json();
                if (data.code !== 'Ok' || !data.routes?.[0]?.geometry) throw new Error();

                const pts = this.decodePolyline(data.routes[0].geometry);
                this.osrmLines.forEach(l => l.remove()); this.osrmLines = [];
                this.osrmLines.push(
                    L.polyline(pts, { color: '#22c55e', weight: 10, opacity: 0.2 }).addTo(this.map),
                    L.polyline(pts, { color: '#22c55e', weight:  5, opacity: 0.9 }).addTo(this.map),
                );
            } catch {
                alert('No se pudo obtener la ruta de OSRM. Verifica la conexión o la posición de los puntos.');
            } finally {
                this.previewLoading = false;
            }
        },

        // ── Guardar ───────────────────────────────────────────────────────
        async saveRouteAction() {
            if (this.saving) return;
            this.saving = true;
            try {
                await this.$wire.saveRoute(this.prepareForSave());
            } finally {
                this.saving = false;
            }
        },

        // ── Calcular órdenes y devolver array listo para el backend ────────
        prepareForSave() {
            if (this.waypoints.length === 0) return [];
            const N = stops.length;

            if (N < 2) {
                // Sin dos paradas no se puede calcular segmentos; usar índice * 10
                return this.waypoints.map((wp, i) => ({
                    lat: wp.lat, lng: wp.lng, label: wp.label, order: (i + 1) * 10,
                }));
            }

            // Asignar cada waypoint al segmento más cercano
            const tagged = this.waypoints.map(wp => {
                let best = { seg: 0, dist: Infinity, t: 0.5 };
                for (let i = 0; i < N - 1; i++) {
                    const { dist, t } = this.ptSegDist(
                        wp.lat, wp.lng,
                        stops[i].lat, stops[i].lng,
                        stops[i + 1].lat, stops[i + 1].lng,
                    );
                    if (dist < best.dist) best = { seg: i, dist, t };
                }
                return { ...wp, _seg: best.seg, _t: best.t };
            });

            // Agrupar por segmento, ordenar por t, asignar orders
            const result = [];
            for (let seg = 0; seg < N - 1; seg++) {
                const group = tagged.filter(w => w._seg === seg).sort((a, b) => a._t - b._t);
                if (!group.length) continue;

                const orderA = stops[seg].pivotOrder     * 100;  // ej. 100
                const orderB = stops[seg + 1].pivotOrder * 100;  // ej. 200
                const step   = (orderB - orderA) / (group.length + 1);

                group.forEach((wp, i) => result.push({
                    lat:   wp.lat,
                    lng:   wp.lng,
                    label: wp.label || '',
                    order: Math.round(orderA + step * (i + 1)),
                }));
            }
            return result;
        },

        // ── Ordenar todos los puntos del recorrido (stops + waypoints) ────
        buildOrderedPath() {
            const N = stops.length;
            if (N === 0) return this.waypoints;
            if (N === 1) return [stops[0], ...this.waypoints];

            const tagged = this.waypoints.map(wp => {
                let best = { seg: 0, dist: Infinity, t: 0.5 };
                for (let i = 0; i < N - 1; i++) {
                    const { dist, t } = this.ptSegDist(
                        wp.lat, wp.lng,
                        stops[i].lat, stops[i].lng,
                        stops[i + 1].lat, stops[i + 1].lng,
                    );
                    if (dist < best.dist) best = { seg: i, dist, t };
                }
                return { ...wp, _seg: best.seg, _t: best.t };
            });

            const path = [];
            for (let i = 0; i < N; i++) {
                path.push(stops[i]);
                tagged.filter(w => w._seg === i).sort((a, b) => a._t - b._t).forEach(w => path.push(w));
            }
            return path;
        },

        // ── Geometría: distancia punto a segmento ─────────────────────────
        ptSegDist(px, py, ax, ay, bx, by) {
            const dx = bx - ax, dy = by - ay;
            const lenSq = dx * dx + dy * dy;
            const t = lenSq > 0 ? Math.max(0, Math.min(1, ((px-ax)*dx + (py-ay)*dy) / lenSq)) : 0;
            return { dist: Math.hypot(px - ax - t*dx, py - ay - t*dy), t };
        },

        // ── Decodificador de polilínea (Google/OSRM) ──────────────────────
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

        esc(s) {
            return s ? String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;') : '';
        },
    };
}
</script>

</x-filament-panels::page>
