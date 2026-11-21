/**
 * BOLT — Banking Operations & Locking Technology
 * Core Application Engine & API Client
 * Seamlessly connects to Spring Boot Backend (http://localhost:8080/api)
 * with graceful fallback to realistic data if the server is offline.
 */

const BOLT_CONFIG = {
  API_BASE: 'http://localhost:8080/api',
  DEFAULT_MODE: 'SAFE',
  CURRENCY_SYMBOL: '₹',
  POLL_INTERVAL_MS: 4000
};

// Global Application State Store
const BoltState = {
  currentMode: 'SAFE',
  isBackendConnected: false,
  accounts: [],
  transactions: [],
  summary: null,
  activeThreads: 0,
  completedThreads: 0,
  listeners: []
};

// Subscribable State Change Listener
function subscribeToState(fn) {
  BoltState.listeners.push(fn);
}

function notifyStateChanged() {
  BoltState.listeners.forEach(fn => {
    try { fn(BoltState); } catch (e) { console.error('Listener error:', e); }
  });
}

// ============================================================
// REALISTIC SAMPLE/MOCK DATA (Used when backend is offline)
// ============================================================
const MockData = {
  accounts: [
    { id: 1, accountNumber: "1001", customerName: "Alice", balance: 10000.00, accountStatus: "ACTIVE", version: 0 },
    { id: 2, accountNumber: "1002", customerName: "Bob", balance: 15000.00, accountStatus: "ACTIVE", version: 0 },
    { id: 3, accountNumber: "1003", customerName: "Charlie", balance: 20000.00, accountStatus: "ACTIVE", version: 0 },
    { id: 4, accountNumber: "1004", customerName: "David", balance: 12000.00, accountStatus: "ACTIVE", version: 0 }
  ],
  transactions: [
    {
      transactionId: "TXN-000001",
      transactionType: "DEPOSIT",
      sourceAccountNumber: "1001",
      destinationAccountNumber: null,
      amount: 2500.00,
      executionMode: "SAFE",
      threadName: "ATM-THREAD-1",
      channel: "ATM",
      status: "SUCCESS",
      timestamp: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
      durationMs: 4,
      description: "Deposit of ₹2500.00 to account 1001 completed"
    },
    {
      transactionId: "TXN-000002",
      transactionType: "WITHDRAWAL",
      sourceAccountNumber: "1002",
      destinationAccountNumber: null,
      amount: 1500.00,
      executionMode: "SAFE",
      threadName: "ONLINE-THREAD-1",
      channel: "ONLINE",
      status: "SUCCESS",
      timestamp: new Date(Date.now() - 1000 * 60 * 32).toISOString(),
      durationMs: 6,
      description: "Withdrawal of ₹1500.00 from account 1002 completed"
    },
    {
      transactionId: "TXN-000003",
      transactionType: "TRANSFER",
      sourceAccountNumber: "1003",
      destinationAccountNumber: "1001",
      amount: 3000.00,
      executionMode: "SAFE",
      threadName: "ONLINE-THREAD-2",
      channel: "ONLINE",
      status: "SUCCESS",
      timestamp: new Date(Date.now() - 1000 * 60 * 58).toISOString(),
      durationMs: 8,
      description: "Transfer of ₹3000.00 from 1003 to 1001 completed"
    },
    {
      transactionId: "TXN-000004",
      transactionType: "WITHDRAWAL",
      sourceAccountNumber: "RACE-DEMO",
      destinationAccountNumber: null,
      amount: 6000.00,
      executionMode: "UNSAFE",
      threadName: "ATM-THREAD-2",
      channel: "ATM",
      status: "SUCCESS",
      timestamp: new Date(Date.now() - 1000 * 60 * 85).toISOString(),
      durationMs: 52,
      description: "Race condition withdrawal demo — lost update demonstrated"
    }
  ],
  summary: {
    totalAccounts: 4,
    totalTransactions: 4,
    successfulTransactions: 4,
    failedTransactions: 0,
    totalDeposits: 1,
    totalWithdrawals: 2,
    totalTransfers: 1,
    totalDepositAmount: 2500.00,
    totalWithdrawalAmount: 7500.00,
    totalTransferAmount: 3000.00,
    currentMode: "SAFE",
    activeThreadCount: 0,
    completedThreadCount: 4,
    averageTransactionDurationMs: 17.5,
    safeVsUnsafeStats: {
      safeTransactions: 3,
      unsafeTransactions: 1,
      safeSuccessful: 3,
      unsafeSuccessful: 1,
      safeFailed: 0,
      unsafeFailed: 0,
      safeAvgDurationMs: 6.0,
      unsafeAvgDurationMs: 52.0
    }
  }
};

// ============================================================
// API CLIENT LAYER
// ============================================================
const BoltApi = {
  // Test connection to backend
  async checkConnection() {
    try {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/mode`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' },
        signal: AbortSignal.timeout(2000)
      });
      if (res.ok) {
        const data = await res.json();
        BoltState.isBackendConnected = true;
        BoltState.currentMode = data.mode || 'SAFE';
        return true;
      }
    } catch (e) {
      // Backend offline, fallback to standalone simulation mode
    }
    BoltState.isBackendConnected = false;
    return false;
  },

  // GET Accounts
  async getAccounts() {
    if (BoltState.isBackendConnected) {
      try {
        const res = await fetch(`${BOLT_CONFIG.API_BASE}/accounts`);
        if (res.ok) return await res.json();
      } catch (e) { console.warn('Falling back to local account data', e); }
    }
    return [...MockData.accounts];
  },

  // GET Single Account
  async getAccount(accountId) {
    if (BoltState.isBackendConnected) {
      try {
        const res = await fetch(`${BOLT_CONFIG.API_BASE}/accounts/${accountId}`);
        if (res.ok) return await res.json();
      } catch (e) { console.warn('Falling back to local account details', e); }
    }
    return MockData.accounts.find(a => a.accountNumber === accountId || a.id == accountId) || MockData.accounts[0];
  },

  // GET Dashboard Summary
  async getDashboardSummary() {
    if (BoltState.isBackendConnected) {
      try {
        const res = await fetch(`${BOLT_CONFIG.API_BASE}/dashboard/summary`);
        if (res.ok) {
          const data = await res.json();
          BoltState.summary = data;
          BoltState.currentMode = data.currentMode || BoltState.currentMode;
          return data;
        }
      } catch (e) { console.warn('Falling back to local dashboard summary', e); }
    }
    return MockData.summary;
  },

  // GET Transactions
  async getTransactions() {
    if (BoltState.isBackendConnected) {
      try {
        const res = await fetch(`${BOLT_CONFIG.API_BASE}/transactions`);
        if (res.ok) return await res.json();
      } catch (e) { console.warn('Falling back to local transactions', e); }
    }
    return [...MockData.transactions];
  },

  // GET Execution Mode
  async getExecutionMode() {
    if (BoltState.isBackendConnected) {
      try {
        const res = await fetch(`${BOLT_CONFIG.API_BASE}/mode`);
        if (res.ok) {
          const data = await res.json();
          BoltState.currentMode = data.mode;
          return data.mode;
        }
      } catch (e) {}
    }
    return BoltState.currentMode;
  },

  // PUT Execution Mode (SAFE / UNSAFE)
  async setExecutionMode(mode) {
    BoltState.currentMode = mode;
    if (BoltState.isBackendConnected) {
      try {
        const res = await fetch(`${BOLT_CONFIG.API_BASE}/mode`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ mode: mode })
        });
        if (res.ok) {
          const data = await res.json();
          notifyStateChanged();
          return data;
        }
      } catch (e) { console.warn('Mode switched locally', e); }
    }
    notifyStateChanged();
    return { mode: mode, message: `Execution mode changed to ${mode}` };
  },

  // POST Deposit
  async deposit(accountNumber, amount, channel = 'ONLINE') {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/transactions/deposit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountNumber, amount: parseFloat(amount), channel })
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Deposit failed');
      }
      return await res.json();
    }
    // Offline simulation
    const account = MockData.accounts.find(a => a.accountNumber === accountNumber);
    if (!account) throw new Error(`Account ${accountNumber} not found`);
    account.balance += parseFloat(amount);
    const txn = {
      transactionId: `TXN-${String(MockData.transactions.length + 1).padStart(6, '0')}`,
      transactionType: 'DEPOSIT',
      sourceAccountNumber: accountNumber,
      destinationAccountNumber: null,
      amount: parseFloat(amount),
      executionMode: BoltState.currentMode,
      threadName: `${channel}-THREAD-1`,
      channel: channel,
      status: 'SUCCESS',
      timestamp: new Date().toISOString(),
      durationMs: Math.floor(Math.random() * 5) + 1,
      description: `Deposit of ₹${amount} to account ${accountNumber} completed`
    };
    MockData.transactions.unshift(txn);
    return txn;
  },

  // POST Withdrawal
  async withdraw(accountNumber, amount, channel = 'ONLINE') {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/transactions/withdraw`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountNumber, amount: parseFloat(amount), channel })
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Withdrawal failed');
      }
      return await res.json();
    }
    // Offline simulation
    const account = MockData.accounts.find(a => a.accountNumber === accountNumber);
    if (!account) throw new Error(`Account ${accountNumber} not found`);
    if (BoltState.currentMode === 'SAFE' && account.balance < parseFloat(amount)) {
      throw new Error(`Insufficient balance: requested ₹${amount}, available ₹${account.balance.toFixed(2)}`);
    }
    account.balance -= parseFloat(amount);
    const txn = {
      transactionId: `TXN-${String(MockData.transactions.length + 1).padStart(6, '0')}`,
      transactionType: 'WITHDRAWAL',
      sourceAccountNumber: accountNumber,
      destinationAccountNumber: null,
      amount: parseFloat(amount),
      executionMode: BoltState.currentMode,
      threadName: `${channel}-THREAD-2`,
      channel: channel,
      status: 'SUCCESS',
      timestamp: new Date().toISOString(),
      durationMs: Math.floor(Math.random() * 6) + 1,
      description: `Withdrawal of ₹${amount} from account ${accountNumber} completed`
    };
    MockData.transactions.unshift(txn);
    return txn;
  },

  // POST Transfer
  async transfer(sourceAccountNumber, destinationAccountNumber, amount, channel = 'ONLINE') {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/transactions/transfer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sourceAccountNumber, destinationAccountNumber, amount: parseFloat(amount), channel })
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Transfer failed');
      }
      return await res.json();
    }
    // Offline simulation
    const src = MockData.accounts.find(a => a.accountNumber === sourceAccountNumber);
    const dest = MockData.accounts.find(a => a.accountNumber === destinationAccountNumber);
    if (!src) throw new Error(`Source account ${sourceAccountNumber} not found`);
    if (!dest) throw new Error(`Destination account ${destinationAccountNumber} not found`);
    if (sourceAccountNumber === destinationAccountNumber) throw new Error('Source and destination accounts must be different');
    if (BoltState.currentMode === 'SAFE' && src.balance < parseFloat(amount)) {
      throw new Error(`Insufficient balance in account ${sourceAccountNumber}`);
    }
    src.balance -= parseFloat(amount);
    dest.balance += parseFloat(amount);
    const txn = {
      transactionId: `TXN-${String(MockData.transactions.length + 1).padStart(6, '0')}`,
      transactionType: 'TRANSFER',
      sourceAccountNumber,
      destinationAccountNumber,
      amount: parseFloat(amount),
      executionMode: BoltState.currentMode,
      threadName: `${channel}-THREAD-1`,
      channel,
      status: 'SUCCESS',
      timestamp: new Date().toISOString(),
      durationMs: Math.floor(Math.random() * 8) + 2,
      description: `Transfer of ₹${amount} from ${sourceAccountNumber} to ${destinationAccountNumber} completed`
    };
    MockData.transactions.unshift(txn);
    return txn;
  },

  // POST Simulation - Concurrent Banking Run
  async runConcurrentSimulation() {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/simulation/run`, { method: 'POST' });
      if (res.ok) return await res.json();
    }
    return {
      simulationId: `SIM-${Math.random().toString(36).substring(2, 9).toUpperCase()}`,
      status: "COMPLETED",
      executionMode: BoltState.currentMode,
      totalTransactions: 5,
      successful: 5,
      failed: 0
    };
  },

  // POST Simulation - Race Condition Demonstration
  async runRaceConditionDemo() {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/simulation/race-condition`, { method: 'POST' });
      if (res.ok) return await res.json();
    }
    // Offline simulated race condition data
    return {
      simulationId: `SIM-${Math.random().toString(36).substring(2, 9).toUpperCase()}`,
      demonstration: "Race Condition",
      initialBalance: 10000.00,
      withdrawalAmountPerThread: 6000.00,
      finalBalance: 4000.00,
      expectedBalanceIfBothSucceed: -2000.00,
      expectedBalanceIfOnlyOneSucceeds: 4000.00,
      totalWithdrawnInRecords: 12000.00,
      successfulWithdrawals: 2,
      raceConditionDetected: true,
      explanation: "RACE CONDITION DEMONSTRATED: Both threads read initial balance of ₹10,000 before either updated it. Both accepted ₹6,000 withdrawal. Total withdrawn per records: ₹12,000, but final balance reflects only ₹6,000 deducted. This is a LOST UPDATE.",
      threadResults: [
        { thread: "ATM-THREAD-1", action: "WITHDRAWAL", amount: 6000, balanceReadBeforeBarrier: 10000, status: "SUCCESS", resultingBalance: 4000, durationMs: 52 },
        { thread: "ATM-THREAD-2", action: "WITHDRAWAL", amount: 6000, balanceReadBeforeBarrier: 10000, status: "SUCCESS", resultingBalance: 4000, durationMs: 55 }
      ]
    };
  },

  // POST Simulation - Deadlock Demo
  async runDeadlockDemo() {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/simulation/deadlock`, { method: 'POST' });
      if (res.ok) return await res.json();
    }
    return {
      deadlockDetected: true,
      account1: "ACC-1001",
      account2: "ACC-1002",
      threadA: "DEADLOCK-THREAD-A",
      threadB: "DEADLOCK-THREAD-B",
      explanation: "Circular wait demonstrated: Thread A held ACC-1001 and waited for ACC-1002, while Thread B held ACC-1002 and waited for ACC-1001. Neither could proceed — classic deadlock.",
      events: [
        { timestamp: Date.now() - 5000, thread: "DEADLOCK-THREAD-A", message: "Attempting to acquire lock on ACC-1001" },
        { timestamp: Date.now() - 5000, thread: "DEADLOCK-THREAD-B", message: "Attempting to acquire lock on ACC-1002" },
        { timestamp: Date.now() - 4950, thread: "DEADLOCK-THREAD-A", message: "✅ Acquired lock on ACC-1001" },
        { timestamp: Date.now() - 4950, thread: "DEADLOCK-THREAD-B", message: "✅ Acquired lock on ACC-1002" },
        { timestamp: Date.now() - 4850, thread: "DEADLOCK-THREAD-A", message: "Attempting to acquire lock on ACC-1002 (Thread B holds this lock)" },
        { timestamp: Date.now() - 4850, thread: "DEADLOCK-THREAD-B", message: "Attempting to acquire lock on ACC-1001 (Thread A holds this lock)" },
        { timestamp: Date.now(), thread: "DEADLOCK-THREAD-A", message: "❌ DEADLOCK: Could not acquire lock on ACC-1002 — timed out" }
      ]
    };
  },

  // POST Simulation - Deadlock Prevention Demo
  async runDeadlockPreventionDemo() {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/simulation/deadlock-prevention`, { method: 'POST' });
      if (res.ok) return await res.json();
    }
    return {
      deadlockPrevented: true,
      technique: "Consistent Lock Ordering",
      lockOrder: "ACC-1001 → ACC-1002",
      explanation: "Both threads acquired locks in the same order (ACC-1001 → ACC-1002), eliminating circular wait. No deadlock occurred.",
      events: [
        { timestamp: Date.now() - 400, thread: "PREVENTION-THREAD-A", message: "Using consistent lock ordering: ACC-1001 → ACC-1002" },
        { timestamp: Date.now() - 400, thread: "PREVENTION-THREAD-B", message: "Using consistent lock ordering: ACC-1001 → ACC-1002" },
        { timestamp: Date.now() - 350, thread: "PREVENTION-THREAD-A", message: "✅ Acquired lock on ACC-1001" },
        { timestamp: Date.now() - 250, thread: "PREVENTION-THREAD-A", message: "✅ Acquired lock on ACC-1002" },
        { timestamp: Date.now() - 240, thread: "PREVENTION-THREAD-A", message: "✅ Transfer completed successfully — released locks" },
        { timestamp: Date.now() - 200, thread: "PREVENTION-THREAD-B", message: "✅ Acquired lock on ACC-1001" },
        { timestamp: Date.now() - 100, thread: "PREVENTION-THREAD-B", message: "✅ Acquired lock on ACC-1002" },
        { timestamp: Date.now(), thread: "PREVENTION-THREAD-B", message: "✅ Transfer completed successfully — released locks" }
      ]
    };
  },

  // POST Simulation - Side-by-side SAFE vs UNSAFE Comparison
  async runComparisonDemo() {
    if (BoltState.isBackendConnected) {
      const res = await fetch(`${BOLT_CONFIG.API_BASE}/simulation/compare`, { method: 'POST' });
      if (res.ok) return await res.json();
    }
    return {
      comparisonId: `SIM-${Math.random().toString(36).substring(2, 9).toUpperCase()}`,
      description: "Equivalent concurrent withdrawal simulation (2 x ₹6,000 on initial ₹10,000)",
      unsafeMode: {
        executionMode: "UNSAFE",
        totalTransactions: 2,
        successfulTransactions: 2,
        failedTransactions: 0,
        initialBalance: 10000.0,
        expectedFinalBalance: -2000.0,
        actualFinalBalance: 4000.0,
        balanceMismatchDetected: true,
        lostUpdatesDetected: 1,
        negativeBalanceDetected: false,
        executionTimeMs: 56
      },
      safeMode: {
        executionMode: "SAFE",
        totalTransactions: 2,
        successfulTransactions: 1,
        failedTransactions: 1,
        initialBalance: 10000.0,
        expectedFinalBalance: 4000.0,
        actualFinalBalance: 4000.0,
        balanceMismatchDetected: false,
        lostUpdatesDetected: 0,
        negativeBalanceDetected: false,
        executionTimeMs: 2
      },
      verdict: "SAFE mode successfully prevented lost updates and prevented overdraft. UNSAFE mode exhibited race condition with 1 lost update."
    };
  }
};

// ============================================================
// FORMATTERS & UTILITY HELPERS
// ============================================================
function formatCurrency(val) {
  if (val === null || val === undefined) return '₹0.00';
  const num = typeof val === 'number' ? val : parseFloat(val);
  return '₹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(isoStr) {
  if (!isoStr) return '--';
  const d = new Date(isoStr);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) +
         ', ' + d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

// Initialise App Connection on startup
document.addEventListener('DOMContentLoaded', async () => {
  await BoltApi.checkConnection();
  if (BoltState.isBackendConnected) {
    console.log('%c⚡ BOLT Live Backend Connected (http://localhost:8080/api)', 'color: #10B981; font-weight: bold;');
  } else {
    console.log('%c⚡ BOLT Running in Standalone Glassmorphism Simulation Mode', 'color: #7091E6; font-weight: bold;');
  }
});
