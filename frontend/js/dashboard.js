/**
 * BOLT — Dashboard Controller
 * Binds real-time backend / simulated metrics to dashboard UI widgets,
 * transactions table, thread pool status, and financial overview chart.
 */

document.addEventListener('DOMContentLoaded', async () => {
  await loadDashboardData();

  // Responsive chart re-render on resize
  window.addEventListener('resize', () => {
    BoltCharts.renderFinancialOverview('financialChart');
    BoltCharts.renderThreadGauge('threadGauge', BoltState.activeThreads || 0, 8);
  });
});

async function loadDashboardData() {
  try {
    const summary = await BoltApi.getDashboardSummary();
    const transactions = await BoltApi.getTransactions();
    const accounts = await BoltApi.getAccounts();

    // 1. Update Metrics Cards
    updateStatCard('totalAccountsVal', summary.totalAccounts || accounts.length);
    updateStatCard('totalTxnsVal', summary.totalTransactions || transactions.length);
    updateStatCard('successTxnsVal', summary.successfulTransactions || transactions.filter(t => t.status === 'SUCCESS').length);
    updateStatCard('failedTxnsVal', summary.failedTransactions || transactions.filter(t => t.status === 'FAILED').length);

    // 2. Render Financial Overview Chart
    BoltCharts.renderFinancialOverview('financialChart');

    // 3. Render Thread Pool Gauge & Status
    const active = summary.activeThreadCount || 0;
    const completed = summary.completedThreadCount || transactions.length;
    BoltCharts.renderThreadGauge('threadGauge', active, 8);
    const activeEl = document.getElementById('activeThreadCount');
    const completedEl = document.getElementById('completedThreadCount');
    if (activeEl) activeEl.textContent = active;
    if (completedEl) completedEl.textContent = completed;

    // 4. Render Recent Transactions Table
    renderRecentTransactions(transactions.slice(0, 6));

  } catch (err) {
    console.error('Error loading dashboard data:', err);
  }
}

function updateStatCard(id, value) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = value;
}

function renderRecentTransactions(transactions) {
  const tbody = document.getElementById('recentTransactionsBody');
  if (!tbody) return;

  if (!transactions || transactions.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 30px;">No recent transactions recorded</td></tr>`;
    return;
  }

  tbody.innerHTML = transactions.map(txn => {
    const isSafe = txn.executionMode === 'SAFE';
    const isSuccess = txn.status === 'SUCCESS';

    return `
      <tr>
        <td class="mono" style="font-weight: 600; color: var(--c-sapphire);">${txn.transactionId}</td>
        <td>
          <span class="badge badge-type">${txn.transactionType}</span>
        </td>
        <td class="mono">${txn.sourceAccountNumber || '--'} ${txn.destinationAccountNumber ? '→ ' + txn.destinationAccountNumber : ''}</td>
        <td style="font-weight: 600; color: var(--text-heading); font-family: 'JetBrains Mono', monospace;">${formatCurrency(txn.amount)}</td>
        <td>
          <span class="badge ${isSafe ? 'badge-safe' : 'badge-unsafe'}">
            <span class="badge-dot"></span>
            ${txn.executionMode}
          </span>
        </td>
        <td>
          <span class="badge ${isSuccess ? 'badge-success' : 'badge-failed'}">
            <span class="badge-dot"></span>
            ${txn.status}
          </span>
        </td>
        <td style="color: var(--text-muted); font-size: 12px;">${formatDate(txn.timestamp)}</td>
      </tr>
    `;
  }).join('');
}
