@php
    $record = $getRecord();
    $initLat = $record?->latitude ? (float) $record->latitude : null;
    $initLng = $record?->longitude ? (float) $record->longitude : null;
    $initRad = (int) ($record?->radius_meters ?? 100);
    $apiKey = config('services.maps.key');

    $others = \App\Models\Stop::active()
        ->whereNotNull('latitude')
        ->whereNotNull('longitude')
        ->when($record, fn($q) => $q->where('id', '!=', $record->id))
        ->get(['id', 'name', 'latitude', 'longitude', 'radius_meters'])
        ->toArray();
@endphp

<div wire:ignore x-data="googleStopMapPicker(@js($initLat), @js($initLng), @js($initRad), @js($others))" class="col-span-full space-y-3">
    {{-- El buscador de Google Places ahora está enlazado directamente al campo 'Dirección' de Filament --}}

    {{-- Leyenda y estado --}}
    <div class="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 px-1">
        <div class="flex items-center gap-4">
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-red-500 border-2 border-white shadow-sm"></span>
                <span>Parada actual</span>
            </div>
            <div class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-full bg-blue-500 border-2 border-white shadow-sm"></span>
                <span>Otras paradas</span>
            </div>
        </div>
        <span x-show="geocoding" class="text-primary-500 animate-pulse" style="display:none;">Buscando
            dirección...</span>
    </div>

    {{-- Contenedor del mapa --}}
    <div id="google-stop-map"
        class="w-full rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden"
        style="height: 480px;"></div>

    {{-- Cargar Google Maps API --}}
    <script
        src="https://maps.googleapis.com/maps/api/js?key={{ $apiKey }}&libraries=places,marker&callback=initGoogleMapPicker&loading=async"
        async></script>

    <script>
        // Callback global para la API de Google
        window.initGoogleMapPicker = () => {
            window.dispatchEvent(new CustomEvent('google-maps-loaded'));
        };

        function googleStopMapPicker(initLat, initLng, initRadius, otherStops) {
            return {
                map: null,
                marker: null,
                circle: null,
                geocoder: null,
                geocoding: false,

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

                    this.map = new google.maps.Map(document.getElementById('google-stop-map'), {
                        center: center,
                        zoom: hasPos ? 17 : 14,
                        mapId: 'STOP_PICKER_MAP', // Requerido para marcadores avanzados
                        mapTypeControl: false,
                        streetViewControl: false,
                        fullscreenControl: true,
                    });

                    this.geocoder = new google.maps.Geocoder();

                    // Configurar el Autocomplete en el input de Dirección de Filament
                    const addressInput = document.getElementById('stop-address-input');
                    if (addressInput) {
                        const autocomplete = new google.maps.places.Autocomplete(addressInput, {
                            componentRestrictions: { country: 'co' },
                            fields: ['geometry', 'name', 'formatted_address']
                        });

                        autocomplete.addListener('place_changed', () => {
                            const place = autocomplete.getPlace();
                            if (!place.geometry || !place.geometry.location) return;
                            
                            const lat = place.geometry.location.lat();
                            const lng = place.geometry.location.lng();
                            
                            this.map.setCenter({ lat, lng });
                            this.map.setZoom(17);
                            this.placeMarker(lat, lng, true);
                            
                            if (place.formatted_address) {
                                this.$wire.$set('data.address', place.formatted_address);
                            }

                            // Autocompletar nombre si está vacío
                            const nameEl = document.getElementById('data.name');
                            if (nameEl && (nameEl.value || '').trim() === '') {
                                this.$wire.$set('data.name', place.name || '');
                            }
                        });

                        // Prevenir que la tecla Enter envíe el formulario al seleccionar una sugerencia
                        addressInput.addEventListener('keydown', (e) => {
                            if (e.key === 'Enter') {
                                e.preventDefault();
                            }
                        });
                    }

                    // Marcadores de referencia: otras paradas (azul)
                    otherStops.forEach(s => {
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
                            background: "#ef4444",
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
                            this.updateCirclePos(pos.lat, pos.lng);
                            this.pushCoords(pos.lat, pos.lng);
                            this.reverseGeocode(pos.lat, pos.lng);
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
                    const center = {
                        lat,
                        lng
                    };

                    if (this.circle) {
                        this.circle.setCenter(center);
                        this.circle.setRadius(r);
                    } else {
                        this.circle = new google.maps.Circle({
                            strokeColor: "#ef4444",
                            strokeOpacity: 0.8,
                            strokeWeight: 2,
                            fillColor: "#ef4444",
                            fillOpacity: 0.15,
                            map: this.map,
                            center: center,
                            radius: r,
                            clickable: false
                        });
                    }
                },

                currentRadius() {
                    const el = document.getElementById('data.radius_meters');
                    return el ? (parseInt(el.value) || initRadius) : initRadius;
                },

                pushCoords(lat, lng) {
                    this.$wire.$set('data.latitude', parseFloat(lat).toFixed(7));
                    this.$wire.$set('data.longitude', parseFloat(lng).toFixed(7));
                },

                async reverseGeocode(lat, lng) {
                    const addrEl = document.getElementById('stop-address-input');
                    // Solo autocompletar dirección si está vacía
                    if (addrEl && (addrEl.value || '').trim() !== '') return;

                    this.geocoding = true;
                    try {
                        const response = await this.geocoder.geocode({
                            location: {
                                lat,
                                lng
                            }
                        });
                        if (response.results[0]) {
                            const address = response.results[0].formatted_address;
                            this.$wire.$set('data.address', address);
                        }
                    } catch (e) {
                        console.error("Geocode failed: " + e);
                    } finally {
                        this.geocoding = false;
                    }
                }
            };
        }
    </script>
</div>
