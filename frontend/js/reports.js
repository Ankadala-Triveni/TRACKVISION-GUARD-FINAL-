/**
 * RailGuard AI – Engineering & Compliance Reports Controller
 * Queries authentic records from MySQL and exports CSV reports and printable PDF logs.
 */

const API_URL = 'http://localhost:8080/api';
const token = localStorage.getItem('token');

let cachedInspections = [];
let cachedTasks = [];

document.addEventListener('DOMContentLoaded', () => {
    loadReportsData();
    initExportButtons();
});

/**
 * Loads authentic inspections and maintenance work orders from MySQL backend
 */
async function loadReportsData() {
    try {
        const [inspRes, maintRes] = await Promise.all([
            fetch(`${API_URL}/inspections`, { headers: { 'Authorization': `Bearer ${token}` } }),
            fetch(`${API_URL}/maintenance`, { headers: { 'Authorization': `Bearer ${token}` } })
        ]);

        if (inspRes.ok) cachedInspections = await inspRes.json();
        if (maintRes.ok) cachedTasks = await maintRes.json();

        renderInspectionsPreview(cachedInspections);
        renderMaintenancePreview(cachedTasks);

    } catch (err) {
        console.error('Error loading report data:', err);
    }
}

/**
 * Renders live preview of inspection records
 */
function renderInspectionsPreview(list) {
    const tbody = document.getElementById('preview-inspections-tbody');
    const badge = document.getElementById('prev-insp-count');

    if (badge) badge.innerText = list.length;
    if (!tbody) return;

    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" class="text-center text-muted py-4">No inspection records found in MySQL.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(i => {
        const defects = (i.detections || []).length;
        const risk = (i.riskLevel || 'LOW').toUpperCase();
        const score = i.riskScore !== null ? Number(i.riskScore).toFixed(1) : '0.0';
        const gps = (i.latitude !== null && i.longitude !== null)
            ? `${Number(i.latitude).toFixed(3)}, ${Number(i.longitude).toFixed(3)}`
            : 'None';

        return `
            <tr>
                <td><strong class="text-dark font-outfit">${escapeHtml(i.inspectionNumber)}</strong></td>
                <td class="text-secondary">${formatDate(i.createdAt)}</td>
                <td>${escapeHtml(i.railwayZone || '-')} / ${escapeHtml(i.trackSection || '-')}</td>
                <td><span class="text-truncate d-inline-block text-secondary" style="max-width: 180px;">${escapeHtml(i.location || '-')}</span></td>
                <td><code class="text-primary">${gps}</code></td>
                <td><span class="badge ${defects > 0 ? 'bg-danger' : 'bg-success'}">${defects}</span></td>
                <td><span class="fw-bold font-outfit text-dark">${score}</span></td>
                <td><span class="risk-badge risk-${risk}">${risk}</span></td>
                <td><span class="status-badge status-${i.status}">${i.status}</span></td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-danger font-outfit py-0 px-2" onclick="downloadInspectionPdf(${i.id}, this)">
                        <i class="bi bi-file-earmark-pdf-fill me-1"></i> PDF
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * Renders live preview of maintenance work orders
 */
function renderMaintenancePreview(list) {
    const tbody = document.getElementById('preview-maintenance-tbody');
    const badge = document.getElementById('prev-maint-count');

    if (badge) badge.innerText = list.length;
    if (!tbody) return;

    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No maintenance tasks recorded in MySQL.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(t => {
        const priority = (t.priority || 'MEDIUM').toUpperCase();
        const status = (t.status || 'SCHEDULED').toUpperCase();

        return `
            <tr>
                <td><strong class="text-dark font-outfit">${escapeHtml(t.taskNumber)}</strong></td>
                <td><span class="risk-badge risk-${priority}">${priority}</span></td>
                <td>
                    <div class="text-dark fw-bold">${escapeHtml(t.title)}</div>
                    <div class="text-muted small text-truncate" style="max-width: 250px;">${escapeHtml(t.description || '-')}</div>
                </td>
                <td><span class="badge bg-light border text-primary">${escapeHtml(t.inspectionNumber || `ID #${t.inspectionId}`)}</span></td>
                <td><span class="text-dark small">${escapeHtml(t.assignedTo || 'Unassigned')}</span></td>
                <td class="text-secondary">${t.scheduledDate ? formatDate(t.scheduledDate) : 'Not specified'}</td>
                <td><span class="status-badge status-${status}">${status}</span></td>
            </tr>
        `;
    }).join('');
}

/**
 * Initializes CSV download triggers
 */
function initExportButtons() {
    // 1. Export all inspections
    document.getElementById('btn-export-inspections-csv')?.addEventListener('click', () => {
        exportInspectionsToCsv(cachedInspections, 'railguard_inspections_audit_report.csv');
    });

    // 2. Export all maintenance work orders
    document.getElementById('btn-export-maintenance-csv')?.addEventListener('click', () => {
        exportMaintenanceToCsv(cachedTasks, 'railguard_maintenance_work_orders.csv');
    });

    // 3. Export high-risk anomalies only (CSV)
    document.getElementById('btn-export-highrisk-csv')?.addEventListener('click', () => {
        const highRisk = cachedInspections.filter(i => {
            const r = (i.riskLevel || '').toUpperCase();
            return r === 'CRITICAL' || r === 'HIGH';
        });
        if (highRisk.length === 0) {
            alert('No Critical or High risk inspections found in the current records (all current records are Low or Medium risk). Use "Download CSV" under Track Inspections Audit to export all records.');
            return;
        }
        exportInspectionsToCsv(highRisk, 'TrackDamageAnalyzer_critical_safety_briefing.csv');
    });

    // 4. Export High-Risk Safety Summary (PDF from Backend Endpoint)
    document.getElementById('btn-export-highrisk-pdf')?.addEventListener('click', async () => {
        const btn = document.getElementById('btn-export-highrisk-pdf');
        btn.disabled = true;
        const origHtml = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> PDF';
        try {
            const res = await fetch(`${API_URL}/inspections/reports/high-risk-pdf`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const blob = await res.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'TrackDamageAnalyzer_HighRisk_Safety_Summary.pdf';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            a.remove();
        } catch (err) {
            console.error('High-Risk PDF generation error:', err);
            alert('Failed to generate High-Risk Safety Summary PDF: ' + err.message);
        } finally {
            btn.disabled = false;
            btn.innerHTML = origHtml;
        }
    });
}

/**
 * Converts inspections array to CSV and triggers file download
 */
function exportInspectionsToCsv(list, filename) {
    if (!list || list.length === 0) {
        alert('No inspection records available to export.');
        return;
    }

    const headers = [
        'Inspection Number',
        'Date Created',
        'Railway Zone',
        'Track Section',
        'Location Description',
        'Latitude',
        'Longitude',
        'Defect Count',
        'Risk Score',
        'Risk Level',
        'Status',
        'AI Model Version'
    ];

    const rows = list.map(i => [
        csvCell(i.inspectionNumber),
        csvCell(i.createdAt),
        csvCell(i.railwayZone),
        csvCell(i.trackSection),
        csvCell(i.location),
        i.latitude !== null ? i.latitude : '',
        i.longitude !== null ? i.longitude : '',
        (i.detections || []).length,
        i.riskScore !== null ? Number(i.riskScore).toFixed(2) : '0.00',
        csvCell(i.riskLevel),
        csvCell(i.status),
        csvCell(i.modelVersion || 'YOLO11-RailGuard-v3.0')
    ]);

    downloadCsv([headers, ...rows], filename);
}

/**
 * Converts maintenance tasks array to CSV and triggers file download
 */
function exportMaintenanceToCsv(list, filename) {
    if (!list || list.length === 0) {
        alert('No maintenance work orders available to export.');
        return;
    }

    const headers = [
        'Task Number',
        'Priority',
        'Work Order Title',
        'Description',
        'Linked Inspection Number',
        'Assigned Personnel / Unit',
        'Scheduled Date',
        'Status',
        'Date Created'
    ];

    const rows = list.map(t => [
        csvCell(t.taskNumber),
        csvCell(t.priority),
        csvCell(t.title),
        csvCell(t.description),
        csvCell(t.inspectionNumber || t.inspectionId),
        csvCell(t.assignedTo),
        csvCell(t.scheduledDate),
        csvCell(t.status),
        csvCell(t.createdAt)
    ]);

    downloadCsv([headers, ...rows], filename);
}

/**
 * Escapes cell for CSV format
 */
function csvCell(val) {
    if (val === null || val === undefined) return '""';
    const str = String(val).replace(/"/g, '""');
    return `"${str}"`;
}

/**
 * Triggers browser file download with UTF-8 BOM
 */
function downloadCsv(grid, filename) {
    const csvContent = '\uFEFF' + grid.map(row => row.join(',')).join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

function formatDate(isoStr) {
    if (!isoStr) return '-';
    try {
        const d = new Date(isoStr);
        return d.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    } catch {
        return isoStr;
    }
}

/**
 * Downloads the official PDF inspection report for a specific inspection ID
 */
async function downloadInspectionPdf(inspectionId, btnEl) {
    let originalHtml = '';
    if (btnEl) {
        originalHtml = btnEl.innerHTML;
        btnEl.disabled = true;
        btnEl.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    }
    try {
        const res = await fetch(`${API_URL}/inspections/${inspectionId}/pdf`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!res.ok) {
            throw new Error(`Failed to generate PDF (HTTP ${res.status})`);
        }
        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `TrackDamageAnalyzer_Inspection_${inspectionId}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
    } catch (err) {
        console.error('PDF download error:', err);
        alert('Could not download PDF report: ' + err.message);
    } finally {
        if (btnEl) {
            btnEl.disabled = false;
            btnEl.innerHTML = originalHtml;
        }
    }
}
window.downloadInspectionPdf = downloadInspectionPdf;

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

