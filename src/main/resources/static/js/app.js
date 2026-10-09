/**
 * CloudCostOptimizer Frontend Application Logic
 */

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container') || createToastContainer();
  const toast = document.createElement('div');
  toast.className = 'toast';
  
  let icon = 'ℹ️';
  let borderCol = 'rgba(56, 189, 248, 0.4)';
  if (type === 'success') {
    icon = '✅';
    borderCol = 'rgba(16, 185, 129, 0.4)';
  } else if (type === 'error') {
    icon = '⚠️';
    borderCol = 'rgba(244, 63, 94, 0.4)';
  }
  
  toast.style.borderColor = borderCol;
  toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
  container.appendChild(toast);
  
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

function createToastContainer() {
  const container = document.createElement('div');
  container.id = 'toast-container';
  container.className = 'toast-container';
  document.body.appendChild(container);
  return container;
}

function openScanModal() {
  const modal = document.getElementById('scanModal');
  if (modal) {
    modal.classList.add('show');
  }
}

function closeScanModal() {
  const modal = document.getElementById('scanModal');
  if (modal) {
    modal.classList.remove('show');
  }
}

async function triggerScanFromModal() {
  const regionSelect = document.getElementById('scanRegionSelect');
  const region = regionSelect ? regionSelect.value : 'ap-south-1';
  const btn = document.getElementById('triggerScanSubmitBtn');
  
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = 'Scanning AWS...';
  }
  
  try {
    const res = await fetch('/api/scans', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ region })
    });
    
    if (res.ok) {
      showToast('Scan initiated successfully! Refreshing dashboard...', 'success');
      setTimeout(() => {
        window.location.reload();
      }, 1500);
    } else {
      const err = await res.json();
      showToast(err.message || 'Failed to trigger scan', 'error');
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = 'Start Scan Now';
      }
    }
  } catch (ex) {
    showToast('Network error triggering scan: ' + ex.message, 'error');
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = 'Start Scan Now';
    }
  }
}

async function updateRecommendationStatus(id, newStatus) {
  try {
    const res = await fetch(`/api/recommendations/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    });
    
    if (res.ok) {
      showToast(`Recommendation #${id} marked as ${newStatus}`, 'success');
      const row = document.getElementById(`reco-row-${id}`);
      if (row) {
        const badge = row.querySelector('.reco-status-badge');
        if (badge) {
          badge.textContent = newStatus;
          badge.className = `badge badge-${newStatus.toLowerCase()} reco-status-badge`;
        }
      }
    } else {
      showToast('Failed to update recommendation status', 'error');
    }
  } catch (ex) {
    showToast('Error: ' + ex.message, 'error');
  }
}

async function markAlertAsRead(id) {
  try {
    const res = await fetch(`/api/alerts/${id}/read`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' }
    });
    
    if (res.ok) {
      showToast('Alert marked as read', 'success');
      const item = document.getElementById(`alert-item-${id}`);
      if (item) {
        item.classList.remove('unread-alert');
        item.style.opacity = '0.6';
        const readBtn = item.querySelector('.mark-read-btn');
        if (readBtn) readBtn.remove();
      }
      
      const badge = document.getElementById('navAlertBadge');
      if (badge) {
        let count = parseInt(badge.textContent, 10);
        if (!isNaN(count) && count > 1) {
          badge.textContent = count - 1;
        } else {
          badge.remove();
        }
      }
    }
  } catch (ex) {
    showToast('Error: ' + ex.message, 'error');
  }
}

async function markAllAlertsRead() {
  try {
    const res = await fetch('/api/alerts/read-all', {
      method: 'POST'
    });
    if (res.ok) {
      showToast('All alerts marked as read', 'success');
      setTimeout(() => window.location.reload(), 800);
    }
  } catch (ex) {
    showToast('Error: ' + ex.message, 'error');
  }
}

async function handleLogout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST' });
    window.location.href = '/login';
  } catch (ex) {
    window.location.href = '/login';
  }
}

function filterTable(inputId, tableId) {
  const input = document.getElementById(inputId);
  if (!input) return;
  const filter = input.value.toLowerCase();
  const table = document.getElementById(tableId);
  if (!table) return;
  const rows = table.getElementsByTagName('tr');
  
  for (let i = 1; i < rows.length; i++) {
    const text = rows[i].textContent.toLowerCase();
    rows[i].style.display = text.includes(filter) ? '' : 'none';
  }
}
