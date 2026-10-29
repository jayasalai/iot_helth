/**
 * Clinical Decision Rules Module (Client-Side Mirror).
 * Evaluates simulated IoT sensor readings instantly as sliders are moved,
 * providing interactive visual feedback matching the Java VitalsClinicalRules engine.
 */
const VitalsRules = {
    STATUS_NORMAL: 'NORMAL',
    STATUS_WARNING: 'WARNING',
    STATUS_CRITICAL: 'CRITICAL',

    evaluateTemperature(temp) {
        if (temp < 35.0 || temp > 38.5) return this.STATUS_CRITICAL;
        if (temp < 36.1 || temp > 37.5) return this.STATUS_WARNING;
        return this.STATUS_NORMAL;
    },

    evaluateHeartRate(hr) {
        if (hr < 50 || hr > 120) return this.STATUS_CRITICAL;
        if (hr < 60 || hr > 100) return this.STATUS_WARNING;
        return this.STATUS_NORMAL;
    },

    evaluateSpO2(spo2) {
        if (spo2 < 90.0) return this.STATUS_CRITICAL;
        if (spo2 < 95.0) return this.STATUS_WARNING;
        return this.STATUS_NORMAL;
    },

    evaluateBloodPressure(systolic, diastolic) {
        if (systolic >= 140 || systolic < 85 || diastolic >= 90 || diastolic < 55) {
            return this.STATUS_CRITICAL;
        }
        if (systolic > 120 || diastolic > 80) {
            return this.STATUS_WARNING;
        }
        return this.STATUS_NORMAL;
    },

    evaluateRespiratoryRate(resp) {
        if (resp < 9 || resp > 24) return this.STATUS_CRITICAL;
        if (resp < 12 || resp > 20) return this.STATUS_WARNING;
        return this.STATUS_NORMAL;
    },

    evaluateOverall(temp, hr, spo2, systolic, diastolic, resp) {
        const statuses = [
            this.evaluateTemperature(temp),
            this.evaluateHeartRate(hr),
            this.evaluateSpO2(spo2),
            this.evaluateBloodPressure(systolic, diastolic),
            this.evaluateRespiratoryRate(resp)
        ];

        if (statuses.includes(this.STATUS_CRITICAL)) {
            return {
                overall: this.STATUS_CRITICAL,
                breakdown: statuses
            };
        }
        if (statuses.includes(this.STATUS_WARNING)) {
            return {
                overall: this.STATUS_WARNING,
                breakdown: statuses
            };
        }
        return {
            overall: this.STATUS_NORMAL,
            breakdown: statuses
        };
    },

    getStatusBadgeClass(status) {
        switch ((status || '').toUpperCase()) {
            case 'CRITICAL': return 'badge-critical';
            case 'WARNING':  return 'badge-warning';
            case 'NORMAL':   return 'badge-normal';
            default:         return 'badge-neutral';
        }
    }
};
