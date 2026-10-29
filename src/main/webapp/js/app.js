/**
 * Main Application Controller - Connects UI with Java Servlets & Oracle JDBC Backend.
 */
const App = {
    currentUser: null,
    patientsCache: [],
    currentPatientPage: 1,
    currentPatientSearch: '',
    deleteTargetId: null,

    init() {
        this.bindEvents();
        this.checkAuth();
        this.verifyDbDiagnostics();
        this.initSimulatorSliders();
    },

    bindEvents() {
        // Tab Navigation
        document.querySelectorAll('.nav-tab').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const target = btn.dataset.tab;
                this.switchTab(target);
            });
        });

        // Logout
        document.getElementById('logoutBtn').addEventListener('click', () => this.handleLogout());

        // Dashboard Refresh Button
        document.getElementById('refreshDashboardBtn').addEventListener('click', () => {
            this.loadDashboardData();
            this.showToast('Metrics refreshed from Oracle Database', 'info');
        });

        // Patient Modal Form Submit
        document.getElementById('patientForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleSavePatient();
        });

        // Patient Search on Enter Key
        document.getElementById('patientSearchInput').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                this.searchPatients();
            }
        });

        // IoT Simulator Form Submit
        document.getElementById('iotSimulatorForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleRecordTelemetry();
        });
    },

    switchTab(tabId) {
        document.querySelectorAll('.nav-tab').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabId);
        });

        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.toggle('active', pane.id === tabId);
        });

        if (tabId === 'dashboardTab') {
            this.loadDashboardData();
        } else if (tabId === 'patientsTab') {
            this.loadPatients(this.currentPatientSearch, this.currentPatientPage);
        } else if (tabId === 'simulatorTab') {
            this.populatePatientDropdowns();
            this.updateSimulatorPreview();
        } else if (tabId === 'historyTab') {
            this.populatePatientDropdowns();
            const histSelect = document.getElementById('historyPatientSelect');
            if (histSelect.value) {
                this.loadPatientHistory();
            }
        }
    },

    // ------------------------------------------------------------------------
    // AUTHENTICATION & DIAGNOSTICS
    // ------------------------------------------------------------------------
    async checkAuth() {
        try {
            const res = await API.getCurrentUser();
            if (res.data) {
                this.currentUser = res.data;
                document.getElementById('userNameDisplay').textContent = res.data.username;
                document.getElementById('userRoleDisplay').textContent = res.data.role;
                document.getElementById('userAvatar').textContent = res.data.username.charAt(0).toUpperCase();
                
                // Initial data load
                this.loadDashboardData();
                this.populatePatientDropdowns();
            }
        } catch (err) {
            console.warn('[AUTH] Not logged in, redirecting to login.html');
            window.location.href = 'login.html';
        }
    },

    async handleLogout() {
        try {
            await API.logout();
        } catch (ignored) {}
        window.location.href = 'login.html';
    },

    async verifyDbDiagnostics() {
        const dot = document.getElementById('headerDbDot');
        const text = document.getElementById('headerDbText');
        const diagBox = document.getElementById('diagnosticsDetails');

        try {
            const res = await API.checkHealth();
            if (res.data && res.data.databaseConnected) {
                dot.className = 'db-dot';
                dot.style.background = 'var(--status-normal)';
                dot.style.boxShadow = '0 0 8px var(--status-normal)';
                text.textContent = 'Oracle Connected';
                diagBox.innerHTML = `
                    <div style="color: var(--status-normal); margin-bottom: 0.5rem; font-weight: 600;">
                        ● Status: Oracle Database Connection Healthy
                    </div>
                    <div><strong>Product:</strong> ${res.data.databaseProduct}</div>
                    <div><strong>Version:</strong> ${res.data.databaseVersion}</div>
                    <div><strong>Driver:</strong> ${res.data.driverName} (${res.data.driverVersion})</div>
                    <div><strong>Target URL:</strong> <code>${res.data.configuredUrl}</code></div>
                    <div><strong>User:</strong> <code>${res.data.configuredUser}</code></div>
                `;
            } else {
                dot.className = 'db-dot unreachable';
                dot.style.background = 'var(--status-warning)';
                dot.style.boxShadow = '0 0 8px var(--status-warning)';
                text.textContent = 'Oracle Offline (Preview Mode)';
                diagBox.innerHTML = `
                    <div style="color: var(--status-warning); margin-bottom: 0.5rem; font-weight: 600;">
                        ● Status: Oracle Listener Offline (Demo Preview Active)
                    </div>
                    <div><strong>Target URL:</strong> <code>${res.data?.configuredUrl || 'jdbc:oracle:thin:@//localhost:1521/XEPDB1'}</code></div>
                    <div><strong>Diagnosis:</strong> <span style="color: var(--status-warning);">${res.data?.errorMessage || 'No listener at localhost:1521'}</span></div>
                    <div style="margin-top: 0.75rem; color: var(--text-secondary); font-size: 0.8rem; line-height: 1.5;">
                        The web app is running in <strong>Educational Preview Mode</strong> using seed clinical data.<br>
                        Once your Oracle Database instance or college remote server is online, this badge will automatically turn green.
                    </div>
                `;
            }
        } catch (err) {
            dot.className = 'db-dot unreachable';
            dot.style.background = 'var(--status-warning)';
            dot.style.boxShadow = '0 0 8px var(--status-warning)';
            text.textContent = 'Oracle Offline (Preview Mode)';
            diagBox.innerHTML = `
                <div style="color: var(--status-warning); margin-bottom: 0.5rem; font-weight: 600;">
                    ● Status: Oracle Listener Offline (Demo Preview Active)
                </div>
                <div style="color: var(--text-muted); font-size: 0.8rem;">
                    Local Oracle Database listener is not active on port 1521. Interactive preview records are displayed.
                </div>
            `;
        }
    },

    // ------------------------------------------------------------------------
    // DASHBOARD OVERVIEW METRICS
    // ------------------------------------------------------------------------
    async loadDashboardData() {
        try {
            const res = await API.getDashboardStats();
            const stats = res.data;

            // Stat Cards (Calculated from Oracle SQL Aggregates)
            document.getElementById('statTotalPatients').textContent = stats.totalPatients || 0;
            document.getElementById('statTotalReadings').textContent = stats.totalReadings || 0;
            document.getElementById('statNormalPatients').textContent = stats.normalPatientsCount || 0;
            document.getElementById('statAbnormalPatients').textContent = stats.abnormalPatientsCount || 0;

            // Population Averages
            document.getElementById('avgTempDisplay').textContent = (stats.avgTemperature ? stats.avgTemperature.toFixed(1) : '0.0') + ' °C';
            document.getElementById('avgHrDisplay').textContent = (stats.avgHeartRate ? Math.round(stats.avgHeartRate) : '0') + ' BPM';
            document.getElementById('avgSpo2Display').textContent = (stats.avgSpo2 ? stats.avgSpo2.toFixed(1) : '0.0') + ' %';

            // Recent Telemetry Table
            this.renderRecentReadingsTable(stats.recentActivity || []);

            // Abnormal Alerts Queue
            this.renderAbnormalAlerts(stats.abnormalAlerts || []);

        } catch (err) {
            console.error('[DASHBOARD ERROR]', err);
            this.showToast('Failed to load dashboard metrics: ' + err.message, 'error');
        }
    },

    renderRecentReadingsTable(readings) {
        const tbody = document.getElementById('recentReadingsTbody');
        if (!readings || readings.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="table-empty">No telemetry readings recorded in database yet.</td></tr>';
            return;
        }

        tbody.innerHTML = readings.map(r => `
            <tr>
                <td style="font-weight: 600;">
                    <a href="javascript:void(0)" onclick="App.viewPatientHistory(${r.patientId})" style="color: var(--primary); text-decoration: none;">
                        ${this.escapeHtml(r.patientName || 'Patient #' + r.patientId)}
                    </a>
                </td>
                <td>${r.temperature.toFixed(1)} °C</td>
                <td>${r.heartRate} BPM</td>
                <td>${r.spo2.toFixed(1)} %</td>
                <td>${r.systolicBp}/${r.diastolicBp}</td>
                <td>${r.respiratoryRate}</td>
                <td><span class="badge ${VitalsRules.getStatusBadgeClass(r.overallStatus)}">${r.overallStatus}</span></td>
                <td style="font-size: 0.775rem; color: var(--text-muted);">${r.recordedAt || '-'}</td>
            </tr>
        `).join('');
    },

    renderAbnormalAlerts(alerts) {
        const container = document.getElementById('abnormalAlertsContainer');
        const badge = document.getElementById('abnormalCountBadge');
        badge.textContent = `${alerts.length} Active`;

        if (!alerts || alerts.length === 0) {
            container.innerHTML = '<div class="table-empty">No active abnormal alerts. All in-patients are stable.</div>';
            return;
        }

        container.innerHTML = alerts.map(a => `
            <div class="alert-banner ${a.overallStatus.toLowerCase()}" style="margin-bottom: 0.75rem;">
                <div>
                    <div style="font-weight: 700;">
                        ${this.escapeHtml(a.patientName)}: ${a.overallStatus} Status
                    </div>
                    <div style="font-size: 0.775rem; opacity: 0.9; margin-top: 0.2rem;">
                        Pulse: ${a.heartRate} BPM | SpO2: ${a.spo2}% | BP: ${a.systolicBp}/${a.diastolicBp} | Temp: ${a.temperature}°C
                    </div>
                </div>
                <button class="btn btn-secondary btn-sm" onclick="App.viewPatientHistory(${a.patientId})">
                    View History
                </button>
            </div>
        `).join('');
    },

    // ------------------------------------------------------------------------
    // IOT SENSOR SIMULATOR MODULE
    // ------------------------------------------------------------------------
    initSimulatorSliders() {
        const sliders = [
            { id: 'inputTemp', badge: 'valTemp', unit: ' °C', decimals: 1 },
            { id: 'inputHr',   badge: 'valHr',   unit: ' BPM', decimals: 0 },
            { id: 'inputSpo2', badge: 'valSpo2', unit: ' %',   decimals: 1 },
            { id: 'inputResp', badge: 'valResp', unit: ' BPM', decimals: 0 },
            { id: 'inputSys',  badge: 'valSys',  unit: ' mmHg', decimals: 0 },
            { id: 'inputDia',  badge: 'valDia',  unit: ' mmHg', decimals: 0 }
        ];

        sliders.forEach(s => {
            const el = document.getElementById(s.id);
            const valBadge = document.getElementById(s.badge);
            el.addEventListener('input', () => {
                const num = parseFloat(el.value);
                valBadge.textContent = (s.decimals === 1 ? num.toFixed(1) : Math.round(num)) + s.unit;
                this.updateSimulatorPreview();
            });
        });

        this.updateSimulatorPreview();
    },

    updateSimulatorPreview() {
        const temp = parseFloat(document.getElementById('inputTemp').value);
        const hr = parseInt(document.getElementById('inputHr').value);
        const spo2 = parseFloat(document.getElementById('inputSpo2').value);
        const resp = parseInt(document.getElementById('inputResp').value);
        const sys = parseInt(document.getElementById('inputSys').value);
        const dia = parseInt(document.getElementById('inputDia').value);

        const tempStatus = VitalsRules.evaluateTemperature(temp);
        const hrStatus = VitalsRules.evaluateHeartRate(hr);
        const spo2Status = VitalsRules.evaluateSpO2(spo2);
        const bpStatus = VitalsRules.evaluateBloodPressure(sys, dia);
        const respStatus = VitalsRules.evaluateRespiratoryRate(resp);

        const triage = VitalsRules.evaluateOverall(temp, hr, spo2, sys, dia, resp);

        // Update preview badges
        this.setBadge('badgeTemp', tempStatus);
        this.setBadge('badgeHr', hrStatus);
        this.setBadge('badgeSpo2', spo2Status);
        this.setBadge('badgeBp', bpStatus);
        this.setBadge('badgeResp', respStatus);
        this.setBadge('previewOverallBadge', triage.overall);
    },

    setBadge(elementId, status) {
        const el = document.getElementById(elementId);
        el.textContent = status;
        el.className = 'badge ' + VitalsRules.getStatusBadgeClass(status);
    },

    applySimulatorPreset(type) {
        switch (type) {
            case 'normal':
                this.setSlider('inputTemp', 'valTemp', 36.8, ' °C', 1);
                this.setSlider('inputHr',   'valHr',   72,   ' BPM', 0);
                this.setSlider('inputSpo2', 'valSpo2', 98.5, ' %',   1);
                this.setSlider('inputResp', 'valResp', 15,   ' BPM', 0);
                this.setSlider('inputSys',  'valSys',  118,  ' mmHg', 0);
                this.setSlider('inputDia',  'valDia',  76,   ' mmHg', 0);
                break;
            case 'fever':
                this.setSlider('inputTemp', 'valTemp', 38.2, ' °C', 1);
                this.setSlider('inputHr',   'valHr',   108,  ' BPM', 0);
                this.setSlider('inputSpo2', 'valSpo2', 94.0, ' %',   1);
                this.setSlider('inputResp', 'valResp', 22,   ' BPM', 0);
                this.setSlider('inputSys',  'valSys',  134,  ' mmHg', 0);
                this.setSlider('inputDia',  'valDia',  86,   ' mmHg', 0);
                break;
            case 'hypoxia':
                this.setSlider('inputTemp', 'valTemp', 37.3, ' °C', 1);
                this.setSlider('inputHr',   'valHr',   128,  ' BPM', 0);
                this.setSlider('inputSpo2', 'valSpo2', 87.5, ' %',   1);
                this.setSlider('inputResp', 'valResp', 28,   ' BPM', 0);
                this.setSlider('inputSys',  'valSys',  155,  ' mmHg', 0);
                this.setSlider('inputDia',  'valDia',  96,   ' mmHg', 0);
                break;
            case 'bradycardia':
                this.setSlider('inputTemp', 'valTemp', 35.6, ' °C', 1);
                this.setSlider('inputHr',   'valHr',   44,   ' BPM', 0);
                this.setSlider('inputSpo2', 'valSpo2', 96.0, ' %',   1);
                this.setSlider('inputResp', 'valResp', 10,   ' BPM', 0);
                this.setSlider('inputSys',  'valSys',  88,   ' mmHg', 0);
                this.setSlider('inputDia',  'valDia',  56,   ' mmHg', 0);
                break;
        }
        this.updateSimulatorPreview();
        this.showToast(`Applied Preset: ${type.toUpperCase()}`, 'info');
    },

    setSlider(id, badgeId, value, unit, decimals) {
        const slider = document.getElementById(id);
        const badge = document.getElementById(badgeId);
        slider.value = value;
        badge.textContent = (decimals === 1 ? value.toFixed(1) : value) + unit;
    },

    async handleRecordTelemetry() {
        const patientId = document.getElementById('simPatientSelect').value;
        if (!patientId) {
            this.showToast('Please select a target patient first.', 'error');
            return;
        }

        const payload = {
            patientId: parseInt(patientId),
            temperature: parseFloat(document.getElementById('inputTemp').value),
            heartRate: parseInt(document.getElementById('inputHr').value),
            spo2: parseFloat(document.getElementById('inputSpo2').value),
            respiratoryRate: parseInt(document.getElementById('inputResp').value),
            systolicBp: parseInt(document.getElementById('inputSys').value),
            diastolicBp: parseInt(document.getElementById('inputDia').value),
            sourceType: 'SIMULATED_IOT_TELEMETRY'
        };

        const btn = document.getElementById('recordReadingBtn');
        btn.disabled = true;
        btn.textContent = 'Inserting into Oracle via JDBC...';

        try {
            const res = await API.recordVitals(payload);
            this.showToast(`Logged reading #${res.data.reading.readingId} (Status: ${res.data.calculatedStatus})`, 'success');
            
            // Switch to dashboard and refresh metrics
            this.loadDashboardData();
        } catch (err) {
            this.showToast('Failed to record vitals: ' + err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = '📡 Record Sensor Reading (INSERT via JDBC)';
        }
    },

    // ------------------------------------------------------------------------
    // PATIENT MANAGEMENT (CRUD)
    // ------------------------------------------------------------------------
    async populatePatientDropdowns() {
        try {
            const res = await API.getPatients('', 1, 100);
            this.patientsCache = res.data.patients || [];

            const simSelect = document.getElementById('simPatientSelect');
            const histSelect = document.getElementById('historyPatientSelect');

            const currentSim = simSelect.value;
            const currentHist = histSelect.value;

            const optionsHtml = '<option value="">-- Choose Patient from Oracle --</option>' + 
                this.patientsCache.map(p => 
                    `<option value="${p.patientId}">#${p.patientId} - ${this.escapeHtml(p.fullName)} (${p.gender}, ${p.age}y)</option>`
                ).join('');

            simSelect.innerHTML = optionsHtml;
            histSelect.innerHTML = optionsHtml;

            if (currentSim) simSelect.value = currentSim;
            if (currentHist) histSelect.value = currentHist;

            if (!simSelect.value && this.patientsCache.length > 0) {
                simSelect.value = this.patientsCache[0].patientId;
            }
            if (!histSelect.value && this.patientsCache.length > 0) {
                histSelect.value = this.patientsCache[0].patientId;
            }
        } catch (err) {
            console.error('[PATIENT DROPDOWN ERROR]', err);
        }
    },

    async loadPatients(search = '', page = 1) {
        this.currentPatientSearch = search;
        this.currentPatientPage = page;

        try {
            const res = await API.getPatients(search, page, 10);
            const { patients, total, totalPages } = res.data;

            document.getElementById('patientCountDisplay').textContent = `Total: ${total} patients registered`;
            document.getElementById('paginationInfo').textContent = `Page ${page} of ${Math.max(1, totalPages)}`;
            document.getElementById('prevPageBtn').disabled = (page <= 1);
            document.getElementById('nextPageBtn').disabled = (page >= totalPages);

            const tbody = document.getElementById('patientsTbody');
            if (!patients || patients.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="table-empty">No matching patient records found in Oracle database.</td></tr>';
                return;
            }

            tbody.innerHTML = patients.map(p => {
                const latestStatus = p.latestReading ? p.latestReading.overallStatus : 'NO_DATA';
                return `
                    <tr>
                        <td style="font-family: var(--font-mono); font-weight: 700;">#${p.patientId}</td>
                        <td style="font-weight: 600;">${this.escapeHtml(p.fullName)}</td>
                        <td>${p.age}</td>
                        <td>${p.gender}</td>
                        <td><span class="badge badge-neutral">${p.bloodGroup}</span></td>
                        <td>${this.escapeHtml(p.contactNumber)}</td>
                        <td style="font-size: 0.8rem; color: var(--text-secondary);">${this.escapeHtml(p.email)}</td>
                        <td>
                            <span class="badge ${VitalsRules.getStatusBadgeClass(latestStatus)}">
                                ${latestStatus === 'NO_DATA' ? 'No Vitals Logged' : latestStatus}
                            </span>
                        </td>
                        <td>
                            <div style="display: flex; gap: 0.35rem;">
                                <button class="btn btn-secondary btn-sm" onclick="App.viewPatientHistory(${p.patientId})" title="View Vitals History">
                                    📈 Trends
                                </button>
                                <button class="btn btn-secondary btn-sm" onclick="App.openEditPatientModal(${p.patientId})" title="Edit Details">
                                    ✏️ Edit
                                </button>
                                <button class="btn btn-danger btn-sm" onclick="App.openDeleteModal(${p.patientId}, '${this.escapeJs(p.fullName)}')" title="Delete Patient">
                                    🗑️ Delete
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');

        } catch (err) {
            console.error('[LOAD PATIENTS ERROR]', err);
            this.showToast('Failed to retrieve patient registry: ' + err.message, 'error');
        }
    },

    searchPatients() {
        const query = document.getElementById('patientSearchInput').value.trim();
        this.loadPatients(query, 1);
    },

    resetPatientSearch() {
        document.getElementById('patientSearchInput').value = '';
        this.loadPatients('', 1);
    },

    changePatientPage(delta) {
        this.loadPatients(this.currentPatientSearch, this.currentPatientPage + delta);
    },

    openPatientModal(patient = null) {
        const modal = document.getElementById('patientModal');
        const title = document.getElementById('patientModalTitle');
        const form = document.getElementById('patientForm');

        form.reset();
        if (patient) {
            title.textContent = `Update Patient Profile (ID #${patient.patientId})`;
            document.getElementById('modalPatientId').value = patient.patientId;
            document.getElementById('modalFullName').value = patient.fullName;
            document.getElementById('modalAge').value = patient.age;
            document.getElementById('modalGender').value = patient.gender;
            document.getElementById('modalBloodGroup').value = patient.bloodGroup;
            document.getElementById('modalContact').value = patient.contactNumber;
            document.getElementById('modalEmail').value = patient.email;
            document.getElementById('modalHistory').value = patient.medicalHistory || '';
        } else {
            title.textContent = 'Register New In-Patient (INSERT)';
            document.getElementById('modalPatientId').value = '';
        }
        modal.classList.remove('hidden');
    },

    async openEditPatientModal(patientId) {
        try {
            const res = await API.getPatientById(patientId);
            this.openPatientModal(res.data);
        } catch (err) {
            this.showToast('Failed to load patient profile: ' + err.message, 'error');
        }
    },

    closePatientModal() {
        document.getElementById('patientModal').classList.add('hidden');
    },

    async handleSavePatient() {
        const id = document.getElementById('modalPatientId').value;
        const isUpdate = (id && id.trim() !== '');

        const payload = {
            fullName: document.getElementById('modalFullName').value.trim(),
            age: parseInt(document.getElementById('modalAge').value),
            gender: document.getElementById('modalGender').value,
            bloodGroup: document.getElementById('modalBloodGroup').value,
            contactNumber: document.getElementById('modalContact').value.trim(),
            email: document.getElementById('modalEmail').value.trim(),
            medicalHistory: document.getElementById('modalHistory').value.trim()
        };

        if (isUpdate) {
            payload.patientId = parseInt(id);
        }

        const btn = document.getElementById('savePatientBtn');
        btn.disabled = true;
        btn.textContent = 'Saving to Oracle...';

        try {
            if (isUpdate) {
                await API.updatePatient(payload);
                this.showToast(`Updated patient #${payload.patientId} successfully`, 'success');
            } else {
                const res = await API.createPatient(payload);
                this.showToast(`Registered patient #${res.data.patientId} in Oracle database`, 'success');
            }
            this.closePatientModal();
            this.loadPatients(this.currentPatientSearch, this.currentPatientPage);
            this.populatePatientDropdowns();
            this.loadDashboardData();
        } catch (err) {
            this.showToast('Save failed: ' + err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Save to Oracle';
        }
    },

    openDeleteModal(patientId, fullName) {
        this.deleteTargetId = patientId;
        document.getElementById('deletePatientIdDisplay').textContent = patientId;
        document.getElementById('deletePatientName').textContent = fullName;
        document.getElementById('deleteConfirmModal').classList.remove('hidden');
    },

    closeDeleteModal() {
        this.deleteTargetId = null;
        document.getElementById('deleteConfirmModal').classList.add('hidden');
    },

    async executeDeletePatient() {
        if (!this.deleteTargetId) return;

        const btn = document.getElementById('confirmDeleteBtn');
        btn.disabled = true;
        btn.textContent = 'Executing Transaction...';

        try {
            await API.deletePatient(this.deleteTargetId);
            this.showToast(`Atomic transaction committed: Patient #${this.deleteTargetId} and telemetry deleted.`, 'success');
            this.closeDeleteModal();
            this.loadPatients(this.currentPatientSearch, this.currentPatientPage);
            this.populatePatientDropdowns();
            this.loadDashboardData();
        } catch (err) {
            this.showToast('Transaction Rolled Back: ' + err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Confirm & Execute Transaction';
        }
    },

    // ------------------------------------------------------------------------
    // CLINICAL HISTORY & TREND CHARTS
    // ------------------------------------------------------------------------
    viewPatientHistory(patientId) {
        this.switchTab('historyTab');
        document.getElementById('historyPatientSelect').value = patientId;
        this.loadPatientHistory();
    },

    async loadPatientHistory() {
        const patientId = document.getElementById('historyPatientSelect').value;
        if (!patientId) return;

        const fromDate = document.getElementById('historyFromDate').value;
        const toDate = document.getElementById('historyToDate').value;

        try {
            const res = await API.getPatientHistory(patientId, fromDate, toDate);
            const { patient, history, latestReading, totalReadings } = res.data;

            // Display Profile Header
            const header = document.getElementById('historyPatientHeader');
            header.style.display = 'block';
            document.getElementById('histPatientName').textContent = patient.fullName;
            document.getElementById('histPatientMeta').textContent = 
                `Patient #${patient.patientId} • ${patient.gender}, ${patient.age} years • Blood: ${patient.bloodGroup} • Phone: ${patient.contactNumber}`;

            const statusBadge = document.getElementById('histPatientStatusBadge');
            const status = latestReading ? latestReading.overallStatus : 'NO_DATA';
            statusBadge.textContent = status;
            statusBadge.className = 'badge ' + VitalsRules.getStatusBadgeClass(status);
            document.getElementById('histTotalLogs').textContent = totalReadings;

            // Render Multi-Vital Canvas Charts
            VitalsChart.render('tempCanvas', history, 'temperature', 'Body Temperature', '#06b6d4', '°C', 34, 41);
            VitalsChart.render('hrCanvas',   history, 'heartRate',   'Heart Rate (Pulse)', '#f59e0b', 'BPM', 40, 160);
            VitalsChart.render('spo2Canvas', history, 'spo2',        'Blood Oxygen (SpO2)', '#10b981', '%', 80, 100);
            VitalsChart.render('bpCanvas',   history, 'systolicBp',  'Systolic Blood Pressure', '#3b82f6', 'mmHg', 70, 180);

            // Populate History Table
            const tbody = document.getElementById('historyTbody');
            if (!history || history.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="table-empty">No telemetry records match the specified date range.</td></tr>';
                return;
            }

            tbody.innerHTML = history.map(h => `
                <tr>
                    <td style="font-family: var(--font-mono); font-weight: 700;">#${h.readingId}</td>
                    <td style="font-size: 0.8rem; color: var(--text-secondary);">${h.recordedAt}</td>
                    <td><strong>${h.temperature.toFixed(1)}</strong> <span class="badge ${VitalsRules.getStatusBadgeClass(h.tempStatus)}">${h.tempStatus}</span></td>
                    <td><strong>${h.heartRate}</strong> <span class="badge ${VitalsRules.getStatusBadgeClass(h.hrStatus)}">${h.hrStatus}</span></td>
                    <td><strong>${h.spo2.toFixed(1)}</strong> <span class="badge ${VitalsRules.getStatusBadgeClass(h.spo2Status)}">${h.spo2Status}</span></td>
                    <td><strong>${h.systolicBp}/${h.diastolicBp}</strong> <span class="badge ${VitalsRules.getStatusBadgeClass(h.bpStatus)}">${h.bpStatus}</span></td>
                    <td><strong>${h.respiratoryRate}</strong> <span class="badge ${VitalsRules.getStatusBadgeClass(h.respStatus)}">${h.respStatus}</span></td>
                    <td><span class="badge ${VitalsRules.getStatusBadgeClass(h.overallStatus)}">${h.overallStatus}</span></td>
                    <td style="font-size: 0.75rem; color: var(--text-muted);">${h.sourceType}</td>
                </tr>
            `).join('');

        } catch (err) {
            console.error('[LOAD HISTORY ERROR]', err);
            this.showToast('Failed to load patient history: ' + err.message, 'error');
        }
    },

    resetHistoryFilter() {
        document.getElementById('historyFromDate').value = '';
        document.getElementById('historyToDate').value = '';
        this.loadPatientHistory();
    },

    // ------------------------------------------------------------------------
    // TOAST NOTIFICATIONS & HELPERS
    // ------------------------------------------------------------------------
    showToast(message, type = 'info') {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span>${type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'}</span> <div>${this.escapeHtml(message)}</div>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            setTimeout(() => toast.remove(), 250);
        }, 4000);
    },

    escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    },

    escapeJs(str) {
        if (!str) return '';
        return String(str).replace(/'/g, "\\'").replace(/"/g, '\\"');
    }
};

// Initialize Application once DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    App.init();
});
