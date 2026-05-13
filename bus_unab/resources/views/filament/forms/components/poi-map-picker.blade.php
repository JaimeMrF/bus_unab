@php
    $record = $getRecord();
    $initLat = $record?->latitude ? (float) $record->latitude : null;
    $initLng = $record?->longitude ? (float) $record->longitude : null;
    $apiKey = config('services.maps.key');

    $others = \App\Models\PointOfInterest::active()
        ->whereNotNull('latitude')
        ->whereNotNull('longitude')
        ->when($record, fn($q) => $q->where('id', '!=', $record->id))
        ->get(['id', 'name', 'latitude', 'longitude', 'category'])
        ->toArray();
@endphp

<div wire:ignore x-data="googlePoiMapPicker(@js($initLat), @js($initLng), @js($others))" class="col-span-full space-y-3">
    {{-- Buscador de Google Places --}}
    <div class="w-full relative">
        <input id="poi-search-input" type="text" placeholder="Buscar lugar en Google Maps..." 
            class="w-full transition duration-75 rounded-lg shadow-sm focus:border-primary-500 focus:ring-1 focus:ring-inset focus:ring-primary-500 disabled:opacity-70 bg-white dark:bg-white/5 border-gray-300 dark:border-white/10"
            style="padding: 0.5rem 0.75rem;">
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
    <div id="google-poi-map"
        class="w-full rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden"
        style="height: 480px;"></div>

    {{-- Cargar Google Maps API --}}
    <script
        src="https://maps.googleapis.com/maps/api/js?key={{ $apiKey }}&libraries=places,marker&callback=initGoogleMapPoiPicker&loading=async"
        async></script>

    <script>
        window.initGoogleMapPoiPicker = () => {
            window.dispatchEvent(new CustomEvent('google-maps-loaded'));
        };

        function googlePoiMapPicker(initLat, initLng, otherPois) {
            return {
                map: null,
                marker: null,

                init() {
                    if (window.google && window.google.maps) {
                        this.setupMap();
                    } else {
                        window.addEventListener('google-maps-loaded', () => this.setupMap());
                    }
                },

                setupMap() {
                    const hasPos = initLat != null && initLng != null;
                    const center = hasPos ? {
                        lat: initLat,
                        lng: initLng
                    } : {
                        lat: 7.1193,
                        lng: -73.1227
                    };

                    this.map = new google.maps.Map(document.getElementById('google-poi-map'), {
                        center: center,
                        zoom: hasPos ? 17 : 14,
                        mapId: 'POI_PICKER_MAP', // Requerido para marcadores avanzados
                        mapTypeControl: false,
                        streetViewControl: false,
                        fullscreenControl: true,
                    });

                    // Configurar el Autocomplete clásico de Google Maps en nuestro input
                    const searchInput = document.getElementById('poi-search-input');
                    if (searchInput) {
                        const autocomplete = new google.maps.places.Autocomplete(searchInput, {
                            componentRestrictions: { country: 'co' },
                            fields: ['geometry', 'name']
                        });

                        autocomplete.addListener('place_changed', () => {
                            const place = autocomplete.getPlace();
                            if (!place.geometry || !place.geometry.location) return;
                            
                            const lat = place.geometry.location.lat();
                            const lng = place.geometry.location.lng();
                            
                            this.map.setCenter({ lat, lng });
                            this.map.setZoom(17);
                            this.placeMarker(lat, lng, true);

                            // Opcional: setear el nombre del punto si está vacío
                            const nameEl = document.getElementById('data.name');
                            if (nameEl && nameEl.value.trim() === '') {
                                this.$wire.$set('data.name', place.name ?? '');
                            }
                        });

                        // Evitar submit del formulario con Enter
                        searchInput.addEventListener('keydown', (e) => {
                            if (e.key === 'Enter') {
                                e.preventDefault();
                            }
                        });
                    }

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
                            position: {
                                lat: parseFloat(s.latitude),
                                lng: parseFloat(s.longitude)
                            },
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
                    const position = {
                        lat,
                        lng
                    };

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
