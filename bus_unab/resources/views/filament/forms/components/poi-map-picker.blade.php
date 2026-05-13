@php
    $record  = $getRecord();
    $initLat = $record?->latitude  ? (float) $record->latitude  : null;
    $initLng = $record?->longitude ? (float) $record->longitude : null;
    $apiKey  = env('GOOGLE_MAPS_API_KEY');

    $others = \App\Models\PointOfInterest::active()
        ->whereNotNull('latitude')
        ->whereNotNull('longitude')
        ->when($record, fn ($q) => $q->where('id', '!=', $record->id))
        ->get(['id', 'name', 'latitude', 'longitude', 'category'])
        ->toArray();
@endphp

<div
    wire:ignore
    x-data="googlePoiMapPicker(@js($initLat), @js($initLng), @js($others))"
    class="col-span-full space-y-3"
>
    {{-- Buscador de Google Places --}}
    <div class="relative w-full">
        <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
            <svg class="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
        </div>
        <input 
            type="text" 
            id="poi-map-search-input" 
            placeholder="Buscar lugar o dirección en Bucaramanga..."
            class="block w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-sm focus:ring-primary-500 focus:border-primary-500 shadow-sm"
        >
    </div>

    {{-- Leyenda --}}
    <div class="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 px-1">
        <div class="flex items-center gap-4">
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-amber-500 border-2 border-white shadow-sm"></span>
                <span>Punto de Interés actual</span>
            </div>
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-blue-500 border-2 border-white shadow-sm"></span>
                <span>Otros puntos</span>
            </div>
        </div>
    </div>

    {{-- Contenedor del mapa --}}
    <div
        id="google-poi-map"
        class="w-full rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden"
        style="height: 480px;"
    ></div>

    {{-- Cargar Google Maps API si no está cargada --}}
    @if (!request()->hasCookie('google_maps_loaded'))
    <script src="https://maps.googleapis.com/maps/api/js?key={{ $apiKey }}&libraries=places&callback=initGoogleMapPoiPicker" async defer></script>
    @endif

    <script>
        window.initGoogleMapPoiPicker = () => {
            window.dispatchEvent(new CustomEvent('google-maps-loaded'));
        };

        function googlePoiMapPicker(initLat, initLng, otherPois) {
            return {
                map: null,
                marker: null,
                autocomplete: null,

                init() {
                    if (window.google && window.google.maps) {
                        this.setupMap();
                    } else {
                        window.addEventListener('google-maps-loaded', () => this.setupMap());
                    }
                },

                setupMap() {
                    const hasPos = initLat != null && initLng != null;
                    const center = hasPos ? { lat: initLat, lng: initLng } : { lat: 7.1193, lng: -73.1227 };

                    this.map = new google.maps.Map(document.getElementById('google-poi-map'), {
                        center: center,
                        zoom: hasPos ? 17 : 14,
                        mapId: 'POI_PICKER_MAP', // Requerido para marcadores avanzados
                        mapTypeControl: false,
                        streetViewControl: false,
                        fullscreenControl: true,
                    });

                    // Setup Autocomplete
                    const input = document.getElementById('poi-map-search-input');
                    this.autocomplete = new google.maps.places.Autocomplete(input, {
                        componentRestrictions: { country: "co" },
                        fields: ["geometry", "name"],
                        strictBounds: false,
                    });

                    this.autocomplete.addListener("place_changed", () => {
                        const place = this.autocomplete.getPlace();
                        if (!place.geometry || !place.geometry.location) return;

                        const pos = place.geometry.location;
                        this.map.setCenter(pos);
                        this.map.setZoom(17);
                        this.placeMarker(pos.lat(), pos.lng(), true);
                        
                        // Opcional: Podríamos autocompletar el nombre si está vacío
                        const nameEl = document.getElementById('data.name');
                        if (nameEl && nameEl.value.trim() === '') {
                            this.$wire.$set('data.name', place.name);
                        }
                    });

                    // Marcadores de referencia: otros puntos (azul)
                    otherPois.forEach(s => {
                        if (!s.latitude || !s.longitude) return;
                        
                        const pinView = new google.maps.marker.PinElement({
                            background: "#3b82f6",
                            borderColor: "white",
                            glyphColor: "white",
                            scale: 0.7
                        });

                        new google.maps.marker.AdvancedMarkerElement({
                            map: this.map,
                            position: { lat: parseFloat(s.latitude), lng: parseFloat(s.longitude) },
                            title: s.name,
                            content: pinView.element
                        });
                    });

                    // Si ya tiene posición, colocar marcador
                    if (hasPos) {
                        this.placeMarker(initLat, initLng, false);
                    }

                    // Clic en el mapa para mover marcador
                    this.map.addListener('click', (e) => {
                        this.placeMarker(e.latLng.lat(), e.latLng.lng(), true);
                    });
                },

                placeMarker(lat, lng, updateForm) {
                    const position = { lat, lng };

                    if (this.marker) {
                        this.marker.position = position;
                    } else {
                        const pinView = new google.maps.marker.PinElement({
                            background: "#f59e0b", // Amber 500
                            borderColor: "white",
                            glyphColor: "white",
                        });

                        this.marker = new google.maps.marker.AdvancedMarkerElement({
                            map: this.map,
                            position: position,
                            gmpDraggable: true,
                            content: pinView.element
                        });

                        this.marker.addListener('dragend', (e) => {
                            const pos = this.marker.position;
                            this.pushCoords(pos.lat, pos.lng);
                        });
                    }

                    if (updateForm) {
                        this.pushCoords(lat, lng);
                    }
                },

                pushCoords(lat, lng) {
                    this.$wire.$set('data.latitude', parseFloat(lat).toFixed(7));
                    this.$wire.$set('data.longitude', parseFloat(lng).toFixed(7));
                }
            };
        }
    </script>
</div>
