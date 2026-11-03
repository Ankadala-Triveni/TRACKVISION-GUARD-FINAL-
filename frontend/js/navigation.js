/**
 * RailGuard AI – Unified Navigation & Session Manager
 * Provides consistent sidebar functionality, mobile drawer toggle,
 * user profile resolution, and authentication lifecycle across all pages.
 */

const API_BASE_URL = 'http://localhost:8080/api';
const authToken = localStorage.getItem('token');

// Redirect immediately to login if unauthenticated (unless on auth pages)
(function enforceAuth() {
    const path = window.location.pathname.toLowerCase();
    const isAuthPage = path.endsWith('login.html') || path.endsWith('register.html');
    if (!authToken && !isAuthPage) {
        window.location.href = 'login.html';
    }
})();

document.addEventListener('DOMContentLoaded', () => {
    initSharedSidebar();
    initSharedLogout();
    highlightCurrentNavLink();
    if (authToken) {
        fetchCurrentUserProfile();
    }
});

/**
 * Mobile sidebar drawer toggle handler
 */
function initSharedSidebar() {
    const toggleBtn = document.getElementById('mobile-sidebar-toggle');
    const sidebar = document.getElementById('app-sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (toggleBtn && sidebar && overlay) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('show');
            overlay.classList.toggle('show');
        });

        overlay.addEventListener('click', () => {
            sidebar.classList.remove('show');
            overlay.classList.remove('show');
        });
    }
}

/**
 * Shared logout handler - clears JWT session and navigates to login
 */
function initSharedLogout() {
    const logoutElements = document.querySelectorAll('.action-logout, #btn-logout');
    logoutElements.forEach(el => {
        el.addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.removeItem('token');
            window.location.href = 'login.html';
        });
    });
}

/**
 * Automatically sets the 'active' class on the matching sidebar link
 */
function highlightCurrentNavLink() {
    const currentFile = window.location.pathname.split('/').pop().toLowerCase() || 'dashboard.html';
    const currentHash = window.location.hash.toLowerCase();
    const navLinks = document.querySelectorAll('.nav-link-rg');

    navLinks.forEach(link => {
        link.classList.remove('active');
        const href = (link.getAttribute('href') || '').toLowerCase();

        if (currentHash.includes('results') && href.includes('#results')) {
            link.classList.add('active');
        } else if (!currentHash.includes('results') && !href.includes('#') && (href === currentFile || href.endsWith('/' + currentFile))) {
            link.classList.add('active');
        }
    });
}

window.addEventListener('hashchange', highlightCurrentNavLink);

/**
 * Retrieves authenticated user details from backend GET /api/auth/me
 */
async function fetchCurrentUserProfile() {
    try {
        const res = await fetch(`${API_BASE_URL}/auth/me`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (res.status === 401 || res.status === 403) {
            localStorage.removeItem('token');
            window.location.href = 'login.html';
            return;
        }

        if (!res.ok) return;

        const user = await res.json();
        
        const nameEls = document.querySelectorAll('#user-name, .user-name-display');
        const roleEls = document.querySelectorAll('#user-role, .user-role-display');
        const welcomeEls = document.querySelectorAll('#welcome-user-name');

        nameEls.forEach(el => el.innerText = user.fullName || user.username);
        roleEls.forEach(el => el.innerText = user.role || 'INSPECTOR');
        welcomeEls.forEach(el => el.innerText = user.fullName ? user.fullName.split(' ')[0] : user.username);

    } catch (err) {
        console.warn('Navigation profile fetch:', err.message);
    }
}

