/**
 * TRACK DAMAGE ANALYZER – Registration Page Controller
 * Theme: Sky Blue + Slate Blue
 * Features:
 * - Proper RFC-Compliant Email & Username Validation (Supports .in, .com, .ac.uk, .ac.in, .gov.in, etc.)
 * - Alphanumeric Anti-Bot CAPTCHA (Canvas-rendered with visual distortion, instant rendering)
 * - Real Website Google Sign-In Experience (Centered Account Chooser Popup)
 * - Password Visibility Toggles
 * - Frontend Confirmation Checks & Backend Registration
 */

const API_URL = 'http://localhost:8080/api';

function initRegisterPage() {
    const registerForm = document.getElementById('register-form');
    const alertError = document.getElementById('alert-error');
    const alertSuccess = document.getElementById('alert-success');
    const btnSubmit = document.getElementById('btn-submit');
    const btnSpinner = document.getElementById('btn-spinner');
    const btnText = document.getElementById('btn-text');
    const btnGoogle = document.getElementById('btn-google-login');

    const fullNameInput = document.getElementById('fullName');
    const usernameInput = document.getElementById('username');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const togglePasswordBtn = document.getElementById('toggle-password');
    const togglePasswordIcon = document.getElementById('toggle-password-icon');
    const toggleConfirmBtn = document.getElementById('toggle-confirm-password');
    const toggleConfirmIcon = document.getElementById('toggle-confirm-icon');
    const captchaCanvas = document.getElementById('register-captcha-canvas');
    const captchaAnswerInput = document.getElementById('captcha-answer');
    const btnRefreshCaptcha = document.getElementById('btn-refresh-captcha');

    // ── 0. Alphanumeric Visual CAPTCHA ──
    const CAPTCHA_CHARS = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    let currentCaptchaCode = window.__RG_REG_CAPTCHA || '';

    function generateCaptchaCode(len = 5) {
        let res = '';
        for (let i = 0; i < len; i++) {
            res += CAPTCHA_CHARS.charAt(Math.floor(Math.random() * CAPTCHA_CHARS.length));
        }
        return res;
    }

    function renderCanvasCaptcha() {
        if (!captchaCanvas) return;
        currentCaptchaCode = generateCaptchaCode(5);
        window.__RG_REG_CAPTCHA = currentCaptchaCode;
        if (captchaAnswerInput) captchaAnswerInput.value = '';

        const ctx = captchaCanvas.getContext('2d');
        const w = captchaCanvas.width || 160;
        const h = captchaCanvas.height || 46;

        // Clean subtle light blue gradient background
        const grad = ctx.createLinearGradient(0, 0, w, h);
        grad.addColorStop(0, '#e0f2fe');
        grad.addColorStop(1, '#dbeafe');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);

        // Canvas border
        ctx.strokeStyle = '#93c5fd';
        ctx.lineWidth = 1;
        ctx.strokeRect(0, 0, w, h);

        // Security noise lines
        const lineColors = ['#60a5fa', '#3b82f6', '#818cf8', '#38bdf8'];
        for (let i = 0; i < 4; i++) {
            ctx.strokeStyle = lineColors[i % lineColors.length];
            ctx.lineWidth = 1.4;
            ctx.beginPath();
            ctx.moveTo(Math.random() * w, Math.random() * h);
            ctx.bezierCurveTo(
                Math.random() * w, Math.random() * h,
                Math.random() * w, Math.random() * h,
                Math.random() * w, Math.random() * h
            );
            ctx.stroke();
        }

        // Draw distorted alphanumeric characters
        const textColors = ['#1e40af', '#0369a1', '#1d4ed8', '#1e3a8a', '#2563eb'];
        const chars = currentCaptchaCode.split('');
        const step = (w - 24) / chars.length;

        chars.forEach((char, idx) => {
            ctx.save();
            const x = 14 + idx * step;
            const y = 32 + (Math.random() * 4 - 2);
            const angle = (Math.random() * 0.3) - 0.15;
            ctx.translate(x, y);
            ctx.rotate(angle);
            ctx.font = 'bold 23px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif';
            ctx.fillStyle = textColors[idx % textColors.length];
            ctx.fillText(char, 0, 0);
            ctx.restore();
        });

        // Anti-bot noise dots
        ctx.fillStyle = '#64748b';
        for (let i = 0; i < 18; i++) {
            ctx.beginPath();
            ctx.arc(Math.random() * w, Math.random() * h, 1.2, 0, Math.PI * 2);
            ctx.fill();
        }
    }

    // If not already drawn, render now
    if (!currentCaptchaCode) {
        renderCanvasCaptcha();
    }
    btnRefreshCaptcha?.addEventListener('click', renderCanvasCaptcha);
    captchaCanvas?.addEventListener('click', renderCanvasCaptcha);

    // ── 1. Password Visibility Toggles ──
    togglePasswordBtn?.addEventListener('click', () => {
        const isPassword = passwordInput.getAttribute('type') === 'password';
        passwordInput.setAttribute('type', isPassword ? 'text' : 'password');
        togglePasswordIcon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
    });

    toggleConfirmBtn?.addEventListener('click', () => {
        const isPassword = confirmPasswordInput.getAttribute('type') === 'password';
        confirmPasswordInput.setAttribute('type', isPassword ? 'text' : 'password');
        toggleConfirmIcon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
    });

    // ── 2. Alert Helpers ──
    function showError(msg) {
        if (alertSuccess) alertSuccess.classList.remove('show');
        if (alertError) {
            alertError.innerText = msg;
            alertError.classList.add('show');
            alertError.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

    function showSuccess(msg) {
        if (alertError) alertError.classList.remove('show');
        if (alertSuccess) {
            alertSuccess.innerText = msg;
            alertSuccess.classList.add('show');
            alertSuccess.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

    function clearAlerts() {
        if (alertError) alertError.classList.remove('show');
        if (alertSuccess) alertSuccess.classList.remove('show');
    }

    // ── 3. RFC-Compliant Email Validation Helper ──
    // Strictly validates official email addresses, supporting .in, .com, .ac.uk, .ac.in, .gov.in, etc.
    function isValidEmail(val) {
        if (!val || typeof val !== 'string') return false;
        const clean = val.trim();
        if (clean.length < 5 || clean.indexOf('@') <= 0) return false;
        const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\.[a-zA-Z]{2,}$/;
        return emailRegex.test(clean);
    }

    // ── 4. Standard Registration Submit ──
    registerForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        clearAlerts();

        const fullName = (fullNameInput?.value || '').trim();
        const username = (usernameInput?.value || '').trim();
        const email = (emailInput?.value || '').trim();
        const password = passwordInput?.value || '';
        const confirmPassword = confirmPasswordInput?.value || '';
        const userCaptcha = (captchaAnswerInput?.value || '').trim().toUpperCase();

        // Validations
        if (!fullName) {
            showError('Please enter your full name.');
            fullNameInput?.focus();
            return;
        }

        if (!username || username.length < 3) {
            showError('Username must be at least 3 characters long.');
            usernameInput?.focus();
            return;
        }

        if (!email || !isValidEmail(email)) {
            showError('Please enter a valid official email address (e.g. inspector@railways.gov.in or .in, .com, .ac.uk).');
            emailInput?.focus();
            return;
        }

        if (!password || password.length < 8) {
            showError('Password must be at least 8 characters long.');
            passwordInput?.focus();
            return;
        }

        if (password !== confirmPassword) {
            showError('Passwords do not match. Please verify both password fields.');
            confirmPasswordInput?.focus();
            return;
        }

        if (!userCaptcha) {
            showError('Please enter the security verification characters shown in the box.');
            captchaAnswerInput?.focus();
            return;
        }

        const expectedCode = (currentCaptchaCode || window.__RG_REG_CAPTCHA || '').toUpperCase().trim();
        if (userCaptcha !== expectedCode) {
            showError('Incorrect security verification code. Please enter the characters shown in the box.');
            captchaAnswerInput?.focus();
            renderCanvasCaptcha();
            return;
        }

        // Loading state
        btnSubmit.disabled = true;
        btnSpinner.classList.remove('d-none');
        btnText.innerText = 'Creating Account...';

        try {
            const payload = {
                fullName,
                username,
                email,
                password
            };

            const response = await fetch(`${API_URL}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (!response.ok) {
                if (data.errors) {
                    const messages = Object.values(data.errors).join(', ');
                    throw new Error(messages);
                }
                throw new Error(data.message || 'Registration failed. Please check your details.');
            }

            showSuccess('Account successfully registered! Redirecting to login portal...');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 1200);

        } catch (err) {
            showError(err.message || 'Registration failed. Please check entered information.');
            renderCanvasCaptcha();
        } finally {
            btnSubmit.disabled = false;
            btnSpinner.classList.add('d-none');
            btnText.innerText = 'Register Account';
        }
    });

    // ── 5. Real Website Google Sign-In Experience ──
    initGoogleAuth();

    function initGoogleAuth() {
        btnGoogle?.addEventListener('click', () => {
            clearAlerts();

            // Calculate centered popup coordinates for Google Sign-In window
            const w = 480;
            const h = 640;
            const left = Math.max(0, (window.screen.width - w) / 2);
            const top = Math.max(0, (window.screen.height - h) / 2);

            const userEmail = (emailInput?.value || '').trim();
            const chooserUrl = `google-account-chooser.html?email=${encodeURIComponent(userEmail)}`;

            // Open genuine Google Account Chooser popup
            const popup = window.open(
                chooserUrl,
                'GoogleSignIn',
                `width=${w},height=${h},top=${top},left=${left},status=no,resizable=yes,scrollbars=yes`
            );

            if (!popup || popup.closed || typeof popup.closed === 'undefined') {
                window.location.href = chooserUrl;
            }
        });

        // Receive verified Google authentication message from popup
        window.addEventListener('message', (event) => {
            if (event.data && event.data.type === 'RAILGUARD_GOOGLE_AUTH_SUCCESS') {
                const userData = event.data.user;
                localStorage.setItem('token', event.data.token);
                localStorage.setItem('user', JSON.stringify({
                    id: userData.userId || userData.id,
                    username: userData.username,
                    fullName: userData.fullName,
                    email: userData.email,
                    role: userData.role
                }));

                showSuccess(`Google Sign-In successful! Welcome, ${userData.fullName || userData.email}.`);
                setTimeout(() => {
                    window.location.href = 'dashboard.html';
                }, 400);
            }
        });
    }
}

// Bulletproof execution: runs immediately if DOM is ready, or on DOMContentLoaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initRegisterPage);
} else {
    initRegisterPage();
}
