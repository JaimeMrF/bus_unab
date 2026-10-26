@php
    $record = $getRecord();
    $initLat = $record?->latitude ? (float) $record->latitude : null;
    $initLng = $record?->longitude ? (float) $record->longitude : null;
    $initRad = (int) ($record?->radius_meters ?? 100);

    $others = \App\Models\Stop::active()
        ->whereNotNull('latitude')
        ->whereNotNull('longitude')
        ->when($record, fn($q) => $q->where('id', '!=', $record->id))
        ->get(['id', 'name', 'latitude', 'longitude', 'radius_meters'])
        ->toArray();
@endphp

<div wire:ignore x-data="stopMapPicker(@js($initLat), @js($initLng), @js($initRad), @js($others))" class="col-span-full space-y-3">
    @include('filament.partials.leaflet-loader')

    {{-- Buscador de lugares (OpenStreetMap) --}}
    <div class="flex gap-2">
        <input x-model="query" @keydown.enter.prevent="search()" type="text" placeholder="Buscar dirección o lugar en Bucaramanga..."
            class="flex-1 rounded-lg shadow-sm bg-white dark:bg-white/5 border-gray-300 dark:border-white/10 text-sm"
            style="padding: 0.5rem 0.75rem;">
        <button type="button" @click="search()" class="fi-btn rounded-lg px-3 py-2 text-sm font-semibold bg-primary-600 text-white">Buscar</button>
    </div>

    {{-- Leyenda y estado --}}
    <div class="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 px-1">
        <div class="flex items-center gap-4">
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-red-500 border-2 border-white shadow-sm"></span>
                <span>Parada actual (clic en el mapa o arrastra)</span>
            </div>
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-blue-500 border-2 border-white shadow-sm"></span>
                <span>Otras paradas</span>
            </div>
        </div>
        <span x-show="busy" class="text-primary-500 animate-pulse" style="display:none;">Buscando...</span>
        <span x-show="notFound" class="text-danger-500" style="display:none;">Sin resultados</span>
    </div>

    <div x-ref="map" class="w-full rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden" style="height: 480px;"></div>

    <script>
        function stopMapPicker(initLat, initLng, initRadius, otherStops) {
            const lf = { L: null, map: null, marker: null, circle: null };
            return {
                // (lf: objetos de Leaflet fuera del estado reactivo de Alpine)
                query: '', busy: false, notFound: false,

                async init() {
                    const L = lf.L = await window.loadLeaflet();
                    const hasPos = initLat != null && initLng != null;
                    lf.map = L.map(this.$refs.map).setView(hasPos ? [initLat, initLng] : [7.1193, -73.1227], hasPos ? 17 : 14);
                    window.osmLayer(L).addTo(lf.map);

                    otherStops.forEach(s => L.marker([+s.latitude, +s.longitude], { icon: window.pinIcon(L, '#3b82f6', 14) })
                        .bindTooltip(s.name).addTo(lf.map));

                    if (hasPos) this.placeMarker(initLat, initLng, false);
                    lf.map.on('click', e => this.placeMarker(e.latlng.lat, e.latlng.lng, true));

                    // El radio sigue al campo "Radio" del formulario
                    document.getElementById('data.radius_meters')?.addEventListener('input', () => lf.circle?.setRadius(this.currentRadius()));
                },

                async search() {
                    if (!this.query.trim()) return;
                    this.busy = true; this.notFound = false;
                    try {
                        const hit = await window.geoSearch(this.query + ', Santander');
                        if (!hit) { this.notFound = true; return; }
                        lf.map.setView([hit.lat, hit.lng], 17);
                        this.placeMarker(hit.lat, hit.lng, true, hit.address);
                        const nameEl = document.getElementById('data.name');
                        if (nameEl && !nameEl.value.trim() && hit.name) this.$wire.$set('data.name', hit.name);
                    } finally { this.busy = false; }
                },

                placeMarker(lat, lng, updateForm, address = null) {
                    const L = lf.L;
                    if (lf.marker) {
                        lf.marker.setLatLng([lat, lng]);
                    } else {
                        lf.marker = L.marker([lat, lng], { draggable: true, icon: window.pinIcon(L, '#ef4444', 22) }).addTo(lf.map);
                        lf.marker.on('dragend', () => {
                            const p = lf.marker.getLatLng();
                            this.placeMarker(p.lat, p.lng, true);
                        });
                    }
                    if (lf.circle) {
                        lf.circle.setLatLng([lat, lng]).setRadius(this.currentRadius());
                    } else {
                        lf.circle = L.circle([lat, lng], { radius: this.currentRadius(), color: '#ef4444', weight: 2, fillOpacity: 0.15, interactive: false }).addTo(lf.map);
                    }
                    if (updateForm) {
                        this.$wire.$set('data.latitude', (+lat).toFixed(7));
                        this.$wire.$set('data.longitude', (+lng).toFixed(7));
                        this.fillAddress(lat, lng, address);
                    }
                },

                currentRadius() {
                    const el = document.getElementById('data.radius_meters');
                    return el ? (parseInt(el.value) || initRadius) : initRadius;
                },

                // Solo completa la dirección si el campo está vacío
                async fillAddress(lat, lng, address) {
                    const el = document.getElementById('stop-address-input');
                    if (el && el.value.trim() !== '') return;
                    try {
                        address ??= await window.geoReverse(lat, lng);
                        if (address) this.$wire.$set('data.address', address);
                    } catch (e) { /* sin red: la dirección se escribe a mano */ }
                },
            };
        }
    </script>
</div>
