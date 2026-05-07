@php
    $record  = $getRecord();
    $initLat = $record?->latitude  ? (float) $record->latitude  : null;
    $initLng = $record?->longitude ? (float) $record->longitude : null;
    $initRad = (int) ($record?->radius_meters ?? 100);

    $others = \App\Models\Stop::active()
        ->whereNotNull('latitude')
        ->whereNotNull('longitude')
        ->when($record, fn ($q) => $q->where('id', '!=', $record->id))
        ->get(['id', 'name', 'latitude', 'longitude', 'radius_meters'])
        ->toArray();
@endphp

{{-- wire:ignore evita que Livewire destruya el mapa en cada re-render --}}
<div
    wire:ignore
    x-data="stopMapPicker(@js($initLat), @js($initLng), @js($initRad), @js($others))"
    class="col-span-full space-y-2"
>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />

    <style>
        #stop-map-picker .leaflet-pane,
        #stop-map-picker .leaflet-tile         { z-index: 1 !important; }
        #stop-map-picker .leaflet-overlay-pane { z-index: 2 !important; }
        #stop-map-picker .leaflet-shadow-pane  { z-index: 3 !important; }
        #stop-map-picker .leaflet-marker-pane  { z-index: 4 !important; }
        #stop-map-picker .leaflet-tooltip-pane { z-index: 5 !important; }
        #stop-map-picker .leaflet-popup-pane   { z-index: 6 !important; }
        #stop-map-picker .leaflet-top,
        #stop-map-picker .leaflet-bottom       { z-index: 7 !important; }
    </style>

    {{-- Instrucción + indicador de geocodificación --}}
    <div class="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
        <svg class="w-4 h-4 flex-shrink-0 text-primary-500" fill="none" viewBox="0 0 24 24"
             stroke-width="2" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/>
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z"/>
        </svg>
        <span>Haz clic en el mapa para fijar la ubicación. Puedes arrastrar el marcador para ajustarla.</span>
        <span x-show="geocoding"
              class="ml-1 text-xs text-primary-500 animate-pulse"
              style="display:none;">
            Buscando dirección…
        </span>
    </div>

    {{-- Leyenda de colores --}}
    <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
        <div class="flex items-center gap-1.5">
            <span class="inline-block w-3 h-3 rounded-full bg-red-500 border-2 border-white shadow-sm"></span>
            <span>Parada actual</span>
        </div>
        <div class="flex items-center gap-1.5">
            <span class="inline-block w-3 h-3 rounded-full bg-blue-500 border-2 border-white shadow-sm"></span>
            <span>Otras paradas</span>
        </div>
    </div>

    {{-- Contenedor del mapa --}}
    <div
        id="stop-map-picker"
        class="w-full rounded-lg ring-1 ring-gray-200 dark:ring-gray-700 shadow-sm overflow-hidden"
        style="height: 440px;"
    ></div>

    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

    <script>
function stopMapPicker(initLat, initLng, initRadius, otherStops) {
    return {
        map:       null,
        marker:    null,
        circle:    null,
        geocoding: false,

        init() {
            this.waitForLeaflet(() => this.setupMap());
        },

        waitForLeaflet(cb) {
            if (window.L) { cb(); return; }
            const t = setInterval(() => { if (window.L) { clearInterval(t); cb(); } }, 50);
        },

        setupMap() {
            const hasPos = initLat != null && initLng != null;
            const center = hasPos ? [initLat, initLng] : [7.1193, -73.1227];

            this.map = L.map('stop-map-picker', { center, zoom: hasPos ? 16 : 14 });

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
                maxZoom: 19,
            }).addTo(this.map);

            // Marcadores de referencia: otras paradas (azul)
            const blueIcon = L.divIcon({
                className: '',
                html: '<div title="" style="width:18px;height:18px;background:#3b82f6;border:2px solid white;border-radius:50% 50% 50% 0;transform:rotate(-45deg);box-shadow:0 1px 4px rgba(0,0,0,.3);"></div>',
                iconSize:   [18, 18],
                iconAnchor: [9, 18],
            });

            otherStops.forEach(s => {
                if (!s.latitude || !s.longitude) return;
                L.marker([s.latitude, s.longitude], { icon: blueIcon })
                    .bindTooltip(this.esc(s.name), { direction: 'top', sticky: false })
                    .addTo(this.map);
            });

            // Si estamos editando, mostrar la posición actual
            if (hasPos) {
                this.placeMarker(initLat, initLng, false);
            }

            // Clic en el mapa → colocar/mover parada
            this.map.on('click', e => {
                this.placeMarker(e.latlng.lat, e.latlng.lng, true);
            });
        },

        redIcon() {
            return L.divIcon({
                className: '',
                html: '<div style="width:30px;height:30px;background:#ef4444;border:3px solid white;border-radius:50% 50% 50% 0;transform:rotate(-45deg);box-shadow:0 3px 8px rgba(0,0,0,.4);"></div>',
                iconSize:   [30, 30],
                iconAnchor: [15, 30],
            });
        },

        placeMarker(lat, lng, updateForm) {
            if (this.marker) {
                this.marker.setLatLng([lat, lng]);
            } else {
                this.marker = L.marker([lat, lng], {
                    icon: this.redIcon(),
                    draggable: true,
                }).addTo(this.map);

                this.marker.on('dragend', e => {
                    const p = e.target.getLatLng();
                    this.updateCirclePos(p.lat, p.lng);
                    this.pushCoords(p.lat, p.lng);
                });
            }

            this.updateCirclePos(lat, lng);

            if (updateForm) {
                this.pushCoords(lat, lng);
                this.reverseGeocode(lat, lng);
            }
        },

        updateCirclePos(lat, lng) {
            const r = this.currentRadius();
            if (this.circle) {
                this.circle.setLatLng([lat, lng]).setRadius(r);
            } else {
                this.circle = L.circle([lat, lng], {
                    radius:      r,
                    color:       '#ef4444',
                    fillColor:   '#ef4444',
                    fillOpacity: 0.10,
                    weight:      2,
                    dashArray:   '6 4',
                }).addTo(this.map);
            }
        },

        currentRadius() {
            // Lee el radio del input de Filament; fallback al valor inicial
            const el = document.getElementById('data.radius_meters');
            return el ? (parseInt(el.value) || initRadius) : initRadius;
        },

        pushCoords(lat, lng) {
            this.$wire.$set('data.latitude',  parseFloat(lat).toFixed(7));
            this.$wire.$set('data.longitude', parseFloat(lng).toFixed(7));
        },

        async reverseGeocode(lat, lng) {
            // Rellena la dirección solo si el campo está vacío
            const addrEl = document.getElementById('data.address');
            if (addrEl && addrEl.value.trim() !== '') return;

            this.geocoding = true;
            try {
                const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`;
                const res = await fetch(url, { headers: { 'Accept-Language': 'es' } });
                if (!res.ok) return;
                const data = await res.json();
                const a    = data.address ?? {};
                const parts = [
                    a.road || a.pedestrian || a.path || '',
                    a.suburb || a.neighbourhood || a.quarter || '',
                    a.city || a.town || a.village || '',
                ].filter(Boolean);
                const address = parts.join(', ');
                if (address && addrEl && addrEl.value.trim() === '') {
                    this.$wire.$set('data.address', address);
                }
            } catch (_) {
                // Fallo silencioso — el usuario puede escribir la dirección manualmente
            } finally {
                this.geocoding = false;
            }
        },

        esc(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;').replace(/</g, '&lt;')
                .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        },
    };
}
    </script>
</div>
