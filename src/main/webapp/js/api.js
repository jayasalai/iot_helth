/**
 * API Client Module - Handles all AJAX communications with Java Servlets.
 */
const API = {
    async request(url, options = {}) {
        const defaultHeaders = {
            'Accept': 'application/json'
        };

        if (options.body && typeof options.body === 'object') {
            options.body = JSON.stringify(options.body);
            defaultHeaders['Content-Type'] = 'application/json';
        }

        options.headers = { ...defaultHeaders, ...(options.headers || {}) };

        try {
            const response = await fetch(url, options);

            // If session expired or unauthorized, redirect to login
            if (response.status === 401 && !window.location.pathname.endsWith('login.html')) {
                window.location.href = 'login.html';
                throw new Error('Session expired. Please log in.');
            }

            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || data.message || `HTTP Error ${response.status}`);
            }
            return data;
        } catch (err) {
            console.error(`[API ERROR] ${options.method || 'GET'} ${url}:`, err);
            throw err;
        }
    },

    // Authentication Endpoints
    login(username, password) {
        return this.request('/api/auth/login', {
            method: 'POST',
            body: { username, password }
        });
    },

    logout() {
        return this.request('/api/auth/logout', { method: 'POST' });
    },

    getCurrentUser() {
        return this.request('/api/auth/me');
    },

    // Health Diagnostic
    checkHealth() {
        return this.request('/api/health');
    },

    // Dashboard Analytics
    getDashboardStats() {
        return this.request('/api/dashboard/stats');
    },

    // Patient CRUD
    getPatients(search = '', page = 1, pageSize = 10) {
        const params = new URLSearchParams();
        if (search) params.append('search', search);
        params.append('page', page);
        params.append('pageSize', pageSize);
        return this.request(`/api/patients?${params.toString()}`);
    },

    getPatientById(id) {
        return this.request(`/api/patients?id=${encodeURIComponent(id)}`);
    },

    createPatient(patientData) {
        return this.request('/api/patients', {
            method: 'POST',
            body: patientData
        });
    },

    updatePatient(patientData) {
        return this.request('/api/patients', {
            method: 'PUT',
            body: patientData
        });
    },

    deletePatient(id) {
        return this.request(`/api/patients?id=${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });
    },

    // IoT Telemetry Readings
    recordVitals(vitalsData) {
        return this.request('/api/vitals', {
            method: 'POST',
            body: vitalsData
        });
    },

    getPatientHistory(patientId, fromDate = '', toDate = '') {
        const params = new URLSearchParams();
        params.append('patientId', patientId);
        if (fromDate) params.append('from', fromDate);
        if (toDate) params.append('to', toDate);
        return this.request(`/api/vitals?${params.toString()}`);
    },

    getAbnormalAlerts(limit = 10) {
        return this.request(`/api/vitals/abnormal?limit=${limit}`);
    }
};
