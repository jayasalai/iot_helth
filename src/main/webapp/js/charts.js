/**
 * Self-Contained HTML5 Canvas Telemetry Charting Library.
 * Renders smooth multi-vital time-series graphs for Temperature, Heart Rate, SpO2, and Blood Pressure
 * without any external CDN dependencies (critical for offline lab exams).
 */
const VitalsChart = {
    render(canvasId, readings, metricKey, label, color, unit, minVal, maxVal) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const width = canvas.width = canvas.parentElement.clientWidth || 500;
        const height = canvas.height = 180;

        ctx.clearRect(0, 0, width, height);

        if (!readings || readings.length === 0) {
            ctx.fillStyle = '#64748b';
            ctx.font = '13px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('No telemetry records available for this period', width / 2, height / 2);
            return;
        }

        // Sort chronologically ascending for the chart
        const sorted = [...readings].reverse();
        const padding = { top: 25, right: 20, bottom: 25, left: 45 };
        const plotWidth = width - padding.left - padding.right;
        const plotHeight = height - padding.top - padding.bottom;

        // Determine min and max scale
        let values = sorted.map(r => r[metricKey]).filter(v => v !== undefined && v !== null);
        if (values.length === 0) return;

        let yMin = minVal !== undefined ? minVal : Math.floor(Math.min(...values) * 0.95);
        let yMax = maxVal !== undefined ? maxVal : Math.ceil(Math.max(...values) * 1.05);
        if (yMax === yMin) { yMax += 1; yMin -= 1; }

        // Draw horizontal grid lines & labels
        ctx.strokeStyle = '#1e293b';
        ctx.fillStyle = '#64748b';
        ctx.font = '10px monospace';
        ctx.lineWidth = 1;

        const gridSteps = 4;
        for (let i = 0; i <= gridSteps; i++) {
            const val = yMin + (i * (yMax - yMin) / gridSteps);
            const y = padding.top + plotHeight - (i * plotHeight / gridSteps);

            ctx.beginPath();
            ctx.moveTo(padding.left, y);
            ctx.lineTo(width - padding.right, y);
            ctx.stroke();

            ctx.textAlign = 'right';
            ctx.fillText(val.toFixed(metricKey === 'temperature' ? 1 : 0), padding.left - 8, y + 3);
        }

        // Compute point coordinates
        const points = sorted.map((r, idx) => {
            const x = padding.left + (idx * plotWidth / Math.max(1, sorted.length - 1));
            const y = padding.top + plotHeight - ((r[metricKey] - yMin) / (yMax - yMin) * plotHeight);
            return { x, y, val: r[metricKey], time: r.recordedAt };
        });

        // Fill area under line
        const gradient = ctx.createLinearGradient(0, padding.top, 0, height - padding.bottom);
        gradient.addColorStop(0, color.replace('rgb', 'rgba').replace(')', ', 0.3)'));
        gradient.addColorStop(1, 'rgba(10, 15, 29, 0)');

        ctx.beginPath();
        ctx.moveTo(points[0].x, height - padding.bottom);
        points.forEach(p => ctx.lineTo(p.x, p.y));
        ctx.lineTo(points[points.length - 1].x, height - padding.bottom);
        ctx.closePath();
        ctx.fillStyle = gradient;
        ctx.fill();

        // Draw line graph
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth = 2.5;
        ctx.lineJoin = 'round';
        points.forEach((p, idx) => {
            if (idx === 0) ctx.moveTo(p.x, p.y);
            else ctx.lineTo(p.x, p.y);
        });
        ctx.stroke();

        // Draw points with outer halos
        points.forEach(p => {
            ctx.beginPath();
            ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
            ctx.fillStyle = color;
            ctx.fill();
            ctx.strokeStyle = '#0a0f1d';
            ctx.lineWidth = 2;
            ctx.stroke();
        });

        // Title and latest value indicator
        const latest = points[points.length - 1];
        ctx.fillStyle = '#f8fafc';
        ctx.font = 'bold 11px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(`${label}: ${latest.val} ${unit}`, padding.left, 16);
    }
};
