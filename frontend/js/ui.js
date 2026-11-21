/**
 * BOLT — UI Interactions & Modal Manager
 * Manages Apple Spring Modals, Toast Notifications,
 * Form submissions for Deposit/Withdraw/Transfer, and Mode Toggling.
 */

// ============================================================
// 1. TOAST NOTIFICATION SYSTEM
// ============================================================
function showToast(message, type = 'info') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let iconSvg = '';
  if (type === 'success') {
    iconSvg = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`;
  } else if (type === 'error') {
    iconSvg = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#EF4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`;
  } else {
    iconSvg = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#06B6D4" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
  }

  toast.innerHTML = `
    ${iconSvg}
    <div class="toast-message">${message}</div>
    <button class="toast-close-btn" style="opacity: 0.6; padding: 4px;" onclick="this.parentElement.remove()">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
    </button>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px) scale(0.95)';
    setTimeout(() => toast.remove(), 250);
  }, 4000);
}

// ============================================================
// 2. MODAL CONTROLLER
// ============================================================
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  modal.classList.add('active');
  document.body.style.overflow = 'hidden';

  // Focus first input if available
  const firstInput = modal.querySelector('input:not([type="hidden"]), select');
  if (firstInput) setTimeout(() => firstInput.focus(), 100);
}

function closeModal(modalId) {
  const modal = typeof modalId === 'string' ? document.getElementById(modalId) : modalId.closest('.modal-overlay');
  if (!modal) return;
  modal.classList.remove('active');
  document.body.style.overflow = '';
}

// Setup global modal closing on backdrop & ESC key
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.modal-overlay').forEach(modal => {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) closeModal(modal);
    });
  });

  document.querySelectorAll('.modal-close-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const modal = btn.closest('.modal-overlay');
      if (modal) closeModal(modal);
    });
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      const activeModal = document.querySelector('.modal-overlay.active');
      if (activeModal) closeModal(activeModal);
    }
  });

  initModeToggles();
  initFormHandlers();
});

// ============================================================
// 3. EXECUTION MODE TOGGLE CONTROLLER
// ============================================================
function initModeToggles() {
  const toggleButtons = document.querySelectorAll('.mode-toggle-btn');
  const indicatorPills = document.querySelectorAll('.mode-indicator-pill');
  const appleToggleSwitches = document.querySelectorAll('.mode-toggle-switch-wrapper');

  updateModeUI(BoltState.currentMode);

  toggleButtons.forEach(btn => {
    btn.addEventListener('click', async () => {
      const targetMode = btn.dataset.mode;
      if (!targetMode || targetMode === BoltState.currentMode) return;

      try {
        await BoltApi.setExecutionMode(targetMode);
        updateModeUI(targetMode);
        if (targetMode === 'SAFE') {
          showToast('✅ SAFE Mode Enabled — Mutual Exclusion & ReentrantLock Active', 'success');
        } else {
          showToast('⚠️ UNSAFE Mode Enabled — Synchronization OFF (Race Conditions Active)', 'error');
        }
      } catch (e) {
        showToast(`Failed to switch mode: ${e.message}`, 'error');
      }
    });
  });

  const handleModeToggle = async () => {
    const nextMode = BoltState.currentMode === 'SAFE' ? 'UNSAFE' : 'SAFE';
    try {
      await BoltApi.setExecutionMode(nextMode);
      updateModeUI(nextMode);
      if (nextMode === 'SAFE') {
        showToast('✅ SAFE Mode Enabled — ReentrantLock Active', 'success');
      } else {
        showToast('⚠️ UNSAFE Mode Enabled — Synchronization OFF (Race Conditions Active)', 'error');
      }
    } catch (e) {
      showToast(`Failed to switch mode: ${e.message}`, 'error');
    }
  };

  indicatorPills.forEach(pill => {
    pill.addEventListener('click', handleModeToggle);
  });

  appleToggleSwitches.forEach(sw => {
    sw.addEventListener('click', handleModeToggle);
  });
}

function updateModeUI(mode) {
  // Update toggle buttons in dashboard hero if any exist
  document.querySelectorAll('.mode-toggle-btn').forEach(btn => {
    if (btn.dataset.mode === mode) {
      btn.classList.add('active');
      btn.classList.toggle('safe-active', mode === 'SAFE');
      btn.classList.toggle('unsafe-active', mode === 'UNSAFE');
    } else {
      btn.classList.remove('active', 'safe-active', 'unsafe-active');
    }
  });

  // Update topbar pills if any exist
  document.querySelectorAll('.mode-indicator-pill').forEach(pill => {
    pill.className = `mode-indicator-pill ${mode.toLowerCase()}`;
    pill.innerHTML = `
      <span class="mode-indicator-dot"></span>
      <span class="mode-text">${mode} MODE</span>
    `;
  });

  // Update Apple toggle switches
  document.querySelectorAll('.mode-toggle-switch-wrapper').forEach(wrapper => {
    const label = wrapper.querySelector('.mode-toggle-label');
    const sw = wrapper.querySelector('.apple-switch');
    if (label) {
      label.className = `mode-toggle-label ${mode.toLowerCase()}`;
      label.textContent = `${mode} MODE`;
    }
    if (sw) {
      sw.className = `apple-switch ${mode.toLowerCase()}`;
    }
  });
}

// ============================================================
// 4. TRANSACTION FORM SUBMISSION HANDLERS
// ============================================================
function initFormHandlers() {
  // Deposit Form
  const depositForm = document.getElementById('depositForm');
  if (depositForm) {
    depositForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const account = document.getElementById('depositAccount').value;
      const amount = document.getElementById('depositAmount').value;
      const channelEl = document.getElementById('depositChannel');
      const channel = channelEl ? channelEl.value : 'ONLINE';

      try {
        const txn = await BoltApi.deposit(account, amount, channel);
        closeModal('depositModal');
        depositForm.reset();
        showToast(`Deposit of ₹${amount} completed (ID: ${txn.transactionId})`, 'success');
        refreshPageData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // Withdrawal Form
  const withdrawForm = document.getElementById('withdrawForm');
  if (withdrawForm) {
    withdrawForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const account = document.getElementById('withdrawAccount').value;
      const amount = document.getElementById('withdrawAmount').value;
      const channelEl = document.getElementById('withdrawChannel');
      const channel = channelEl ? channelEl.value : 'ATM';

      try {
        const txn = await BoltApi.withdraw(account, amount, channel);
        closeModal('withdrawModal');
        withdrawForm.reset();
        showToast(`Withdrawal of ₹${amount} completed (ID: ${txn.transactionId})`, 'success');
        refreshPageData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // Transfer Form
  const transferForm = document.getElementById('transferForm');
  if (transferForm) {
    transferForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const source = document.getElementById('transferSource').value;
      const dest = document.getElementById('transferDest').value;
      const amount = document.getElementById('transferAmount').value;
      const channelEl = document.getElementById('transferChannel');
      const channel = channelEl ? channelEl.value : 'ONLINE';

      try {
        const txn = await BoltApi.transfer(source, dest, amount, channel);
        closeModal('transferModal');
        transferForm.reset();
        showToast(`Transfer of ₹${amount} completed (ID: ${txn.transactionId})`, 'success');
        refreshPageData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // Create Account Form
  const createAccountForm = document.getElementById('createAccountForm');
  if (createAccountForm) {
    createAccountForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const accountNumber = document.getElementById('newAccNumber').value;
      const customerName = document.getElementById('newCustomerName').value;
      const initialBalance = document.getElementById('newInitialBalance').value;

      try {
        const acc = await BoltApi.createAccount(accountNumber, customerName, initialBalance);
        closeModal('createAccountModal');
        createAccountForm.reset();
        showToast(`Account #${acc.accountNumber} created for ${acc.customerName}`, 'success');
        refreshPageData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }
}

function refreshPageData() {
  if (typeof loadDashboardData === 'function') loadDashboardData();
  if (typeof loadAccountsData === 'function') loadAccountsData();
  if (typeof loadTransactionsData === 'function') loadTransactionsData();
}
