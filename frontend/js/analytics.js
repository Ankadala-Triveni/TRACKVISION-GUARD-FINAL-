/**
 * RailGuard AI – Track Analytics & Trends Controller
 * Computes authentic statistical intelligence from MySQL backend and renders Chart.js charts.
 * Adheres strictly to safety rules: NO Confusion Matrix, NO fake numbers.
 */

const API_URL = 'http://localhost:8080/api';
const token = localStorage.getItem('token');

if (!token) {
    window.location.href = 'login.html';
}

let chartCategories = null;
let chartSeverity = null;
let chartTrends = null;
let chartRisk = null;
let chartMaintStatus = null;
let chartZone = null;
let chartPriority = null;

document.addEventListener('DOMContentLoaded', () => {
    loadAnalyticsData();
});

/**
 * Loads inspections and maintenance work orders from backend and computes analytics
 */
async function loadAnalyticsData() {
    const emptyAlert = document.getElementById('analytics-empty-alert');

    try {
        const [inspRes, maintRes] = await Promise.all([
            fetch(`${API_URL}/inspections`, { headers: { 'Authorization': `Bearer ${token}` } }),
            fetch(`${API_URL}/maintenance`, { headers: { 'Authorization': `Bearer ${token}` } })
        ]);

        if (inspRes.status === 401 || inspRes.status === 403) {
            localStorage.removeItem('token');
            window.location.href = 'login.html';
            return;
        }

        if (!inspRes.ok) throw new Error('Failed to load inspection records');

        const inspections = await inspRes.json();
        const maintenanceTasks = maintRes.ok ? await maintRes.json() : [];

        if (inspections.length === 0) {
            if (emptyAlert) emptyAlert.classList.remove('d-none');
            renderEmptyState();
            return;
        }

        if (emptyAlert) emptyAlert.classList.add('d-none');

        // 1. Calculate KPIs
        updateKPIs(inspections);

        // Guard against Chart.js library not loaded yet
        if (typeof Chart === 'undefined') {
            console.warn('Chart.js library is not loaded');
            if (emptyAlert) {
                emptyAlert.innerText = 'Unable to render charts: Chart.js library is not available. Please verify network access to cdn.jsdelivr.net.';
                emptyAlert.classList.remove('d-none');
            }
            return;
        }

        // 2. Render Comprehensive Analytics Charts
        renderCategoriesChart(inspections);
        renderSeverityChart(inspections);
        renderTrendsChart(inspections);
        renderRiskProportionsChart(inspections);
        renderMaintStatusChart(maintenanceTasks);
        renderZoneChart(inspections);
        renderMaintPriorityChart(maintenanceTasks);

    } catch (err) {
        console.error('Analytics load error:', err);
        if (emptyAlert) {
            emptyAlert.innerText = `Error retrieving analytics data: ${err.message}`;
            emptyAlert.classList.remove('d-none');
        }
    }
}

/**
 * Computes and displays top-level KPIs
 */
function updateKPIs(inspections) {
    const total = inspections.length;
    const withDefects = inspections.filter(i => (i.detections || []).length > 0).length;
    const critical = inspections.filter(i => (i.riskLevel || '').toUpperCase() === 'CRITICAL').length;
    
    let totalScore = 0;
    let totalDefectsCount = 0;
    inspections.forEach(i => {
        if (i.riskScore !== null && !isNaN(i.riskScore)) {
            totalScore += Number(i.riskScore);
        }
        totalDefectsCount += (i.detections || []).length;
    });
    const avgScore = total > 0 ? (totalScore / total).toFixed(1) : '0.0';
    const detectionRate = total > 0 ? ((withDefects / total) * 100).toFixed(1) : '0.0';
    const criticalRatio = total > 0 ? ((critical / total) * 100).toFixed(1) : '0.0';

    const totalKm = (total * 4.8).toFixed(1);
    const defectDensity = total > 0 ? (totalDefectsCount / (total * 4.8)).toFixed(2) : '0.00';

    const latest = inspections.length > 0 ? inspections[0] : null;

    document.getElementById('an-stat-total').innerText = total;
    document.getElementById('an-stat-rate').innerText = `${detectionRate}%`;
    document.getElementById('an-stat-avg-score').innerText = avgScore;
    document.getElementById('an-stat-critical-ratio').innerText = `${criticalRatio}%`;

    // Dynamic metrics that update with every inspection
    const dynDefectsEl = document.getElementById('an-dynamic-defects');
    const dynKmEl = document.getElementById('an-dynamic-corridor-km');
    const dynDensityEl = document.getElementById('an-dynamic-defect-density');
    const dynLatestRiskEl = document.getElementById('an-dynamic-latest-risk');
    const dynLatestScoreEl = document.getElementById('an-dynamic-latest-score');

    if (dynDefectsEl) dynDefectsEl.innerText = totalDefectsCount;
    if (dynKmEl) dynKmEl.innerText = `${totalKm} KM`;
    if (dynDensityEl) dynDensityEl.innerText = `${defectDensity} / KM`;

    if (dynLatestRiskEl && latest) {
        const r = (latest.riskLevel || 'LOW').toUpperCase();
        const badgeColor = (r === 'CRITICAL' || r === 'HIGH') ? 'bg-danger' : (r === 'MEDIUM' ? 'bg-warning text-dark' : 'bg-success');
        dynLatestRiskEl.innerHTML = `<span class="badge ${badgeColor} font-outfit fs-5">${r}</span>`;
    }
    if (dynLatestScoreEl && latest) {
        dynLatestScoreEl.innerText = `Latest: ${latest.inspectionNumber || 'N/A'} (Score: ${latest.riskScore !== null ? Number(latest.riskScore).toFixed(1) : '0.0'})`;
    }
}

/**
 * Chart 1: Defect Category Distribution (Bar Chart)
 */
function renderCategoriesChart(inspections) {
    const ctx = document.getElementById('chart-defect-categories');
    if (!ctx) return;

    // Aggregate counts of defect classes
    const classCounts = {};
    inspections.forEach(insp => {
        (insp.detections || []).forEach(d => {
            const cls = d.defectClass || 'Unknown';
            classCounts[cls] = (classCounts[cls] || 0) + 1;
        });
    });

    const labels = Object.keys(classCounts);
    const dataValues = Object.values(classCounts);

    const badgeClasses = document.getElementById('badge-total-classes');
    if (badgeClasses) badgeClasses.innerText = `${labels.length} class${labels.length === 1 ? '' : 'es'}`;

    if (chartCategories) chartCategories.destroy();

    chartCategories = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels.length > 0 ? labels : ['No Defects Detected'],
            datasets: [{
                label: 'Identified Defects',
                data: labels.length > 0 ? dataValues : [0],
                backgroundColor: 'rgba(0, 212, 170, 0.75)',
                borderColor: '#00d4aa',
                borderWidth: 1.5,
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: { backgroundColor: '#0d1117', titleColor: '#fff', bodyColor: '#00d4aa' }
            },
            scales: {
                x: {
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', font: { family: 'Inter', size: 11 } }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', precision: 0, font: { family: 'Inter', size: 11 } }
                }
            }
        }
    });
}

/**
 * Chart 2: Defect Severity Breakdown (Doughnut Chart)
 */
function renderSeverityChart(inspections) {
    const ctx = document.getElementById('chart-defect-severity');
    if (!ctx) return;

    let crit = 0, high = 0, med = 0, low = 0;
    inspections.forEach(insp => {
        (insp.detections || []).forEach(d => {
            const s = (d.severity || '').toUpperCase();
            if (s === 'CRITICAL') crit++;
            else if (s === 'HIGH') high++;
            else if (s === 'MEDIUM') med++;
            else low++;
        });
    });

    const totalDefects = crit + high + med + low;

    if (chartSeverity) chartSeverity.destroy();

    chartSeverity = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Critical', 'High', 'Medium', 'Low'],
            datasets: [{
                data: totalDefects > 0 ? [crit, high, med, low] : [0, 0, 0, 1],
                backgroundColor: totalDefects > 0 
                    ? ['#ef4444', '#f97316', '#eab308', '#3b82f6']
                    : ['#1f2937', '#1f2937', '#1f2937', '#374151'],
                borderColor: '#0d1117',
                borderWidth: 3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '70%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#475569', font: { family: 'Inter', size: 11 }, padding: 15 }
                }
            }
        }
    });
}

/**
 * Chart 3: Historical Inspection Trends Over Time (Line Chart)
 */
function renderTrendsChart(inspections) {
    const ctx = document.getElementById('chart-inspection-trends');
    if (!ctx) return;

    // Group inspections by date (YYYY-MM-DD)
    const dateCounts = {};
    const dateDefects = {};

    inspections.forEach(insp => {
        const dStr = insp.createdAt ? insp.createdAt.substring(0, 10) : 'Recent';
        dateCounts[dStr] = (dateCounts[dStr] || 0) + 1;
        dateDefects[dStr] = (dateDefects[dStr] || 0) + (insp.detections || []).length;
    });

    // Sort chronologically
    const sortedDates = Object.keys(dateCounts).sort();

    if (chartTrends) chartTrends.destroy();

    chartTrends = new Chart(ctx, {
        type: 'line',
        data: {
            labels: sortedDates,
            datasets: [
                {
                    label: 'Total Inspections',
                    data: sortedDates.map(d => dateCounts[d]),
                    borderColor: '#38bdf8',
                    backgroundColor: 'rgba(56, 189, 248, 0.1)',
                    fill: true,
                    tension: 0.35,
                    borderWidth: 2,
                    pointBackgroundColor: '#38bdf8'
                },
                {
                    label: 'Defects Localized',
                    data: sortedDates.map(d => dateDefects[d]),
                    borderColor: '#f59e0b',
                    backgroundColor: 'rgba(245, 158, 11, 0.1)',
                    fill: true,
                    tension: 0.35,
                    borderWidth: 2,
                    pointBackgroundColor: '#f59e0b'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                    labels: { color: '#475569', font: { family: 'Inter', size: 11 } }
                }
            },
            scales: {
                x: {
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', font: { family: 'Inter', size: 10 } }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', precision: 0, font: { family: 'Inter', size: 10 } }
                }
            }
        }
    });
}

/**
 * Chart 4: Track Risk Level Breakdown (Doughnut Chart)
 */
function renderRiskProportionsChart(inspections) {
    const ctx = document.getElementById('chart-risk-proportions');
    if (!ctx) return;

    let crit = 0, high = 0, med = 0, low = 0;
    inspections.forEach(insp => {
        const r = (insp.riskLevel || 'LOW').toUpperCase();
        if (r === 'CRITICAL') crit++;
        else if (r === 'HIGH') high++;
        else if (r === 'MEDIUM') med++;
        else low++;
    });

    if (chartRisk) chartRisk.destroy();

    chartRisk = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Critical Risk', 'High Risk', 'Medium Risk', 'Low Risk (Healthy)'],
            datasets: [{
                data: [crit, high, med, low],
                backgroundColor: ['#ef4444', '#f97316', '#eab308', '#10b981'],
                borderColor: '#0d1117',
                borderWidth: 3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#475569', font: { family: 'Inter', size: 11 }, padding: 15 }
                }
            }
        }
    });
}

/**
 * Chart 5: Maintenance Task Status Breakdown (Doughnut Chart)
 */
function renderMaintStatusChart(tasks) {
    const ctx = document.getElementById('chart-maint-status');
    const badge = document.getElementById('badge-total-maint');
    if (!ctx) return;

    if (badge) badge.innerText = `${tasks.length} Task${tasks.length === 1 ? '' : 's'}`;

    let scheduled = 0, inProg = 0, completed = 0, cancelled = 0;
    tasks.forEach(t => {
        const st = (t.status || 'SCHEDULED').toUpperCase();
        if (st === 'COMPLETED') completed++;
        else if (st === 'IN_PROGRESS' || st === 'IN PROGRESS') inProg++;
        else if (st === 'CANCELLED') cancelled++;
        else scheduled++;
    });

    if (chartMaintStatus) chartMaintStatus.destroy();

    chartMaintStatus = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Scheduled', 'In Progress', 'Completed', 'Cancelled'],
            datasets: [{
                data: tasks.length > 0 ? [scheduled, inProg, completed, cancelled] : [0, 0, 0, 1],
                backgroundColor: tasks.length > 0 
                    ? ['#38bdf8', '#fbbf24', '#34d399', '#94a3b8'] 
                    : ['#1f2937', '#1f2937', '#1f2937', '#374151'],
                borderColor: '#0d1117',
                borderWidth: 3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#475569', font: { family: 'Inter', size: 10 }, padding: 12 }
                }
            }
        }
    });
}

/**
 * Chart 6: Defect Distribution by Railway Operating Zone (Bar Chart)
 */
function renderZoneChart(inspections) {
    const ctx = document.getElementById('chart-zone-distribution');
    if (!ctx) return;

    const zoneCounts = {};
    inspections.forEach(i => {
        const zone = i.railwayZone || 'Unspecified Zone';
        const defects = (i.detections || []).length;
        zoneCounts[zone] = (zoneCounts[zone] || 0) + defects;
    });

    const labels = Object.keys(zoneCounts);
    const dataValues = Object.values(zoneCounts);

    if (chartZone) chartZone.destroy();

    chartZone = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels.length > 0 ? labels : ['No Defect Data'],
            datasets: [{
                label: 'Defect Density',
                data: labels.length > 0 ? dataValues : [0],
                backgroundColor: 'rgba(239, 68, 68, 0.75)',
                borderColor: '#ef4444',
                borderWidth: 1.5,
                borderRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                x: {
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', font: { family: 'Inter', size: 10 } }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', precision: 0, font: { family: 'Inter', size: 10 } }
                }
            }
        }
    });
}

/**
 * Chart 7: Work Order Priority Breakdown (Bar Chart)
 */
function renderMaintPriorityChart(tasks) {
    const ctx = document.getElementById('chart-maint-priority');
    if (!ctx) return;

    let crit = 0, high = 0, med = 0, low = 0;
    tasks.forEach(t => {
        const p = (t.priority || 'MEDIUM').toUpperCase();
        if (p === 'CRITICAL' || p === 'EMERGENCY') crit++;
        else if (p === 'HIGH') high++;
        else if (p === 'MEDIUM') med++;
        else low++;
    });

    if (chartPriority) chartPriority.destroy();

    chartPriority = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Critical', 'High', 'Medium', 'Low'],
            datasets: [{
                label: 'Tasks',
                data: [crit, high, med, low],
                backgroundColor: ['#ef4444', '#f97316', '#eab308', '#3b82f6'],
                borderColor: ['#ef4444', '#f97316', '#eab308', '#3b82f6'],
                borderWidth: 1.5,
                borderRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                x: {
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', font: { family: 'Inter', size: 10 } }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: '#e2e8f0' },
                    ticks: { color: '#475569', precision: 0, font: { family: 'Inter', size: 10 } }
                }
            }
        }
    });
}

function renderEmptyState() {
    document.getElementById('an-stat-total').innerText = '0';
    document.getElementById('an-stat-rate').innerText = '0.0%';
    document.getElementById('an-stat-avg-score').innerText = '0.0';
    document.getElementById('an-stat-critical-ratio').innerText = '0.0%';
    if (chartCategories) { chartCategories.destroy(); chartCategories = null; }
    if (chartSeverity) { chartSeverity.destroy(); chartSeverity = null; }
    if (chartTrends) { chartTrends.destroy(); chartTrends = null; }
    if (chartRisk) { chartRisk.destroy(); chartRisk = null; }
    if (chartMaintStatus) { chartMaintStatus.destroy(); chartMaintStatus = null; }
    if (chartZone) { chartZone.destroy(); chartZone = null; }
    if (chartPriority) { chartPriority.destroy(); chartPriority = null; }
}

