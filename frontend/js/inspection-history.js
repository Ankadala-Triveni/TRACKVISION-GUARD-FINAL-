/**
 * RailGuard AI – Inspection History Controller
 * Loads authentic historical inspection records and renders visual reports on HTML5 Canvas.
 */

const API_URL = 'http://localhost:8080/api';
const token = localStorage.getItem('token');

let allInspections = [];

document.addEventListener('DOMContentLoaded', () => {
    loadInspectionHistory();
    initSearchAndFilters();
});

/**
 * Fetches all inspection records from MySQL backend
 */
async function loadInspectionHistory() {
    const tbody = document.getElementById('history-tbody');
    const badge = document.getElementById('history-count-badge');

    if (!token) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5"><i class="bi bi-shield-lock text-primary display-6 d-block mb-2"></i><div class="fw-bold text-dark mb-1">Authentication Required</div><div class="small text-muted mb-3">Please sign in to view official railway track inspection records.</div><a href="login.html" class="btn btn-primary btn-sm px-3">Sign In to Continue</a></td></tr>';
        badge.innerText = '0 records';
        return;
    }

    try {
        const res = await fetch(`${API_URL}/inspections`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (res.status === 401 || res.status === 403) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5"><i class="bi bi-shield-lock text-warning display-6 d-block mb-2"></i><div class="fw-bold text-dark mb-1">Session Expired</div><div class="small text-muted mb-3">Your credentials have expired. Please re-authenticate.</div><a href="login.html" class="btn btn-primary btn-sm px-3">Sign In Again</a></td></tr>';
            badge.innerText = '0 records';
            return;
        }

        if (!res.ok) throw new Error('Failed to load inspection history (Status ' + res.status + ').');

        allInspections = await res.json();
        // Deduplicate records by unique id
        const seen = new Set();
        allInspections = (Array.isArray(allInspections) ? allInspections : []).filter(item => {
            if (seen.has(item.id)) return false;
            seen.add(item.id);
            return true;
        });
        renderTable(allInspections);

    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4"><i class="bi bi-exclamation-triangle me-2"></i>${escapeHtml(err.message)}</td></tr>`;
        badge.innerText = '0 records';
    }
}

/**
 * Filters and renders inspection rows into the table
 */
function renderTable(inspections) {
    const tbody = document.getElementById('history-tbody');
    const badge = document.getElementById('history-count-badge');

    badge.innerText = `${inspections.length} record${inspections.length === 1 ? '' : 's'}`;

    if (inspections.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-5"><i class="bi bi-inbox display-6 d-block mb-2"></i>No historical inspections matching criteria.</td></tr>`;
        return;
    }

    tbody.innerHTML = inspections.map(insp => {
        const defectCount = (insp.detections || []).length;
        const risk = (insp.riskLevel || 'LOW').toUpperCase();
        const score = (insp.riskScore !== null ? Number(insp.riskScore).toFixed(1) : '0.0');
        const dateStr = formatDate(insp.createdAt);
        const hasGps = insp.latitude !== null && insp.longitude !== null;

        return `
            <tr>
                <td><strong class="text-dark font-outfit fw-bold">${escapeHtml(insp.inspectionNumber)}</strong></td>
                <td class="text-muted">${dateStr}</td>
                <td>
                    <div class="text-dark fw-semibold small">${escapeHtml(insp.railwayZone || '-')}</div>
                    <div class="text-muted small">${escapeHtml(insp.trackSection || '-')}</div>
                </td>
                <td>
                    <div class="text-dark small text-truncate" style="max-width: 200px;">${escapeHtml(insp.location || '-')}</div>
                    ${hasGps ? `<span class="badge bg-dark border border-secondary text-info font-monospace" style="font-size: 0.68rem;">GPS ${Number(insp.latitude).toFixed(3)}, ${Number(insp.longitude).toFixed(3)}</span>` : ''}
                </td>
                <td>
                    <span class="badge ${defectCount > 0 ? 'bg-danger' : 'bg-success'} font-outfit px-2 py-1">
                        ${defectCount} defect${defectCount === 1 ? '' : 's'}
                    </span>
                </td>
                <td><span class="fw-bold font-outfit text-dark">${score}</span></td>
                <td><span class="risk-badge risk-${risk}">${risk}</span></td>
                <td><span class="status-badge status-${insp.status || 'UNKNOWN'}">${insp.status || 'UNKNOWN'}</span></td>
                <td class="text-end">
                    <button class="btn btn-outline-info btn-sm px-2 py-1" onclick="openHistoryModal(${insp.id})" title="View Visual Defect Report">
                        <i class="bi bi-eye-fill me-1"></i> Visual Report
                    </button>
                    <a href="inspection.html?id=${insp.id}" class="btn btn-outline-primary btn-sm px-2 py-1 ms-1" title="Open in Results View">
                        <i class="bi bi-clipboard2-pulse me-1"></i> Results
                    </a>
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * Initializes real-time search & filter dropdowns
 */
function initSearchAndFilters() {
    const searchInput = document.getElementById('search-input');
    const riskSelect = document.getElementById('filter-risk');
    const statusSelect = document.getElementById('filter-status');

    function applyFilters() {
        const query = (searchInput?.value || '').trim().toLowerCase();
        const selectedRisk = (riskSelect?.value || 'ALL').toUpperCase();
        const selectedStatus = (statusSelect?.value || 'ALL').toUpperCase();

        const filtered = allInspections.filter(i => {
            const matchesQuery = !query || 
                (i.inspectionNumber || '').toLowerCase().includes(query) ||
                (i.railwayZone || '').toLowerCase().includes(query) ||
                (i.trackSection || '').toLowerCase().includes(query) ||
                (i.location || '').toLowerCase().includes(query);

            const matchesRisk = (selectedRisk === 'ALL') || ((i.riskLevel || '').toUpperCase() === selectedRisk);
            const matchesStatus = (selectedStatus === 'ALL') || ((i.status || '').toUpperCase() === selectedStatus);

            return matchesQuery && matchesRisk && matchesStatus;
        });

        renderTable(filtered);
    }

    searchInput?.addEventListener('input', applyFilters);
    riskSelect?.addEventListener('change', applyFilters);
    statusSelect?.addEventListener('change', applyFilters);
}

/**
 * Opens modal and renders visual track image with YOLO11 defect bounding boxes
 */
window.openHistoryModal = async function(id) {
    const modalEl = document.getElementById('historyModal');
    const modal = new bootstrap.Modal(modalEl);
    
    const canvas = document.getElementById('modal-history-canvas');
    const canvasLoading = document.getElementById('modal-canvas-loading');
    const tbody = document.getElementById('m-detections-tbody');

    canvas.classList.add('d-none');
    canvasLoading.classList.remove('d-none');
    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Loading defect details...</td></tr>';

    modal.show();

    try {
        const res = await fetch(`${API_URL}/inspections/${id}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!res.ok) throw new Error('Inspection details not found');
        const insp = await res.json();

        document.getElementById('m-insp-num').innerText = insp.inspectionNumber;
        document.getElementById('m-insp-score').innerText = (insp.riskScore !== null ? Number(insp.riskScore).toFixed(1) : '0.0');
        document.getElementById('m-insp-loc').innerText = `${insp.location || '-'} (${insp.railwayZone || '-'} / ${insp.trackSection || '-'})`;
        
        const risk = (insp.riskLevel || 'LOW').toUpperCase();
        const riskEl = document.getElementById('m-insp-risk');
        riskEl.innerText = risk;
        riskEl.className = `risk-badge risk-${risk}`;

        const detections = insp.detections || [];
        if (detections.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-success py-3"><i class="bi bi-shield-check me-2"></i>Clean track verified. No defects localized by YOLO11 V3.</td></tr>';
        } else {
            tbody.innerHTML = detections.map((d, idx) => {
                const bb = d.boundingBox || {};
                const coords = `[${Math.round(bb.xMin || 0)}, ${Math.round(bb.yMin || 0)}, ${Math.round(bb.xMax || 0)}, ${Math.round(bb.yMax || 0)}]`;
                return `
                    <tr>
                        <td class="font-outfit text-muted">${idx + 1}</td>
                        <td class="text-dark fw-bold">${escapeHtml(d.defectClass)}</td>
                        <td><span class="badge bg-secondary font-outfit">${(d.confidence * 100).toFixed(1)}%</span></td>
                        <td><span class="risk-badge risk-${d.severity}">${d.severity}</span></td>
                        <td><code class="text-info">${coords}</code></td>
                    </tr>
                `;
            }).join('');
        }

        // Fetch image blob
        const imgRes = await fetch(`${API_URL}/inspections/${id}/image`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!imgRes.ok) {
            canvasLoading.innerHTML = '<span class="text-warning">No track photo stored for this inspection.</span>';
            return;
        }

        const blob = await imgRes.blob();
        const objUrl = URL.createObjectURL(blob);
        const img = new Image();

        img.onload = () => {
            canvas.width = img.naturalWidth || img.width;
            canvas.height = img.naturalHeight || img.height;

            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

            const baseScale = Math.max(1, canvas.width / 1000);
            const strokeWidth = Math.max(3, Math.round(3 * baseScale));
            const fontSize = Math.max(14, Math.round(16 * baseScale));
            ctx.font = `bold ${fontSize}px "Outfit", sans-serif`;

            detections.forEach(d => {
                const bb = d.boundingBox;
                if (!bb) return;
                const xMin = bb.xMin;
                const yMin = bb.yMin;
                const width = bb.xMax - xMin;
                const height = bb.yMax - yMin;

                const col = getSeverityColor(d.severity);
                ctx.strokeStyle = col.stroke;
                ctx.lineWidth = strokeWidth;
                ctx.fillStyle = col.fill;
                ctx.fillRect(xMin, yMin, width, height);
                ctx.strokeRect(xMin, yMin, width, height);

                const label = `${d.defectClass} ${(d.confidence * 100).toFixed(0)}%`;
                const tw = ctx.measureText(label).width;
                const th = fontSize + 8 * baseScale;
                const ty = Math.max(0, yMin - th);

                ctx.fillStyle = col.stroke;
                ctx.fillRect(xMin, ty, tw + 12 * baseScale, th);
                ctx.fillStyle = '#fff';
                ctx.textBaseline = 'middle';
                ctx.fillText(label, xMin + 6 * baseScale, ty + th / 2);
            });

            canvasLoading.classList.add('d-none');
            canvas.classList.remove('d-none');
        };

        img.onerror = () => {
            canvasLoading.innerHTML = '<span class="text-danger">Failed to decode track image.</span>';
        };

        img.src = objUrl;

    } catch (e) {
        canvasLoading.innerHTML = `<span class="text-danger">Failed to load report: ${escapeHtml(e.message)}</span>`;
    }
};

function getSeverityColor(sev) {
    const s = (sev || '').toUpperCase();
    switch (s) {
        case 'CRITICAL': return { stroke: '#ef4444', fill: 'rgba(239, 68, 68, 0.20)' };
        case 'HIGH':     return { stroke: '#f97316', fill: 'rgba(249, 115, 22, 0.20)' };
        case 'MEDIUM':   return { stroke: '#eab308', fill: 'rgba(234, 179, 8, 0.20)' };
        default:         return { stroke: '#3b82f6', fill: 'rgba(59, 130, 246, 0.20)' };
    }
}

function formatDate(isoStr) {
    if (!isoStr) return '-';
    try {
        const d = new Date(isoStr);
        return d.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return isoStr;
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

