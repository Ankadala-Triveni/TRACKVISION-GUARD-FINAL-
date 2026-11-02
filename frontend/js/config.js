/**
 * RailGuard AI – External Integrations Configuration
 * 
 * To enable Google Sign-In:
 * 1. Open Google Cloud Console -> APIs & Services -> Credentials
 *    (https://console.cloud.google.com/apis/credentials)
 * 2. Create an OAuth 2.0 Client ID (Application type: "Web application")
 * 3. Add your frontend origin (e.g., http://localhost:5500 and http://127.0.0.1:5500)
 *    to "Authorized JavaScript origins"
 * 4. Add http://localhost:5500/pages/login.html to "Authorized redirect URIs"
 * 5. Paste the Client ID into GOOGLE_CLIENT_ID below, OR set GOOGLE_CLIENT_ID in your root .env file.
 */
window.RAILGUARD_CONFIG = {
    // Backend API base URL
    API_URL: "http://localhost:8080/api",

    // Google Cloud OAuth 2.0 Web Client ID
    // Leave blank to automatically load from backend GOOGLE_CLIENT_ID environment variable
    GOOGLE_CLIENT_ID: "",

    // Optional Google reCAPTCHA v2/v3 Site Key
    RECAPTCHA_SITE_KEY: "",

    // Detected local runtime origin
    CURRENT_ORIGIN: window.location.origin
};
