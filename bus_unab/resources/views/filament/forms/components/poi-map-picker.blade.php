@php
    $record = $getRecord();
    $initLat = $record?->latitude ? (float) $record->latitude : null;
    $initLng = $record?->longitude ? (float) $record->longitude : null;

    $others = \App\Models\PointOfInterest::active()
        ->whereNotNull('latitude')
        ->whereNotNull('longitude')
        ->when($record, fn($q) => $q->where('id', '!=', $record->id))
        ->get(['id', 'name', 'latitude', 'longitude', 'category'])
        ->toArray();
@endphp

<div wire:ignore x-data="poiMapPicker(@js($initLat), @js($initLng), @js($others))" class="col-span-full space-y-3">
    @include('filament.partials.leaflet-loader')

    {{-- Buscador de lugares (OpenStreetMap) --}}
    <div class="flex gap-2">
        <input x-model="query" @keydown.enter.prevent="search()" type="text" placeholder="Buscar lugar en Bucaramanga..."
            class="flex-1 rounded-lg shadow-sm bg-white dark:bg-white/5 border-gray-300 dark:border-white/10 text-sm"
            style="padding: 0.5rem 0.75rem;">
        <button type="button" @click="search()" class="fi-btn rounded-lg px-3 py-2 text-sm font-semibold bg-primary-600 text-white">Buscar</button>
    </div>

    {{-- Leyenda --}}
    <div class="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 px-1">
        <div class="flex items-center gap-4">
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-amber-500 border-2 border-white shadow-sm"></span>
                <span>Punto de Interés actual (clic en el mapa o arrastra)</span>
            </div>
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-blue-500 border-2 border-white shadow-sm"></span>
                <span>Otros puntos</span>
            </div>
        </div>
        <span x-show="busy" class="text-primary-500 animate-pulse" style="display:none;">Buscando...</span>
        <span x-show="notFound" class="text-danger-500" style="display:none;">Sin resultados</span>
    </div>

    <div x-ref="map" class="w-full rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden" style="height: 480px;"></div>

    <script>
        function poiMapPicker(initLat, initLng, otherPois) {
            const lf = { L: null, map: null, marker: null, circle: null };
            return {
                // (lf: objetos de Leaflet fuera del estado reactivo de Alpine)
                query: '', busy: false, notFound: false,

                async init() {
                    const L = lf.L = await window.loadLeaflet();
                    const hasPos = initLat != null && initLng != null;
                    lf.map = L.map(this.$refs.map).setView(hasPos ? [initLat, initLng] : [7.1193, -73.1227], hasPos ? 17 : 14);
                    window.osmLayer(L).addTo(lf.map);

                    otherPois.forEach(s => L.marker([+s.latitude, +s.longitude], { icon: window.pinIcon(L, '#3b82f6', 14) })
                        .bindTooltip(s.name).addTo(lf.map));

                    if (hasPos) this.placeMarker(initLat, initLng, false);
                    lf.map.on('click', e => this.placeMarker(e.latlng.lat, e.latlng.lng, true));
                },

                async search() {
                    if (!this.query.trim()) return;
                    this.busy = true; this.notFound = false;
                    try {
                        const hit = await window.geoSearch(this.query + ', Santander');
                        if (!hit) { this.notFound = true; return; }
                        lf.map.setView([hit.lat, hit.lng], 17);
                        this.placeMarker(hit.lat, hit.lng, true);
                        const nameEl = document.getElementById('data.name');
                        if (nameEl && !nameEl.value.trim() && hit.name) this.$wire.$set('data.name', hit.name);
                    } finally { this.busy = false; }
                },

                placeMarker(lat, lng, updateForm) {
                    if (lf.marker) {
                        lf.marker.setLatLng([lat, lng]);
                    } else {
                        lf.marker = lf.L.marker([lat, lng], { draggable: true, icon: window.pinIcon(lf.L, '#f59e0b', 22) }).addTo(lf.map);
                        lf.marker.on('dragend', () => {
                            const p = lf.marker.getLatLng();
                            this.pushCoords(p.lat, p.lng);
                        });
                    }
                    if (updateForm) this.pushCoords(lat, lng);
                },

                pushCoords(lat, lng) {
                    this.$wire.$set('data.latitude', (+lat).toFixed(7));
                    this.$wire.$set('data.longitude', (+lng).toFixed(7));
                },
            };
        }
    </script>
</div>
