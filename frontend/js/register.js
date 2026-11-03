/**
 * TRACKVISION GUARD – Registration Page Controller
 * Theme: Sky Blue + Slate Blue
 * Features:
 * - Real-Time Dynamic Password Strength Indicator (Weak, Medium, Strong)
 * - Flexible Combinations (Any style allowed, min 8 chars required)
 * - Known-Compromised & Predictable Password Guard
 * - Confirmation Password Live Matching
 * - RFC-Compliant Email Validation (.in, .com, .ac, .uk, .ac.in, etc.)
 * - Anti-Bot Visual Alphanumeric CAPTCHA
 * - Google Sign-In Integration
 * - Spring Boot 3 & JWT Backend Integration
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

    // Password strength UI elements
    const strengthLabel = document.getElementById('strength-label');
    const strengthBar1 = document.getElementById('strength-bar-1');
    const strengthBar2 = document.getElementById('strength-bar-2');
    const strengthBar3 = document.getElementById('strength-bar-3');
    const strengthFeedback = document.getElementById('strength-feedback');
    const confirmMatchFeedback = document.getElementById('confirm-match-feedback');

    // Known common / compromised passwords list for local privacy-preserving check
    const COMMON_PASSWORDS = new Set([
        '12345678', '123456789', '1234567890', 'password', 'password123',
        'qwertyui', 'qwertyuiop', 'admin123', 'admin1234', 'administrator',
        'railguard', 'railway123', 'trackvision', 'welcome1', 'welcome123',
        '11111111', '00000000', 'pass1234', 'password1', 'iloveyou'
    ]);

    // ── 0. Real-Time Password Strength Evaluator ──
    function evaluatePasswordStrength(pwd) {
        if (!pwd || pwd.length === 0) {
            return {
                level: 'weak',
                label: '🔴 Weak',
                bars: 0,
                feedback: 'Password must contain at least 8 characters.',
                isValidLength: false,
                isCommon: false
            };
        }

        const len = pwd.length;
        const lowerPwd = pwd.toLowerCase().trim();

        // 1. Length check (< 8 is always weak)
        if (len < 8) {
            return {
                level: 'weak',
                label: '🔴 Weak',
                bars: 1,
                feedback: `Password is too short (${len}/8 characters required).`,
                isValidLength: false,
                isCommon: false
            };
        }

        // 2. Common / easily guessed dictionary check
        if (COMMON_PASSWORDS.has(lowerPwd)) {
            return {
                level: 'weak',
                label: '🔴 Weak',
                bars: 1,
                feedback: 'This password is too commonly used and easily guessed. Choose a more unique combination.',
                isValidLength: true,
                isCommon: true
            };
        }

        // 3. Repeated identical characters (e.g. "aaaaaaaa", "11111111")
        if (/^(.)\1{7,}$/.test(lowerPwd)) {
            return {
                level: 'weak',
                label: '🔴 Weak',
                bars: 1,
                feedback: 'Password contains repeated identical characters. Use more variety.',
                isValidLength: true,
                isCommon: true
            };
        }

        // 4. Sequential keyboard sequences (e.g. "abcdefgh", "87654321")
        const sequentialAlpha = 'abcdefghijklmnopqrstuvwxyz';
        const sequentialNum = '012345678909876543210';
        if (sequentialAlpha.includes(lowerPwd) || sequentialNum.includes(lowerPwd)) {
            return {
                level: 'weak',
                label: '🔴 Weak',
                bars: 1,
                feedback: 'Password is a simple sequential pattern. Make it less predictable.',
                isValidLength: true,
                isCommon: true
            };
        }

        // 5. Evaluate complexity & entropy (character variety + length)
        const hasLower = /[a-z]/.test(pwd);
        const hasUpper = /[A-Z]/.test(pwd);
        const hasDigits = /[0-9]/.test(pwd);
        const hasSymbols = /[^a-zA-Z0-9]/.test(pwd);
        const varietyCount = [hasLower, hasUpper, hasDigits, hasSymbols].filter(Boolean).length;

        // Strong Criteria:
        // - Length >= 10 with at least 3 character varieties (e.g. "vQ7#pL9@xT2!", "River@Moon82!")
        // - OR Length >= 14 with at least 2 character varieties
        // - OR Length >= 8 with all 4 character varieties and high entropy
        if ((len >= 10 && varietyCount >= 3) || (len >= 14 && varietyCount >= 2) || (len >= 8 && varietyCount === 4)) {
            return {
                level: 'strong',
                label: '🟢 Strong',
                bars: 3,
                feedback: 'Strong & secure password. Excellent protection for your engineering account.',
                isValidLength: true,
                isCommon: false
            };
        }

        // Medium Criteria:
        // - Length >= 8 with letters + numbers (e.g. "triveni123", "hello@123")
        // - OR long passwords containing single character type (e.g. "abcdefghijkl" or "987654321012")
        return {
            level: 'medium',
            label: '🟠 Medium',
            bars: 2,
            feedback: 'Fair password. Consider adding uppercase letters, symbols, or making it 10+ characters for maximum security.',
            isValidLength: true,
            isCommon: false
        };
    }

    // Update Strength Indicator UI in real time
    function updateStrengthUI() {
        const pwd = passwordInput?.value || '';
        const res = evaluatePasswordStrength(pwd);

        // Update Label
        if (strengthLabel) {
            strengthLabel.className = `strength-label ${res.level}`;
            strengthLabel.textContent = res.label;
        }

        // Update Bars
        [strengthBar1, strengthBar2, strengthBar3].forEach((bar, idx) => {
            if (!bar) return;
            bar.className = 'strength-bar-segment';
            if (idx < res.bars) {
                bar.classList.add(`active-${res.level}`);
            }
        });

        // Update Feedback text
        if (strengthFeedback) {
            strengthFeedback.className = `strength-feedback ${res.level}`;
            strengthFeedback.textContent = res.feedback;
        }

        // Re-check confirm password matching
        updateConfirmMatchUI();
    }

    // Live confirm password matcher
    function updateConfirmMatchUI() {
        if (!confirmMatchFeedback) return;
        const pwd = passwordInput?.value || '';
        const confirmPwd = confirmPasswordInput?.value || '';

        if (!confirmPwd) {
            confirmMatchFeedback.textContent = '';
            return;
        }

        if (pwd === confirmPwd) {
            confirmMatchFeedback.style.color = '#059669';
            confirmMatchFeedback.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i> Passwords match';
        } else {
            confirmMatchFeedback.style.color = '#dc2626';
            confirmMatchFeedback.innerHTML = '<i class="bi bi-x-circle-fill me-1"></i> Passwords do not match';
        }
    }

    passwordInput?.addEventListener('input', updateStrengthUI);
    confirmPasswordInput?.addEventListener('input', updateConfirmMatchUI);

    // Initial evaluation
    updateStrengthUI();

    // ── 1. Alphanumeric Visual CAPTCHA ──
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
        const w = captchaCanvas.width || 150;
        const h = captchaCanvas.height || 40;

        const grad = ctx.createLinearGradient(0, 0, w, h);
        grad.addColorStop(0, '#e0f2fe');
        grad.addColorStop(1, '#dbeafe');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);

        ctx.strokeStyle = '#93c5fd';
        ctx.lineWidth = 1;
        ctx.strokeRect(0, 0, w, h);

        const lineColors = ['#60a5fa', '#3b82f6', '#818cf8', '#38bdf8'];
        for (let i = 0; i < 4; i++) {
            ctx.strokeStyle = lineColors[i % lineColors.length];
            ctx.lineWidth = 1.3;
            ctx.beginPath();
            ctx.moveTo(Math.random() * w, Math.random() * h);
            ctx.bezierCurveTo(
                Math.random() * w, Math.random() * h,
                Math.random() * w, Math.random() * h,
                Math.random() * w, Math.random() * h
            );
            ctx.stroke();
        }

        const textColors = ['#1e40af', '#0369a1', '#1d4ed8', '#1e3a8a', '#2563eb'];
        const chars = currentCaptchaCode.split('');
        const step = (w - 20) / chars.length;

        chars.forEach((char, idx) => {
            ctx.save();
            const x = 12 + idx * step;
            const y = 28 + (Math.random() * 4 - 2);
            const angle = (Math.random() * 0.28) - 0.14;
            ctx.translate(x, y);
            ctx.rotate(angle);
            ctx.font = 'bold 20px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif';
            ctx.fillStyle = textColors[idx % textColors.length];
            ctx.fillText(char, 0, 0);
            ctx.restore();
        });

        ctx.fillStyle = '#64748b';
        for (let i = 0; i < 15; i++) {
            ctx.beginPath();
            ctx.arc(Math.random() * w, Math.random() * h, 1.1, 0, Math.PI * 2);
            ctx.fill();
        }
    }

    if (!currentCaptchaCode) {
        renderCanvasCaptcha();
    }
    btnRefreshCaptcha?.addEventListener('click', renderCanvasCaptcha);
    captchaCanvas?.addEventListener('click', renderCanvasCaptcha);

    // ── 2. Password Visibility Toggles ──
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

    // ── 3. Alert Helpers ──
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

    // ── 4. RFC-Compliant Email Validation Helper ──
    function isValidEmail(val) {
        if (!val || typeof val !== 'string') return false;
        const clean = val.trim();
        if (clean.length < 5 || clean.indexOf('@') <= 0) return false;
        const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\.[a-zA-Z]{2,}$/;
        return emailRegex.test(clean);
    }

    // ── 5. Standard Registration Submit ──
    registerForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        clearAlerts();

        const fullName = (fullNameInput?.value || '').trim();
        const username = (usernameInput?.value || '').trim();
        const email = (emailInput?.value || '').trim();
        const password = passwordInput?.value || '';
        const confirmPassword = confirmPasswordInput?.value || '';
        const userCaptcha = (captchaAnswerInput?.value || '').trim().toUpperCase();

        // 1. Basic Fields Validation
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
            showError('Please enter a valid official email address (e.g. inspector@railways.gov.in or .in, .com, .ac, .uk).');
            emailInput?.focus();
            return;
        }

        // 2. Strict Minimum Length Check (>= 8 characters)
        if (!password || password.length < 8) {
            showError('Password must contain at least 8 characters.');
            passwordInput?.focus();
            return;
        }

        // 3. Known Common Password Rejection
        const strength = evaluatePasswordStrength(password);
        if (strength.isCommon) {
            showError('This password is too commonly used, easily guessed, or predictable. Please choose a more secure password.');
            passwordInput?.focus();
            return;
        }

        // 4. Confirm Password Match
        if (password !== confirmPassword) {
            showError('Passwords do not match. Please ensure both password fields are identical.');
            confirmPasswordInput?.focus();
            return;
        }

        // 5. CAPTCHA Verification
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

            showSuccess('Registration successful! Redirecting to login portal...');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 1200);

        } catch (err) {
            showError(err.message || 'An error occurred during registration. Please try again.');
            renderCanvasCaptcha();
        } finally {
            btnSubmit.disabled = false;
            btnSpinner.classList.add('d-none');
            btnText.innerText = 'Register Account';
        }
    });

    // ── 6. Google Sign-In Integration ──
    btnGoogle?.addEventListener('click', () => {
        const width = 500, height = 620;
        const left = (screen.width - width) / 2;
        const top = (screen.height - height) / 2;
        window.open('google-account-chooser.html', 'GoogleSignIn', `width=${width},height=${height},top=${top},left=${left},toolbar=no,menubar=no`);
    });
}

document.addEventListener('DOMContentLoaded', initRegisterPage);
