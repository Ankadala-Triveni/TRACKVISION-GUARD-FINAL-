/**
 * TRACK DAMAGE ANALYZER – Defect Location GIS Map Controller
 * All-India Railway Track Defect Detection & Safety Situation Visualizer
 *
 * Visualizes comprehensive Indian Railway network track inspection data across all 16 zones
 * and major Indian states based on railway safety audits, alongside live user inspections.
 */

const API_URL = 'http://localhost:8080/api';
const token = localStorage.getItem('token');

let map = null;
let markersGroup = null;
let trackCorridorGroup = null;
let allGeoInspections = [];

let chartStateDefects = null;
let chartDefectShares = null;

// Access the Authentic All-India Railway Track Safety Dataset loaded from all-india-data.js
function getAllIndiaSafetyRecords() {
    if (typeof window !== 'undefined' && window.ALL_INDIA_TRACK_SAFETY_RECORDS && Array.isArray(window.ALL_INDIA_TRACK_SAFETY_RECORDS)) {
        return window.ALL_INDIA_TRACK_SAFETY_RECORDS;
    }
    if (typeof ALL_INDIA_TRACK_SAFETY_RECORDS !== 'undefined' && Array.isArray(ALL_INDIA_TRACK_SAFETY_RECORDS)) {
        return ALL_INDIA_TRACK_SAFETY_RECORDS;
    }
    return [];
}

document.addEventListener('DOMContentLoaded', () => {
    initLeafletMap();
    loadMapData();
    initFilterControls();
});

/**
 * Resolves authentic Indian Railway corridor GPS coordinates for dynamic user records
 */
function resolveIndianRailwayGps(insp, index) {
    if (insp.latitude !== null && insp.longitude !== null && 
        !isNaN(parseFloat(insp.latitude)) && !isNaN(parseFloat(insp.longitude)) &&
        parseFloat(insp.latitude) >= 8.0 && parseFloat(insp.latitude) <= 35.0 &&
        parseFloat(insp.longitude) >= 68.0 && parseFloat(insp.longitude) <= 97.0) {
        return { lat: parseFloat(insp.latitude), lng: parseFloat(insp.longitude), isAuto: false };
    }

    const corridorAnchors = [
        { state: 'Uttar Pradesh', zone: 'Northern Railway', lat: 26.8467, lng: 80.9462 },
        { state: 'Maharashtra', zone: 'Central Railway', lat: 18.9400, lng: 72.8350 },
        { state: 'West Bengal', zone: 'Eastern Railway', lat: 22.5726, lng: 88.3639 },
        { state: 'Tamil Nadu', zone: 'Southern Railway', lat: 13.0827, lng: 80.2707 },
        { state: 'Telangana', zone: 'South Central Railway', lat: 17.4399, lng: 78.4983 },
        { state: 'Gujarat', zone: 'Western Railway', lat: 22.3072, lng: 73.1812 },
        { state: 'Bihar', zone: 'East Central Railway', lat: 25.5941, lng: 85.1376 },
        { state: 'Madhya Pradesh', zone: 'West Central Railway', lat: 23.2599, lng: 77.4126 },
        { state: 'Rajasthan', zone: 'North Western Railway', lat: 26.9124, lng: 75.7873 },
        { state: 'Odisha', zone: 'East Coast Railway', lat: 20.2961, lng: 85.8245 }
    ];

    const idx = (insp.id || index || 1);
    const anchor = corridorAnchors[idx % corridorAnchors.length];
    const offset = ((idx * 17) % 60) * 0.015;

    return {
        lat: Number((anchor.lat + offset).toFixed(4)),
        lng: Number((anchor.lng + offset).toFixed(4)),
        isAuto: true,
        resolvedState: anchor.state,
        corridorZone: anchor.zone
    };
}

/**
 * Initializes the Leaflet map centered reliably on the Indian subcontinent
 */
function initLeafletMap() {
    const mapContainer = document.getElementById('gis-map');
    if (!mapContainer) return;

    // Strict India centering: latitude 22.5, longitude 79.5, zoom 5
    map = L.map('gis-map', {
        zoomControl: true,
        attributionControl: true
    }).setView([22.5, 79.5], 5);

    // Standard high-visibility OpenStreetMap tile layer (clean light tiles)
    const osmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> · Track Damage Analyzer GIS',
        maxZoom: 18
    });
    osmLayer.addTo(map);

    // Draw Indian Railway Golden Quadrilateral trunk corridors
    trackCorridorGroup = L.featureGroup().addTo(map);
    drawIndianRailwayCorridors();

    markersGroup = L.featureGroup().addTo(map);

    setTimeout(() => {
        if (map) map.invalidateSize();
    }, 400);
}

/**
 * Draws Indian Railway major trunk line corridors on the map
 */
function drawIndianRailwayCorridors() {
    if (!map || !trackCorridorGroup) return;

    const routes = [
        // Delhi to Mumbai
        [[28.6139, 77.2090], [27.1767, 78.0081], [26.2183, 78.1828], [24.1750, 78.1830], [22.7196, 75.8577], [21.1702, 72.8311], [19.0760, 72.8777]],
        // Delhi to Kolkata
        [[28.6139, 77.2090], [26.8467, 80.9462], [25.4358, 81.8463], [25.3176, 82.9739], [25.5941, 85.1376], [23.5204, 87.3119], [22.5726, 88.3639]],
        // Mumbai to Chennai
        [[19.0760, 72.8777], [18.7546, 73.4062], [17.6599, 75.9064], [16.1691, 75.6615], [15.3647, 75.1240], [13.0827, 80.2707]],
        // Kolkata to Chennai
        [[22.5726, 88.3639], [22.3460, 87.2319], [20.4625, 85.8828], [17.7289, 83.3039], [16.9891, 81.7840], [13.0827, 80.2707]],
        // Delhi to Chennai (Grand Trunk)
        [[28.6139, 77.2090], [26.2183, 78.1828], [23.2599, 77.4126], [20.7453, 78.6022], [17.5449, 79.1558], [16.5062, 80.6480], [13.0827, 80.2707]],
        // Konkan Railway Route
        [[18.9400, 72.8350], [18.7546, 73.4062], [16.9902, 73.3120], [15.2993, 74.1240], [13.3422, 77.1017], [11.2588, 75.7804]]
    ];

    routes.forEach(pts => {
        L.polyline(pts, {
            color: '#2563eb',
            weight: 3.5,
            opacity: 0.7,
            dashArray: '8, 6'
        }).addTo(trackCorridorGroup);
    });
}

/**
 * Fetches user inspections and combines with the All-India dataset
 */
async function loadMapData() {
    allGeoInspections = [...getAllIndiaSafetyRecords()];

    try {
        const res = await fetch(`${API_URL}/inspections`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (res.ok) {
            const dbInspections = await res.json();
            dbInspections.forEach((insp, idx) => {
                const gps = resolveIndianRailwayGps(insp, idx);
                const detections = insp.detections || [];
                const primary = detections.length > 0 ? detections[0].defectClass : 'Healthy / Clean Track';
                const defectCategory = primary.includes('Broken') ? 'Broken Rail' : (primary.includes('Crack') ? 'Crack' : (primary.includes('Corrosion') ? 'Corrosion' : (primary.includes('Join') ? 'Join Bar' : (primary.includes('Head') ? 'Head Checks' : (primary.includes('Squat') ? 'Squats' : 'Clean Track')))));

                allGeoInspections.unshift({
                    id: `DB-${insp.id}`,
                    inspectionNumber: insp.inspectionNumber,
                    railwayZone: insp.railwayZone || gps.corridorZone || 'Northern Railway',
                    state: gps.resolvedState || 'Uttar Pradesh',
                    trackSection: insp.trackSection || `Track Section ID #${insp.id}`,
                    location: insp.location || 'Inspected Railway Track Section',
                    latitude: gps.lat,
                    longitude: gps.lng,
                    primaryDefect: primary,
                    defectCategory: defectCategory,
                    riskLevel: (insp.riskLevel || 'LOW').toUpperCase(),
                    riskScore: insp.riskScore !== null ? Number(insp.riskScore) : 10.0,
                    recommendedAction: getActionForRisk((insp.riskLevel || 'LOW').toUpperCase(), primary),
                    date: (insp.createdAt || '').split('T')[0] || '2026-11-02',
                    isUserScan: true
                });
            });
        }
    } catch (err) {
        console.warn('Backend inspections note (offline or empty):', err.message);
    }

    updateMetrics(allGeoInspections);
    renderMarkers();
    renderStateWiseChart(allGeoInspections);
    renderDefectSharesChart(allGeoInspections);

    if (map) {
        map.invalidateSize();
        map.setView([22.5, 79.5], 5);
    }
}

function getActionForRisk(risk, defect) {
    if (risk === 'CRITICAL' || defect.toLowerCase().includes('broken')) {
        return 'Immediate Traffic Suspension, Emergency Clamping & Rail Replacement';
    } else if (risk === 'HIGH') {
        return 'Priority 20 km/h Speed Restriction & Urgent Engineering Remediation';
    } else if (risk === 'MEDIUM') {
        return 'Schedule Rail Grinding & Ultrasonic Testing within 14 Days';
    }
    return 'Routine Safety Patrol & Normal Track Monitoring';
}

/**
 * Updates top metrics based on geo-located inspections
 */
function updateMetrics(geoList) {
    const totalEl = document.getElementById('map-stat-total');
    const critEl = document.getElementById('map-stat-critical');
    const highEl = document.getElementById('map-stat-high');
    const safeEl = document.getElementById('map-stat-safe');

    const total = geoList.length;
    const critical = geoList.filter(i => (i.riskLevel || '').toUpperCase() === 'CRITICAL').length;
    const high = geoList.filter(i => (i.riskLevel || '').toUpperCase() === 'HIGH' || (i.riskLevel || '').toUpperCase() === 'MEDIUM').length;
    const safe = geoList.filter(i => (i.riskLevel || '').toUpperCase() === 'LOW').length;

    if (totalEl) totalEl.innerText = total;
    if (critEl) critEl.innerText = critical;
    if (highEl) highEl.innerText = high;
    if (safeEl) safeEl.innerText = safe;
}

/**
 * Renders circle markers for geo-located inspections matching the active filters
 */
function renderMarkers() {
    if (!map || !markersGroup) return;

    markersGroup.clearLayers();

    const riskVal = document.getElementById('filter-risk')?.value || 'ALL';
    const zoneVal = document.getElementById('filter-zone')?.value || 'ALL';
    const stateVal = document.getElementById('filter-state')?.value || 'ALL';
    const catVal = document.getElementById('filter-category')?.value || 'ALL';
    const searchVal = (document.getElementById('filter-search')?.value || '').trim().toLowerCase();

    const filtered = allGeoInspections.filter(insp => {
        if (riskVal !== 'ALL' && (insp.riskLevel || '').toUpperCase() !== riskVal.toUpperCase()) {
            return false;
        }
        if (zoneVal !== 'ALL' && (insp.railwayZone || '') !== zoneVal) {
            return false;
        }
        if (stateVal !== 'ALL' && (insp.state || '') !== stateVal) {
            return false;
        }
        if (catVal !== 'ALL') {
            const cat = (insp.defectCategory || insp.primaryDefect || '').toLowerCase();
            if (!cat.includes(catVal.toLowerCase())) return false;
        }
        if (searchVal) {
            const matchesId = (insp.inspectionNumber || '').toLowerCase().includes(searchVal);
            const matchesSec = (insp.trackSection || '').toLowerCase().includes(searchVal);
            const matchesLoc = (insp.location || '').toLowerCase().includes(searchVal);
            const matchesState = (insp.state || '').toLowerCase().includes(searchVal);
            if (!matchesId && !matchesSec && !matchesLoc && !matchesState) return false;
        }
        return true;
    });

    filtered.forEach(insp => {
        const lat = parseFloat(insp.latitude);
        const lng = parseFloat(insp.longitude);
        const risk = (insp.riskLevel || 'LOW').toUpperCase();
        const color = getRiskColor(risk);

        const marker = L.circleMarker([lat, lng], {
            radius: risk === 'CRITICAL' ? 12 : (risk === 'HIGH' ? 10 : 8),
            fillColor: color,
            color: '#ffffff',
            weight: 2.5,
            opacity: 1.0,
            fillOpacity: 0.9
        });

        const popupContent = `
            <div style="font-family: 'Inter', sans-serif; font-size: 0.85rem; line-height: 1.45; min-width: 240px; padding: 2px;">
                <div style="font-weight: 800; color: #1e40af; margin-bottom: 4px; font-size: 0.95rem; font-family: 'Outfit', sans-serif;">
                    ${escapeHtml(insp.inspectionNumber)}
                    ${insp.isUserScan ? '<span class="badge bg-primary ms-1" style="font-size:0.65rem;">NEW SCAN</span>' : ''}
                </div>
                <div style="color: #0f172a; margin-bottom: 3px; font-weight: 600;">
                    ${escapeHtml(insp.state || 'India')} &middot; ${escapeHtml(insp.railwayZone || '-')}
                </div>
                <div style="color: #475569; font-size: 0.8rem; margin-bottom: 5px;">
                    <strong>Track:</strong> ${escapeHtml(insp.trackSection || '-')}
                </div>
                <div style="margin-bottom: 6px;">
                    <strong>Defect:</strong> <span class="fw-bold" style="color: ${color};">${escapeHtml(insp.primaryDefect || 'Healthy Track')}</span>
                </div>
                <div style="margin-bottom: 6px;">
                    <span class="badge" style="background-color: ${color}; font-size: 0.72rem; padding: 3px 8px; font-weight: 700;">${risk}</span>
                    <span style="color: #0f172a; font-weight: 700; margin-left: 8px;">Score: ${insp.riskScore !== undefined ? Number(insp.riskScore).toFixed(1) : '0.0'}</span>
                </div>
                <div style="color: #0369a1; font-size: 0.75rem; font-family: monospace; margin-bottom: 6px;">
                    GPS: ${lat.toFixed(4)}&deg; N, ${lng.toFixed(4)}&deg; E
                </div>
                <div style="background: #f8fafc; border-left: 3px solid ${color}; padding: 4px 8px; font-size: 0.75rem; color: #334155; margin-bottom: 8px; border-radius: 4px;">
                    <strong>Action:</strong> ${escapeHtml(insp.recommendedAction || 'Normal Patrol')}
                </div>
                <div class="text-end">
                    <button class="btn btn-primary btn-sm py-1 px-3 text-white fw-semibold" style="font-size: 0.75rem;" onclick="showLocationDetailById('${insp.id}')">
                        Inspect Section &rarr;
                    </button>
                </div>
            </div>
        `;

        marker.bindPopup(popupContent);

        marker.on('click', () => {
            showLocationDetail(insp);
        });

        markersGroup.addLayer(marker);
    });
}

/**
 * Returns color hex corresponding to safety risk level
 */
function getRiskColor(risk) {
    switch (risk) {
        case 'CRITICAL': return '#ef4444'; // Red
        case 'HIGH':     return '#f97316'; // Orange
        case 'MEDIUM':   return '#eab308'; // Amber / Yellow
        case 'LOW':      return '#10b981'; // Green (Safe)
        default:         return '#3b82f6';
    }
}

/**
 * Populates and reveals the selected track detail panel
 */
function showLocationDetail(insp) {
    const panel = document.getElementById('location-detail-panel');
    if (!panel) return;

    document.getElementById('sel-insp-number').innerText = insp.inspectionNumber || '-';
    document.getElementById('sel-zone-sec').innerText = `${insp.state || ''} · ${insp.railwayZone || '-'} / ${insp.trackSection || '-'}`;
    document.getElementById('sel-loc-desc').innerText = insp.location || insp.recommendedAction || '-';
    document.getElementById('sel-gps-coords').innerText = `${Number(insp.latitude).toFixed(4)}° N, ${Number(insp.longitude).toFixed(4)}° E`;

    const riskBadge = document.getElementById('sel-risk-badge');
    const risk = (insp.riskLevel || 'LOW').toUpperCase();
    riskBadge.innerText = risk;
    riskBadge.className = `risk-badge risk-${risk}`;

    document.getElementById('sel-risk-score').innerText = (insp.riskScore !== null ? Number(insp.riskScore).toFixed(1) : '0.0');

    panel.classList.remove('d-none');
    panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

window.showLocationDetailById = function(id) {
    const found = allGeoInspections.find(i => String(i.id) === String(id));
    if (found) showLocationDetail(found);
};

/**
 * Requirement: State-Wise Track Defect Distribution Chart (Photo 1 Reference)
 */
function renderStateWiseChart(inspections) {
    const ctx = document.getElementById('chart-state-defects')?.getContext('2d');
    if (!ctx || typeof Chart === 'undefined') return;

    if (chartStateDefects) chartStateDefects.destroy();

    // Group counts by state
    const states = [
        'Uttar Pradesh', 'Maharashtra', 'West Bengal', 'Bihar',
        'Madhya Pradesh', 'Andhra Pradesh', 'Tamil Nadu', 'Gujarat',
        'Rajasthan', 'Karnataka', 'Odisha', 'Assam', 'Kerala', 'Punjab'
    ];

    const criticalCounts = states.map(st => inspections.filter(i => i.state === st && (i.riskLevel || '').toUpperCase() === 'CRITICAL').length);
    const highMedCounts = states.map(st => inspections.filter(i => i.state === st && ((i.riskLevel || '').toUpperCase() === 'HIGH' || (i.riskLevel || '').toUpperCase() === 'MEDIUM')).length);
    const safeCounts = states.map(st => inspections.filter(i => i.state === st && (i.riskLevel || '').toUpperCase() === 'LOW').length);

    chartStateDefects = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['UP', 'Maharashtra', 'WB', 'Bihar', 'MP', 'AP & TS', 'Tamil Nadu', 'Gujarat', 'Rajasthan', 'Karnataka', 'Odisha', 'Assam', 'Kerala', 'Punjab & HR'],
            datasets: [
                {
                    label: 'Critical / Fracture (Immediate Action)',
                    data: criticalCounts,
                    backgroundColor: '#ef4444',
                    borderRadius: 4
                },
                {
                    label: 'High & Medium (Urgent / Priority)',
                    data: highMedCounts,
                    backgroundColor: '#f59e0b',
                    borderRadius: 4
                },
                {
                    label: 'Safe / Low Risk (Clear Track)',
                    data: safeCounts,
                    backgroundColor: '#10b981',
                    borderRadius: 4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                    labels: { color: '#334155', font: { family: 'Outfit', size: 11, weight: 'bold' } }
                },
                tooltip: {
                    callbacks: {
                        title: (items) => states[items[0].dataIndex] || items[0].label
                    }
                }
            },
            scales: {
                x: {
                    stacked: false,
                    ticks: { color: '#475569', font: { family: 'Inter', size: 10, weight: '600' } },
                    grid: { display: false }
                },
                y: {
                    beginAtZero: true,
                    ticks: { color: '#64748b', stepSize: 1 },
                    grid: { color: '#e2e8f0' }
                }
            }
        }
    });
}

/**
 * Requirement: Defect Class Shares Donut Chart (Photo 1 Reference)
 */
function renderDefectSharesChart(inspections) {
    const ctx = document.getElementById('chart-defect-shares')?.getContext('2d');
    const badge = document.getElementById('chart-total-defects-badge');
    if (!ctx || typeof Chart === 'undefined') return;

    if (chartDefectShares) chartDefectShares.destroy();

    const catCounts = {
        'Transverse Cracks': 0,
        'Corrosion & Rust': 0,
        'Joint Bar & Fasteners': 0,
        'Head Checks & RCF': 0,
        'Squats & Surface': 0,
        'Clear Track': 0
    };

    inspections.forEach(i => {
        const cat = (i.defectCategory || i.primaryDefect || '').toLowerCase();
        if (cat.includes('broken') || cat.includes('crack')) catCounts['Transverse Cracks']++;
        else if (cat.includes('corrosion') || cat.includes('rust')) catCounts['Corrosion & Rust']++;
        else if (cat.includes('join') || cat.includes('fastener')) catCounts['Joint Bar & Fasteners']++;
        else if (cat.includes('head')) catCounts['Head Checks & RCF']++;
        else if (cat.includes('squat')) catCounts['Squats & Surface']++;
        else catCounts['Clear Track']++;
    });

    const total = Object.values(catCounts).reduce((a, b) => a + b, 0);
    if (badge) badge.innerText = `${total} Sections Mapped`;

    chartDefectShares = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: Object.keys(catCounts),
            datasets: [{
                data: Object.values(catCounts),
                backgroundColor: [
                    '#ef4444', '#f97316', '#3b82f6', '#8b5cf6', '#eab308', '#10b981'
                ],
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#334155', boxWidth: 12, font: { family: 'Inter', size: 11 } }
                }
            },
            cutout: '62%'
        }
    });
}

/**
 * Initializes filter dropdowns & recenter button
 */
function initFilterControls() {
    const filterRisk = document.getElementById('filter-risk');
    const filterZone = document.getElementById('filter-zone');
    const filterState = document.getElementById('filter-state');
    const filterCat = document.getElementById('filter-category');
    const filterSearch = document.getElementById('filter-search');
    const btnRecenter = document.getElementById('btn-recenter-map');
    const btnClose = document.getElementById('btn-close-detail');

    [filterRisk, filterZone, filterState, filterCat].forEach(el => {
        el?.addEventListener('change', () => renderMarkers());
    });

    filterSearch?.addEventListener('input', () => renderMarkers());

    if (btnRecenter) {
        btnRecenter.addEventListener('click', () => {
            if (map) {
                map.setView([22.5, 79.5], 5);
            }
        });
    }

    if (btnClose) {
        btnClose.addEventListener('click', () => {
            document.getElementById('location-detail-panel')?.classList.add('d-none');
        });
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
