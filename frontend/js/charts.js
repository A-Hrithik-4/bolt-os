/**
 * BOLT — Charts & Visualizations Engine
 * Lightweight, zero-dependency, retina-sharp Canvas charting engine
 * Designed with Apple macOS Financial Analytics aesthetic and Portfolio Color Identity.
 */

const BoltCharts = {
  // Financial Overview Smooth Spline Multi-Area Chart
  renderFinancialOverview(canvasId, timeData) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;

    // Retina display scaling
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    const width = rect.width;
    const height = rect.height;
    const padding = { top: 30, right: 30, bottom: 40, left: 60 };

    ctx.clearRect(0, 0, width, height);

    // Sample series data points if not provided
    const labels = ['09:00', '11:00', '13:00', '15:00', '17:00', '19:00', '21:00'];
    const deposits = [4000, 7500, 5000, 11000, 8500, 14000, 12500];
    const withdrawals = [2000, 4500, 3200, 6000, 4800, 7500, 6800];
    const transfers = [1000, 2500, 4000, 3000, 6200, 4500, 5000];

    const chartW = width - padding.left - padding.right;
    const chartH = height - padding.top - padding.bottom;
    const maxVal = 16000;

    // Subtle horizontal gridlines
    const gridLines = 4;
    ctx.strokeStyle = 'rgba(148, 163, 184, 0.25)';
    ctx.lineWidth = 1;
    ctx.font = '11px JetBrains Mono, monospace';
    ctx.fillStyle = '#64748B';
    ctx.textAlign = 'right';

    for (let i = 0; i <= gridLines; i++) {
      const y = padding.top + (chartH / gridLines) * i;
      const val = Math.round(maxVal - (maxVal / gridLines) * i);
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();
      ctx.fillText('₹' + (val >= 1000 ? (val / 1000) + 'k' : val), padding.left - 10, y + 4);
    }

    // X-Axis Labels
    ctx.textAlign = 'center';
    const stepX = chartW / (labels.length - 1);
    labels.forEach((lbl, i) => {
      const x = padding.left + i * stepX;
      ctx.fillText(lbl, x, height - 12);
    });

    // Draw Smooth Area Curve helper
    function drawSeries(points, strokeColor, fillColor, glowColor) {
      if (points.length < 2) return;
      const coords = points.map((p, i) => ({
        x: padding.left + i * stepX,
        y: padding.top + chartH - (p / maxVal) * chartH
      }));

      // Draw Gradient Fill
      ctx.beginPath();
      ctx.moveTo(coords[0].x, padding.top + chartH);
      ctx.lineTo(coords[0].x, coords[0].y);

      for (let i = 0; i < coords.length - 1; i++) {
        const cpX = (coords[i].x + coords[i + 1].x) / 2;
        ctx.bezierCurveTo(cpX, coords[i].y, cpX, coords[i + 1].y, coords[i + 1].x, coords[i + 1].y);
      }

      ctx.lineTo(coords[coords.length - 1].x, padding.top + chartH);
      ctx.closePath();
      ctx.fillStyle = fillColor;
      ctx.fill();

      // Draw Stroke Line
      ctx.beginPath();
      ctx.moveTo(coords[0].x, coords[0].y);
      for (let i = 0; i < coords.length - 1; i++) {
        const cpX = (coords[i].x + coords[i + 1].x) / 2;
        ctx.bezierCurveTo(cpX, coords[i].y, cpX, coords[i + 1].y, coords[i + 1].x, coords[i + 1].y);
      }
      ctx.strokeStyle = strokeColor;
      ctx.lineWidth = 2.5;
      ctx.shadowColor = glowColor;
      ctx.shadowBlur = 10;
      ctx.stroke();
      ctx.shadowBlur = 0;

      // Draw Data Point Dots
      coords.forEach(pt => {
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2);
        ctx.fillStyle = '#FFFFFF';
        ctx.fill();
        ctx.strokeStyle = strokeColor;
        ctx.lineWidth = 2;
        ctx.stroke();
      });
    }

    // 1. Transfers Series (Electric Arctic Cyan)
    const transferGrad = ctx.createLinearGradient(0, padding.top, 0, height);
    transferGrad.addColorStop(0, 'rgba(6, 182, 212, 0.22)');
    transferGrad.addColorStop(1, 'rgba(6, 182, 212, 0.00)');
    drawSeries(transfers, '#06B6D4', transferGrad, 'rgba(6, 182, 212, 0.45)');

    // 2. Withdrawals Series (Coral / Amber)
    const withdrawGrad = ctx.createLinearGradient(0, padding.top, 0, height);
    withdrawGrad.addColorStop(0, 'rgba(239, 68, 68, 0.20)');
    withdrawGrad.addColorStop(1, 'rgba(239, 68, 68, 0.00)');
    drawSeries(withdrawals, '#EF4444', withdrawGrad, 'rgba(239, 68, 68, 0.4)');

    // 3. Deposits Series (Emerald Green)
    const depositGrad = ctx.createLinearGradient(0, padding.top, 0, height);
    depositGrad.addColorStop(0, 'rgba(16, 185, 129, 0.25)');
    depositGrad.addColorStop(1, 'rgba(16, 185, 129, 0.00)');
    drawSeries(deposits, '#10B981', depositGrad, 'rgba(16, 185, 129, 0.5)');
  },

  // Radial Gauge for Thread Pool Capacity
  renderThreadGauge(canvasId, activeCount = 2, totalCapacity = 8) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;

    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    const w = rect.width;
    const h = rect.height;
    const centerX = w / 2;
    const centerY = h / 2;
    const radius = Math.min(w, h) / 2 - 12;

    ctx.clearRect(0, 0, w, h);

    // Background track ring
    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, 0, Math.PI * 2);
    ctx.strokeStyle = 'rgba(148, 163, 184, 0.28)';
    ctx.lineWidth = 10;
    ctx.stroke();

    // Active usage arc
    const pct = Math.min(activeCount / totalCapacity, 1);
    const startAngle = -Math.PI / 2;
    const endAngle = startAngle + Math.PI * 2 * pct;

    const grad = ctx.createLinearGradient(0, 0, w, h);
    grad.addColorStop(0, '#0E7490');
    grad.addColorStop(1, '#06B6D4');

    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, startAngle, endAngle);
    ctx.strokeStyle = grad;
    ctx.lineWidth = 10;
    ctx.lineCap = 'round';
    ctx.shadowColor = 'rgba(6, 182, 212, 0.40)';
    ctx.shadowBlur = 8;
    ctx.stroke();
    ctx.shadowBlur = 0;

    // Center text
    ctx.font = '700 22px Space Grotesk, sans-serif';
    ctx.fillStyle = '#0F172A';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(`${activeCount}/${totalCapacity}`, centerX, centerY - 4);

    ctx.font = '500 11px Inter, sans-serif';
    ctx.fillStyle = '#64748B';
    ctx.fillText('ACTIVE', centerX, centerY + 16);
  }
};
