/**
 * Clean & Simple Chart.js configurations for CloudCostOptimizer
 */

function initCostBreakdownChart(canvasId, costData) {
  const ctx = document.getElementById(canvasId);
  if (!ctx || typeof Chart === 'undefined') return;

  const labels = Object.keys(costData || {});
  const values = Object.values(costData || {});

  new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels.length ? labels : ['No Data'],
      datasets: [{
        label: 'Monthly Cost (USD)',
        data: values.length ? values : [0],
        backgroundColor: [
          '#2563eb', // Blue
          '#0d9488', // Teal
          '#6366f1', // Indigo
          '#059669'  // Emerald
        ],
        borderRadius: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#0f172a',
          titleColor: '#ffffff',
          bodyColor: '#cbd5e1',
          padding: 10,
          cornerRadius: 6,
          callbacks: {
            label: function(context) {
              return ` $${Number(context.raw).toFixed(2)} / month`;
            }
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: '#64748b', font: { family: 'Inter', size: 12 } }
        },
        y: {
          grid: { color: '#f1f5f9' },
          ticks: {
            color: '#64748b',
            font: { family: 'Inter', size: 12 },
            callback: function(value) { return '$' + value; }
          }
        }
      }
    }
  });
}

function initResourceDistributionChart(canvasId, resourceCounts) {
  const ctx = document.getElementById(canvasId);
  if (!ctx || typeof Chart === 'undefined') return;

  const labels = Object.keys(resourceCounts || {});
  const values = Object.values(resourceCounts || {});

  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: labels.length ? labels : ['None'],
      datasets: [{
        data: values.length ? values : [1],
        backgroundColor: [
          '#2563eb', // EC2 - Blue
          '#0d9488', // EBS - Teal
          '#6366f1', // EIP - Indigo
          '#059669', // S3 - Emerald
          '#d97706'  // IAM - Amber
        ],
        borderColor: '#ffffff',
        borderWidth: 2,
        hoverOffset: 4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '70%',
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: '#475569',
            font: { family: 'Inter', size: 12 },
            boxWidth: 10,
            padding: 12
          }
        },
        tooltip: {
          backgroundColor: '#0f172a',
          titleColor: '#ffffff',
          bodyColor: '#cbd5e1',
          padding: 10,
          cornerRadius: 6
        }
      }
    }
  });
}
