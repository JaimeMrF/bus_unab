{{--
    Carga Leaflet una sola vez y expone window.loadLeaflet() → Promise<L>.
    Los mapas del panel usan Leaflet + OpenStreetMap: no requieren API key
    (antes eran Google Maps y sin GOOGLE_MAPS_API_KEY quedaban en blanco).
--}}
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<style>
    .leaflet-container { z-index: 0; }
    .leaflet-pane, .leaflet-top, .leaflet-bottom { z-index: 1 !important; }
</style>
<script>
    window.loadLeaflet ??= () => window.__leafletPromise ??= new Promise((resolve, reject) => {
        if (window.L) return resolve(window.L);
        const s = document.createElement('script');
        s.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        s.onload = () => resolve(window.L);
        s.onerror = reject;
        document.head.appendChild(s);
    });

    // Capa base OSM y pin circular de color; compartidos por los pickers y el editor de rutas.
    window.osmLayer ??= (L) => L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap',
    });
    window.pinIcon ??= (L, color, size = 18, label = '') => L.divIcon({
        className: '',
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
        html: `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 1px 3px rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;color:#fff;font:600 ${Math.round(size * 0.55)}px/1 sans-serif">${label}<\/div>`,
    });

    // Geocodificación con Nominatim (OSM), limitada a Colombia.
    // Nominatim devuelve la jerarquía completa (comuna, área metropolitana, país...): basta con lo primero.
    window.shortAddress ??= (s) => s.split(', ').slice(0, 3).join(', ');
    window.geoSearch ??= async (q) => {
        const r = await fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=co&accept-language=es&q=' + encodeURIComponent(q));
        const [hit] = await r.json();
        return hit ? { lat: parseFloat(hit.lat), lng: parseFloat(hit.lon), name: hit.name, address: window.shortAddress(hit.display_name) } : null;
    };
    window.geoReverse ??= async (lat, lng) => {
        const r = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&accept-language=es&lat=${lat}&lon=${lng}`);
        const j = await r.json();
        return j?.display_name ? window.shortAddress(j.display_name) : null;
    };
</script>
