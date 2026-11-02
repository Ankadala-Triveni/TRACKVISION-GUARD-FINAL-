/**
 * RailGuard AI – Maintenance Work Orders Controller
 * Manages persistent engineering repair orders via Spring Boot & MySQL.
 */

const API_URL = 'http://localhost:8080/api';
const token = localStorage.getItem('token');

let allTasks = [];
let allInspections = [];

document.addEventListener('DOMContentLoaded', () => {
    loadMaintenanceData();
    loadInspectionsForDropdown();
    initFilterHandlers();
    initModalForms();
});

/**
 * Loads all persistent maintenance work orders from MySQL backend
 */
async function loadMaintenanceData() {
    const tbody = document.getElementById('maintenance-tbody');
    const badge = document.getElementById('m-count-badge');

    try {
        const res = await fetch(`${API_URL}/maintenance`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!res.ok) throw new Error('Failed to retrieve maintenance tasks.');

        const rawTasks = await res.json();
        
        // Deduplicate tasks by unique task ID and show latest work order per inspection
        const seenInspections = new Set();
        const seenIds = new Set();
        allTasks = [];

        for (const t of rawTasks) {
            if (!t || seenIds.has(t.id)) continue;
            seenIds.add(t.id);

            const inspKey = t.inspectionId || `task_${t.id}`;
            if (!seenInspections.has(inspKey)) {
                seenInspections.add(inspKey);
                allTasks.push(t);
            }
        }

        updateKPIs(allTasks);
        renderTable(allTasks);

    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4"><i class="bi bi-exclamation-triangle me-2"></i>${escapeHtml(err.message)}</td></tr>`;
        badge.innerText = '0 work orders';
    }
}

/**
 * Fetches inspections to populate linked inspection selector
 */
async function loadInspectionsForDropdown() {
    const selectEl = document.getElementById('task-inspection-id');
    if (!selectEl) return;

    try {
        const res = await fetch(`${API_URL}/inspections`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!res.ok) return;

        allInspections = await res.json();

        selectEl.innerHTML = '<option value="">Select inspection record...</option>' + 
            allInspections.map(i => {
                const defectCount = (i.detections || []).length;
                return `<option value="${i.id}">${i.inspectionNumber} - ${i.railwayZone || ''} (${i.trackSection || ''}) [${defectCount} defects, ${i.riskLevel || 'LOW'}]</option>`;
            }).join('');

    } catch (err) {
        console.warn('Could not load inspections for dropdown:', err);
    }
}

/**
 * Computes metric card numbers
 */
function updateKPIs(tasks) {
    const total = tasks.length;
    const scheduled = tasks.filter(t => (t.status || '').toUpperCase() === 'SCHEDULED').length;
    const inProgress = tasks.filter(t => (t.status || '').toUpperCase() === 'IN_PROGRESS').length;
    const completed = tasks.filter(t => (t.status || '').toUpperCase() === 'COMPLETED').length;

    document.getElementById('m-stat-total').innerText = total;
    document.getElementById('m-stat-scheduled').innerText = scheduled;
    document.getElementById('m-stat-in-progress').innerText = inProgress;
    document.getElementById('m-stat-completed').innerText = completed;
}

/**
 * Renders filtered task rows into the table
 */
function renderTable(tasks) {
    const tbody = document.getElementById('maintenance-tbody');
    const badge = document.getElementById('m-count-badge');

    badge.innerText = `${tasks.length} work order${tasks.length === 1 ? '' : 's'}`;

    if (tasks.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-5"><i class="bi bi-inbox display-6 d-block mb-2"></i>No maintenance work orders found matching criteria.</td></tr>`;
        return;
    }

    tbody.innerHTML = tasks.map(task => {
        const priority = (task.priority || 'MEDIUM').toUpperCase();
        const status = (task.status || 'SCHEDULED').toUpperCase();
        const schedStr = task.scheduledDate ? formatDate(task.scheduledDate) : '<span class="text-muted">Not specified</span>';

        return `
            <tr>
                <td><strong class="text-dark font-outfit">${escapeHtml(task.taskNumber)}</strong></td>
                <td><span class="risk-badge risk-${priority}">${priority}</span></td>
                <td>
                    <div class="text-dark fw-bold">${escapeHtml(task.title)}</div>
                    <div class="text-secondary small text-truncate" style="max-width: 250px;">${escapeHtml(task.description || 'No additional engineering instructions.')}</div>
                </td>
                <td>
                    <span class="badge bg-light border text-primary font-outfit">
                        ${escapeHtml(task.inspectionNumber || `ID #${task.inspectionId}`)}
                    </span>
                    ${task.defectCategory ? `<div class="small text-danger fw-semibold mt-1">${escapeHtml(task.defectCategory)}</div>` : ''}
                </td>
                <td><span class="text-dark fw-medium small">${escapeHtml(task.assignedTo || 'Unassigned')}</span></td>
                <td><span class="small text-secondary">${schedStr}</span></td>
                <td><span class="status-badge status-${status}">${status}</span></td>
                <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm px-2 py-1" onclick="openStatusModal(${task.id}, '${escapeHtml(task.taskNumber)}', '${status}')" title="Update Repair Status">
                        <i class="bi bi-pencil-square me-1"></i> Update Status
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * Initializes filter and search handlers
 */
function initFilterHandlers() {
    const searchInput = document.getElementById('m-search-input');
    const prioritySelect = document.getElementById('m-filter-priority');
    const statusSelect = document.getElementById('m-filter-status');

    function applyFilters() {
        const query = (searchInput?.value || '').trim().toLowerCase();
        const selectedPriority = (prioritySelect?.value || 'ALL').toUpperCase();
        const selectedStatus = (statusSelect?.value || 'ALL').toUpperCase();

        const filtered = allTasks.filter(t => {
            const matchesQuery = !query || 
                (t.taskNumber || '').toLowerCase().includes(query) ||
                (t.title || '').toLowerCase().includes(query) ||
                (t.assignedTo || '').toLowerCase().includes(query) ||
                (t.inspectionNumber || '').toLowerCase().includes(query) ||
                (t.description || '').toLowerCase().includes(query);

            const matchesPriority = (selectedPriority === 'ALL') || ((t.priority || '').toUpperCase() === selectedPriority);
            const matchesStatus = (selectedStatus === 'ALL') || ((t.status || '').toUpperCase() === selectedStatus);

            return matchesQuery && matchesPriority && matchesStatus;
        });

        renderTable(filtered);
    }

    searchInput?.addEventListener('input', applyFilters);
    prioritySelect?.addEventListener('change', applyFilters);
    statusSelect?.addEventListener('change', applyFilters);
}

/**
 * Initializes modals for Create Task and Update Status
 */
function initModalForms() {
    const btnOpenCreate = document.getElementById('btn-open-create-modal');
    const createModalEl = document.getElementById('createTaskModal');
    const createForm = document.getElementById('create-task-form');
    const createErr = document.getElementById('create-modal-error');
    const createSucc = document.getElementById('create-modal-success');
    const btnSubmitTask = document.getElementById('btn-submit-task');

    if (btnOpenCreate && createModalEl) {
        const createModal = new bootstrap.Modal(createModalEl);
        btnOpenCreate.addEventListener('click', () => {
            createErr.classList.add('d-none');
            createSucc.classList.add('d-none');
            createForm.reset();
            createModal.show();
        });

        createForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            createErr.classList.add('d-none');
            createSucc.classList.add('d-none');

            const inspIdVal = document.getElementById('task-inspection-id').value;
            const scheduledVal = document.getElementById('task-scheduled').value;

            const payload = {
                inspectionId: parseInt(inspIdVal),
                title: document.getElementById('task-title').value.trim(),
                description: document.getElementById('task-desc').value.trim(),
                priority: document.getElementById('task-priority').value,
                assignedTo: document.getElementById('task-assigned').value.trim(),
                scheduledDate: scheduledVal ? new Date(scheduledVal).toISOString() : null
            };

            btnSubmitTask.disabled = true;
            btnSubmitTask.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Issuing...';

            try {
                const res = await fetch(`${API_URL}/maintenance`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Failed to issue work order.');

                createSucc.innerHTML = `<strong>Work order created!</strong> Task Number: <span class="badge bg-warning text-dark font-outfit">${data.taskNumber}</span> has been saved in MySQL.`;
                createSucc.classList.remove('d-none');

                // Immediate data reload to update table & stats in real-time
                loadMaintenanceData();

                setTimeout(() => {
                    createModal.hide();
                }, 1200);

            } catch (err) {
                createErr.innerText = err.message;
                createErr.classList.remove('d-none');
            } finally {
                btnSubmitTask.disabled = false;
                btnSubmitTask.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Issue Work Order';
            }
        });
    }

    // Update Status Form
    const updateModalEl = document.getElementById('updateStatusModal');
    const updateForm = document.getElementById('update-status-form');
    const btnSaveStatus = document.getElementById('btn-save-task-status');

    if (updateModalEl && updateForm) {
        const updateModal = new bootstrap.Modal(updateModalEl);

        window.openStatusModal = function(id, taskNumber, currentStatus) {
            document.getElementById('edit-task-id').value = id;
            document.getElementById('edit-task-number').innerText = taskNumber;
            document.getElementById('edit-task-status').value = currentStatus;
            updateModal.show();
        };

        updateForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('edit-task-id').value;
            const newStatus = document.getElementById('edit-task-status').value;

            btnSaveStatus.disabled = true;
            btnSaveStatus.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Saving...';

            try {
                const res = await fetch(`${API_URL}/maintenance/${id}/status`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ status: newStatus })
                });

                const data = await res.json();
                if (!res.ok) throw new Error(data.message || 'Failed to update status.');

                updateModal.hide();
                loadMaintenanceData();

            } catch (err) {
                alert(`Error updating status: ${err.message}`);
            } finally {
                btnSaveStatus.disabled = false;
                btnSaveStatus.innerHTML = 'Save Status';
            }
        });
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
