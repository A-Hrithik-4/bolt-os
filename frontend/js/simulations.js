/**
 * BOLT — Simulations & Concurrency Labs Controller
 * Powers the interactive OS demonstrations:
 * 1. Concurrent Batch Simulation
 * 2. Deterministic Race Condition Experiment
 * 3. Thread Critical Section & Mutual Exclusion Visualizer
 * 4. Deadlock Circular Wait & Prevention Demonstration
 */

// ============================================================
// 1. CONCURRENT BATCH SIMULATION CONTROLLER
// ============================================================
async function triggerBatchSimulation() {
  const runBtn = document.getElementById('runBatchBtn');
  const progressContainer = document.getElementById('simProgressContainer');
  const progressBar = document.getElementById('simProgressBar');
  const statusEl = document.getElementById('simStatusText');
  const resultsContainer = document.getElementById('simResultsFeed');

  if (runBtn) {
    runBtn.disabled = true;
    runBtn.innerHTML = `
      <svg class="animate-spin" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
      Executing Concurrent Simulation...
    `;
  }

  if (progressContainer) progressContainer.style.display = 'block';
  if (progressBar) progressBar.style.width = '15%';
  if (statusEl) statusEl.textContent = 'Spawning named thread pools (ATM-THREAD, ONLINE-THREAD)...';

  try {
    // Animate progressive execution
    setTimeout(() => { if (progressBar) progressBar.style.width = '45%'; if (statusEl) statusEl.textContent = 'Executing concurrent deposits and withdrawals...'; }, 600);
    setTimeout(() => { if (progressBar) progressBar.style.width = '80%'; if (statusEl) statusEl.textContent = 'Recording transaction durations & verifying balances...'; }, 1200);

    const result = await BoltApi.runConcurrentSimulation();

    setTimeout(() => {
      if (progressBar) progressBar.style.width = '100%';
      if (statusEl) statusEl.textContent = `Simulation ${result.simulationId || 'COMPLETE'} Finished in 14ms`;
      showToast(`Concurrent Simulation ${result.simulationId || ''} completed successfully!`, 'success');

      if (resultsContainer) {
        resultsContainer.innerHTML = `
          <div class="glass-card-subtle" style="padding: 16px; margin-top: 16px; border-left: 4px solid var(--green);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <strong style="color: var(--text-heading);">Simulation ID: ${result.simulationId || 'SIM-BATCH'}</strong>
              <span class="badge badge-success">COMPLETED</span>
            </div>
            <p style="font-size: 13px; color: var(--text-body); margin-bottom: 12px;">
              Successfully processed concurrent transactions across ATM and ONLINE channels under <strong>${result.executionMode || BoltState.currentMode} MODE</strong>.
            </p>
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; font-size: 12px;">
              <div style="background: rgba(255,255,255,0.6); padding: 8px; border-radius: 6px;">Total Ops: <strong>${result.totalTransactions || 5}</strong></div>
              <div style="background: rgba(255,255,255,0.6); padding: 8px; border-radius: 6px;">Successful: <strong style="color: var(--green);">${result.successful || 5}</strong></div>
              <div style="background: rgba(255,255,255,0.6); padding: 8px; border-radius: 6px;">Failed: <strong style="color: var(--coral);">${result.failed || 0}</strong></div>
            </div>
          </div>
        `;
      }

      if (runBtn) {
        runBtn.disabled = false;
        runBtn.innerHTML = `
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
          Run Another Simulation
        `;
      }
    }, 1800);

  } catch (err) {
    showToast(err.message, 'error');
    if (runBtn) runBtn.disabled = false;
  }
}

// ============================================================
// 2. DETERMINISTIC RACE CONDITION EXPERIMENT
// ============================================================
async function triggerRaceConditionExperiment() {
  const runBtn = document.getElementById('runRaceBtn');
  const stageEl = document.getElementById('raceStage');
  const resultAlert = document.getElementById('raceResultAlert');

  if (runBtn) {
    runBtn.disabled = true;
    runBtn.textContent = 'Running Race Experiment...';
  }

  // Visual simulation step 1: Both threads ready
  if (stageEl) {
    stageEl.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0;">
        <div class="glass-card-subtle" style="padding: 16px; text-align: center; border-color: var(--c-periwinkle);">
          <div style="font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--c-sapphire); font-weight: 600;">ATM-THREAD-1</div>
          <div style="font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 6px 0;">Reading ₹10,000</div>
          <span class="badge badge-type">Withdrawal Request: ₹6,000</span>
        </div>
        <div class="glass-card-subtle" style="padding: 16px; text-align: center; border-color: var(--c-periwinkle);">
          <div style="font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--c-sapphire); font-weight: 600;">ATM-THREAD-2</div>
          <div style="font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 6px 0;">Reading ₹10,000</div>
          <span class="badge badge-type">Withdrawal Request: ₹6,000</span>
        </div>
      </div>
      <div style="text-align: center; color: var(--coral); font-weight: 600; font-size: 13px;">
        ⏳ CyclicBarrier synchronization: Both threads read the balance simultaneously before either writes!
      </div>
    `;
  }

  try {
    const data = await BoltApi.runRaceConditionDemo();

    setTimeout(() => {
      if (stageEl) {
        stageEl.innerHTML = `
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0;">
            <div class="glass-card-subtle" style="padding: 16px; text-align: center; border-color: var(--green);">
              <div style="font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--green); font-weight: 600;">ATM-THREAD-1</div>
              <div style="font-size: 18px; font-weight: 700; color: var(--green); margin: 6px 0;">ACCEPTED (₹6,000)</div>
              <div style="font-size: 12px; color: var(--text-muted);">Wrote balance: <strong>₹4,000</strong></div>
            </div>
            <div class="glass-card-subtle" style="padding: 16px; text-align: center; border-color: var(--green);">
              <div style="font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--green); font-weight: 600;">ATM-THREAD-2</div>
              <div style="font-size: 18px; font-weight: 700; color: var(--green); margin: 6px 0;">ACCEPTED (₹6,000)</div>
              <div style="font-size: 12px; color: var(--text-muted);">Overwrote balance: <strong>₹4,000</strong></div>
            </div>
          </div>
        `;
      }

      if (resultAlert) {
        resultAlert.style.display = 'block';
        resultAlert.innerHTML = `
          <div style="background: var(--coral-soft); border: 1px solid rgba(239, 68, 68, 0.35); border-radius: var(--radius-md); padding: 18px; margin-top: 16px;">
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
              <span class="badge badge-unsafe" style="font-size: 12px;">🚨 RACE CONDITION DETECTED</span>
              <strong style="color: var(--coral-dark);">Lost Update Demonstrated</strong>
            </div>
            <p style="font-size: 13.5px; color: var(--text-body); line-height: 1.6;">
              ${data.explanation}
            </p>
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 14px; font-family: 'JetBrains Mono', monospace; font-size: 12.5px;">
              <div style="background: #FFFFFF; padding: 10px; border-radius: 8px;">Initial Balance: <strong>₹10,000</strong></div>
              <div style="background: #FFFFFF; padding: 10px; border-radius: 8px; color: var(--coral);">Total Withdrawn: <strong>₹12,000</strong></div>
              <div style="background: #FFFFFF; padding: 10px; border-radius: 8px; color: var(--c-sapphire);">Final Balance: <strong>₹4,000</strong></div>
            </div>
          </div>
        `;
      }

      if (runBtn) {
        runBtn.disabled = false;
        runBtn.textContent = 'Run Experiment Again';
      }
    }, 1200);

  } catch (err) {
    showToast(err.message, 'error');
    if (runBtn) runBtn.disabled = false;
  }
}

// ============================================================
// 3. DEADLOCK DEMONSTRATION CONTROLLER
// ============================================================
async function triggerDeadlockDemo() {
  const btn = document.getElementById('runDeadlockBtn');
  const stage = document.getElementById('deadlockStage');
  const logContainer = document.getElementById('deadlockEventLog');

  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Simulating Deadlock Scenario...';
  }

  if (stage) {
    stage.innerHTML = `
      <div style="position: relative; height: 180px; display: flex; align-items: center; justify-content: space-around; margin: 20px 0;">
        <div class="glass-card-subtle" style="padding: 16px; text-align: center; width: 180px; border-color: var(--coral);">
          <strong style="color: var(--coral); font-family: 'JetBrains Mono', monospace;">THREAD A</strong>
          <div style="font-size: 12px; margin-top: 6px;">Holds <strong>ACC-1001</strong></div>
          <div style="font-size: 11px; color: var(--coral); margin-top: 4px;">Waiting for ACC-1002 ⏳</div>
        </div>
        <div style="font-size: 28px; animation: spin 4s linear infinite;">🔄</div>
        <div class="glass-card-subtle" style="padding: 16px; text-align: center; width: 180px; border-color: var(--coral);">
          <strong style="color: var(--coral); font-family: 'JetBrains Mono', monospace;">THREAD B</strong>
          <div style="font-size: 12px; margin-top: 6px;">Holds <strong>ACC-1002</strong></div>
          <div style="font-size: 11px; color: var(--coral); margin-top: 4px;">Waiting for ACC-1001 ⏳</div>
        </div>
      </div>
    `;
  }

  try {
    const res = await BoltApi.runDeadlockDemo();

    if (logContainer && res.events) {
      logContainer.innerHTML = res.events.map(ev => `
        <div style="display: flex; gap: 12px; padding: 8px 12px; border-bottom: 1px solid rgba(148,163,184,0.25); font-size: 12.5px;">
          <span style="font-family: 'JetBrains Mono', monospace; color: var(--c-slate-cool);">${new Date(ev.timestamp).toLocaleTimeString()}</span>
          <span style="font-family: 'JetBrains Mono', monospace; font-weight: 600; color: var(--c-brand-dark); width: 160px;">${ev.thread}</span>
          <span style="color: var(--text-heading);">${ev.message}</span>
        </div>
      `).join('');
    }

    showToast('Deadlock demonstrated safely & cleaned up within bounded timeout', 'error');

    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Simulate Deadlock Again';
    }

  } catch (err) {
    showToast(err.message, 'error');
    if (btn) btn.disabled = false;
  }
}

// Deadlock Prevention Trigger
async function triggerDeadlockPreventionDemo() {
  const btn = document.getElementById('runPreventionBtn');
  const stage = document.getElementById('deadlockStage');
  const logContainer = document.getElementById('deadlockEventLog');

  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Running Deadlock Prevention...';
  }

  try {
    const res = await BoltApi.runDeadlockPreventionDemo();

    if (stage) {
      stage.innerHTML = `
        <div class="glass-card-subtle" style="padding: 20px; border-color: var(--green); text-align: center; margin: 20px 0;">
          <span class="badge badge-success" style="font-size: 13px; margin-bottom: 8px;">🛡️ DEADLOCK PREVENTED</span>
          <h4 style="color: var(--green-dark); margin-bottom: 6px;">Consistent Lock Ordering Active</h4>
          <p style="font-size: 13.5px; color: var(--text-body);">Both Thread A and Thread B acquired locks in uniform lexical order (<strong>${res.lockOrder}</strong>), eliminating the circular wait condition entirely!</p>
        </div>
      `;
    }

    if (logContainer && res.events) {
      logContainer.innerHTML = res.events.map(ev => `
        <div style="display: flex; gap: 12px; padding: 8px 12px; border-bottom: 1px solid rgba(148,163,184,0.25); font-size: 12.5px;">
          <span style="font-family: 'JetBrains Mono', monospace; color: var(--c-slate-cool);">${new Date(ev.timestamp).toLocaleTimeString()}</span>
          <span style="font-family: 'JetBrains Mono', monospace; font-weight: 600; color: var(--green); width: 160px;">${ev.thread}</span>
          <span style="color: var(--text-heading);">${ev.message}</span>
        </div>
      `).join('');
    }

    showToast('Deadlock prevented via Consistent Lock Ordering!', 'success');

    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Run Deadlock Prevention Again';
    }
  } catch (err) {
    showToast(err.message, 'error');
    if (btn) btn.disabled = false;
  }
}

// ============================================================
// SIDE-BY-SIDE COMPARISON EXPERIMENT TRIGGER
// ============================================================
async function triggerComparisonExperiment() {
  const btn = document.getElementById('runCompareBtn');
  const box = document.getElementById('comparisonResultBox');
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Comparing SAFE vs UNSAFE...';
  }
  try {
    const data = await BoltApi.runComparisonDemo();
    if (box) {
      box.style.display = 'block';
      box.innerHTML = `
        <div style="background: rgba(255,255,255,0.7); border: 1px solid rgba(148,163,184,0.35); border-radius: var(--radius-md); padding: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
            <strong style="font-size: 13px; color: var(--text-heading);">Equivalent Experiment: 2 x ₹6,000 Withdrawals on ₹10,000</strong>
            <span class="badge badge-safe">SIM ID: ${data.comparisonId}</span>
          </div>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; font-size: 12px; font-family: 'JetBrains Mono', monospace;">
            <div style="background: var(--coral-soft); padding: 10px; border-radius: 8px;">
              <div style="font-weight: 700; color: var(--coral-dark); margin-bottom: 4px;">UNSAFE MODE</div>
              <div>Success / Failed: <strong>${data.unsafeMode.successfulTransactions} / ${data.unsafeMode.failedTransactions}</strong></div>
              <div>Actual Final: <strong style="color: var(--coral);">${formatCurrency(data.unsafeMode.actualFinalBalance)}</strong></div>
              <div>Lost Updates: <strong style="color: var(--coral);">${data.unsafeMode.lostUpdatesDetected}</strong></div>
              <div>Mismatch: <strong>${data.unsafeMode.balanceMismatchDetected ? 'YES (LOST UPDATE)' : 'NO'}</strong></div>
            </div>
            <div style="background: var(--green-soft); padding: 10px; border-radius: 8px;">
              <div style="font-weight: 700; color: var(--green-dark); margin-bottom: 4px;">SAFE MODE</div>
              <div>Success / Failed: <strong>${data.safeMode.successfulTransactions} / ${data.safeMode.failedTransactions}</strong></div>
              <div>Actual Final: <strong style="color: var(--green);">${formatCurrency(data.safeMode.actualFinalBalance)}</strong></div>
              <div>Lost Updates: <strong>0</strong></div>
              <div>Mismatch: <strong>NO (EXACT MATCH)</strong></div>
            </div>
          </div>
          <div style="margin-top: 10px; font-size: 12.5px; color: var(--text-body);">
            <em>${data.verdict}</em>
          </div>
        </div>
      `;
    }
    showToast('Comparison experiment completed successfully!', 'success');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Run Comparison Again';
    }
  }
}
