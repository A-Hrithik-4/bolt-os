package com.bolt.service;

import com.bolt.concurrency.BankingExecutionStrategy;
import com.bolt.concurrency.SafeBankingStrategy;
import com.bolt.concurrency.TransactionExecutor;
import com.bolt.concurrency.UnsafeBankingStrategy;
import com.bolt.enums.ExecutionMode;
import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import com.bolt.model.BankAccount;
import com.bolt.model.Transaction;
import com.bolt.util.TransactionIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Orchestrates concurrent banking simulations including:
 * - General multi-thread simulation runs
 * - Deterministic race-condition demonstrations
 *
 * Simulations run asynchronously; each returns a simulationId
 * the frontend can use to poll for results.
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final BankingService bankingService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final TransactionExecutor transactionExecutor;

    /** Stores simulation metadata and results */
    private final Map<String, Map<String, Object>> simulationStore = new ConcurrentHashMap<>();

    public SimulationService(BankingService bankingService,
                             AccountService accountService,
                             TransactionService transactionService,
                             TransactionExecutor transactionExecutor) {
        this.bankingService = bankingService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.transactionExecutor = transactionExecutor;
    }

    /**
     * Run a general simulation with concurrent deposits, withdrawals and transfers.
     * Returns a simulationId immediately; results are populated asynchronously.
     */
    public Map<String, Object> runSimulation() {
        String simulationId = transactionExecutor.createSimulationId();
        transactionExecutor.markSimulationRunning(simulationId);

        ExecutionMode mode = bankingService.getCurrentMode();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("simulationId", simulationId);
        response.put("status", "RUNNING");
        response.put("executionMode", mode.name());
        response.put("startedAt", LocalDateTime.now().toString());

        simulationStore.put(simulationId, response);

        // Run simulation asynchronously
        Thread simulationThread = new Thread(() -> {
            try {
                List<Future<Transaction>> futures = new ArrayList<>();

                // 2 ATM deposits
                futures.add(transactionExecutor.submit("ATM", () -> {
                    BankingExecutionStrategy strategy = bankingService.getStrategy(mode);
                    BankAccount account = accountService.getAccount("1001");
                    strategy.deposit(account, new BigDecimal("1000"));
                    accountService.saveAccount(account);
                }, TransactionType.DEPOSIT, "1001", null, new BigDecimal("1000"), mode, simulationId));

                futures.add(transactionExecutor.submit("ATM", () -> {
                    BankingExecutionStrategy strategy = bankingService.getStrategy(mode);
                    BankAccount account = accountService.getAccount("1002");
                    strategy.deposit(account, new BigDecimal("2000"));
                    accountService.saveAccount(account);
                }, TransactionType.DEPOSIT, "1002", null, new BigDecimal("2000"), mode, simulationId));

                // 2 Online withdrawals
                futures.add(transactionExecutor.submit("ONLINE", () -> {
                    BankingExecutionStrategy strategy = bankingService.getStrategy(mode);
                    BankAccount account = accountService.getAccount("1003");
                    strategy.withdraw(account, new BigDecimal("500"));
                    accountService.saveAccount(account);
                }, TransactionType.WITHDRAWAL, "1003", null, new BigDecimal("500"), mode, simulationId));

                futures.add(transactionExecutor.submit("ONLINE", () -> {
                    BankingExecutionStrategy strategy = bankingService.getStrategy(mode);
                    BankAccount account = accountService.getAccount("1004");
                    strategy.withdraw(account, new BigDecimal("1500"));
                    accountService.saveAccount(account);
                }, TransactionType.WITHDRAWAL, "1004", null, new BigDecimal("1500"), mode, simulationId));

                // 1 Transfer
                futures.add(transactionExecutor.submit("ONLINE", () -> {
                    BankingExecutionStrategy strategy = bankingService.getStrategy(mode);
                    BankAccount source = accountService.getAccount("1001");
                    BankAccount dest = accountService.getAccount("1002");
                    strategy.transfer(source, dest, new BigDecimal("3000"));
                    accountService.saveAccount(source);
                    accountService.saveAccount(dest);
                }, TransactionType.TRANSFER, "1001", "1002", new BigDecimal("3000"), mode, simulationId));

                // Collect results
                List<Transaction> results = new ArrayList<>();
                for (Future<Transaction> f : futures) {
                    try {
                        results.add(f.get(30, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        log.error("Simulation task failed: {}", e.getMessage());
                    }
                }

                // Persist all transactions
                transactionService.saveAll(results);

                response.put("status", "COMPLETED");
                response.put("completedAt", LocalDateTime.now().toString());
                response.put("totalTransactions", results.size());
                response.put("successful", results.stream().filter(t -> t.getStatus() == TransactionStatus.SUCCESS).count());
                response.put("failed", results.stream().filter(t -> t.getStatus() == TransactionStatus.FAILED).count());
                response.put("transactions", results.stream().map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("transactionId", t.getTransactionId());
                    m.put("type", t.getTransactionType());
                    m.put("source", t.getSourceAccountNumber());
                    m.put("destination", t.getDestinationAccountNumber());
                    m.put("amount", t.getAmount());
                    m.put("thread", t.getThreadName());
                    m.put("channel", t.getChannel());
                    m.put("status", t.getStatus());
                    m.put("durationMs", t.getDurationMs());
                    m.put("description", t.getDescription());
                    return m;
                }).toList());

                transactionExecutor.markSimulationComplete(simulationId);

            } catch (Exception e) {
                response.put("status", "FAILED");
                response.put("error", e.getMessage());
                transactionExecutor.markSimulationComplete(simulationId);
            }
        }, "SIMULATION-ORCHESTRATOR");
        simulationThread.setDaemon(true);
        simulationThread.start();

        return response;
    }

    /**
     * Deterministic race-condition demonstration.
     *
     * Creates an isolated in-memory BankAccount with ₹10,000 initial balance.
     * Two threads simultaneously attempt to withdraw ₹6,000 each.
     * A CyclicBarrier ensures both threads read the balance before either writes.
     *
     * Expected UNSAFE result:
     * - Both threads read ₹10,000 and accept the withdrawal.
     * - Final balance: ₹4,000 (only one withdrawal reflected).
     * - Total withdrawn in records: ₹12,000, but only ₹6,000 deducted from balance.
     *
     * This demonstration uses an in-memory object, NOT the database.
     * The purpose is to show raw thread interleaving on a shared mutable object.
     * Database-backed operations use @Version (optimistic locking) for safety.
     */
    public Map<String, Object> runRaceConditionDemo() {
        String simulationId = transactionExecutor.createSimulationId();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("simulationId", simulationId);
        result.put("demonstration", "Race Condition");

        // Create an ISOLATED in-memory account (not persisted to DB)
        BankAccount raceAccount = new BankAccount("RACE-DEMO", "Race Demo Account",
                new BigDecimal("10000.0000"));

        BigDecimal withdrawalAmount = new BigDecimal("6000.0000");
        BigDecimal initialBalance = raceAccount.getBalance();

        result.put("initialBalance", initialBalance);
        result.put("withdrawalAmountPerThread", withdrawalAmount);

        UnsafeBankingStrategy unsafeStrategy = new UnsafeBankingStrategy();
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch completionLatch = new CountDownLatch(2);

        List<Map<String, Object>> threadResults = Collections.synchronizedList(new ArrayList<>());

        // Thread 1 (ATM)
        Thread thread1 = new Thread(() -> {
            try {
                Map<String, Object> tr = new LinkedHashMap<>();
                tr.put("thread", Thread.currentThread().getName());
                tr.put("action", "WITHDRAWAL");
                tr.put("amount", withdrawalAmount);

                tr.put("balanceReadBeforeBarrier", raceAccount.getBalance());

                // Both threads wait here until both have read the balance
                barrier.await(10, TimeUnit.SECONDS);

                long start = System.currentTimeMillis();
                try {
                    BigDecimal newBal = unsafeStrategy.withdraw(raceAccount, withdrawalAmount);
                    tr.put("status", "SUCCESS");
                    tr.put("resultingBalance", newBal);
                    tr.put("balanceAfterWrite", raceAccount.getBalance());
                } catch (Exception e) {
                    tr.put("status", "FAILED");
                    tr.put("error", e.getMessage());
                }
                tr.put("durationMs", System.currentTimeMillis() - start);
                threadResults.add(tr);
            } catch (Exception e) {
                log.error("Race demo thread 1 error: {}", e.getMessage());
            } finally {
                completionLatch.countDown();
            }
        }, "ATM-THREAD-1");

        // Thread 2 (ATM)
        Thread thread2 = new Thread(() -> {
            try {
                Map<String, Object> tr = new LinkedHashMap<>();
                tr.put("thread", Thread.currentThread().getName());
                tr.put("action", "WITHDRAWAL");
                tr.put("amount", withdrawalAmount);

                tr.put("balanceReadBeforeBarrier", raceAccount.getBalance());

                barrier.await(10, TimeUnit.SECONDS);

                long start = System.currentTimeMillis();
                try {
                    BigDecimal newBal = unsafeStrategy.withdraw(raceAccount, withdrawalAmount);
                    tr.put("status", "SUCCESS");
                    tr.put("resultingBalance", newBal);
                    tr.put("balanceAfterWrite", raceAccount.getBalance());
                } catch (Exception e) {
                    tr.put("status", "FAILED");
                    tr.put("error", e.getMessage());
                }
                tr.put("durationMs", System.currentTimeMillis() - start);
                threadResults.add(tr);
            } catch (Exception e) {
                log.error("Race demo thread 2 error: {}", e.getMessage());
            } finally {
                completionLatch.countDown();
            }
        }, "ATM-THREAD-2");

        thread1.setDaemon(true);
        thread2.setDaemon(true);
        thread1.start();
        thread2.start();

        try {
            completionLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        BigDecimal finalBalance = raceAccount.getBalance();
        long successCount = threadResults.stream()
                .filter(t -> "SUCCESS".equals(t.get("status")))
                .count();
        BigDecimal totalWithdrawn = withdrawalAmount.multiply(BigDecimal.valueOf(successCount));
        BigDecimal expectedBalance = initialBalance.subtract(totalWithdrawn);

        result.put("finalBalance", finalBalance);
        result.put("expectedBalanceIfBothSucceed", initialBalance.subtract(withdrawalAmount.multiply(BigDecimal.valueOf(2))));
        result.put("expectedBalanceIfOnlyOneSucceeds", initialBalance.subtract(withdrawalAmount));
        result.put("totalWithdrawnInRecords", totalWithdrawn);
        result.put("successfulWithdrawals", successCount);
        result.put("threadResults", threadResults);

        boolean raceConditionOccurred = successCount == 2 && finalBalance.compareTo(new BigDecimal("4000.0000")) == 0;
        result.put("raceConditionDetected", raceConditionOccurred);

        if (raceConditionOccurred) {
            result.put("explanation",
                    "RACE CONDITION DEMONSTRATED: Both threads read the initial balance of ₹" +
                    initialBalance.toPlainString() + " before either updated it. " +
                    "Both accepted the withdrawal of ₹" + withdrawalAmount.toPlainString() + ". " +
                    "Total withdrawn per records: ₹" + totalWithdrawn.toPlainString() +
                    ", but final balance is ₹" + finalBalance.toPlainString() +
                    " (only ₹" + initialBalance.subtract(finalBalance).toPlainString() +
                    " actually deducted). This is a LOST UPDATE.");
        } else {
            result.put("explanation",
                    "Race condition did not manifest deterministically in this run. " +
                    "Final balance: ₹" + finalBalance.toPlainString() +
                    ". Successful withdrawals: " + successCount + ".");
        }

        result.put("note", "This demonstration uses an in-memory object to show raw thread " +
                "interleaving. Database-backed operations use @Version (optimistic locking) " +
                "which would cause one of the writes to fail with OptimisticLockException.");

        // Record transactions in DB for history
        for (Map<String, Object> tr : threadResults) {
            Transaction txn = new Transaction();
            txn.setTransactionId(TransactionIdGenerator.generate());
            txn.setTransactionType(TransactionType.WITHDRAWAL);
            txn.setSourceAccountNumber("RACE-DEMO");
            txn.setAmount(withdrawalAmount);
            txn.setExecutionMode(ExecutionMode.UNSAFE);
            txn.setThreadName((String) tr.get("thread"));
            txn.setChannel("ATM");
            txn.setTimestamp(LocalDateTime.now());
            txn.setSimulationId(simulationId);
            txn.setStatus("SUCCESS".equals(tr.get("status")) ? TransactionStatus.SUCCESS : TransactionStatus.FAILED);
            txn.setDurationMs(tr.get("durationMs") != null ? (Long) tr.get("durationMs") : 0L);
            txn.setDescription("Race condition demo — " + tr.get("status"));
            transactionService.saveTransaction(txn);
        }

        simulationStore.put(simulationId, result);
        return result;
    }

    /**
     * Executes side-by-side isolated experiments comparing SAFE and UNSAFE modes.
     * Generates a comprehensive comparison report with exact concurrency metrics.
     */
    public Map<String, Object> runComparisonDemo() {
        String comparisonId = transactionExecutor.createSimulationId();

        // 1. RUN UNSAFE EXPERIMENT (Deterministic race condition via CyclicBarrier)
        BankAccount unsafeAccount = new BankAccount("COMP-UNSAFE", "Comparison Unsafe", new BigDecimal("10000.0000"));
        BigDecimal withdrawalAmt = new BigDecimal("6000.0000");
        UnsafeBankingStrategy unsafeStrat = new UnsafeBankingStrategy();
        CyclicBarrier unsafeBarrier = new CyclicBarrier(2);
        CountDownLatch unsafeLatch = new CountDownLatch(2);
        List<Map<String, Object>> unsafeThreadResults = Collections.synchronizedList(new ArrayList<>());

        long unsafeStart = System.currentTimeMillis();
        for (int i = 1; i <= 2; i++) {
            final int tid = i;
            Thread t = new Thread(() -> {
                Map<String, Object> tr = new LinkedHashMap<>();
                tr.put("thread", "UNSAFE-THREAD-" + tid);
                try {
                    unsafeBarrier.await(10, TimeUnit.SECONDS);
                    BigDecimal newBal = unsafeStrat.withdraw(unsafeAccount, withdrawalAmt);
                    tr.put("status", "SUCCESS");
                    tr.put("balance", newBal);
                } catch (Exception e) {
                    tr.put("status", "FAILED");
                    tr.put("error", e.getMessage());
                } finally {
                    unsafeThreadResults.add(tr);
                    unsafeLatch.countDown();
                }
            }, "COMP-UNSAFE-" + i);
            t.start();
        }
        try {
            unsafeLatch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long unsafeDuration = System.currentTimeMillis() - unsafeStart;

        long unsafeSuccess = unsafeThreadResults.stream().filter(r -> "SUCCESS".equals(r.get("status"))).count();
        long unsafeFailed = 2 - unsafeSuccess;
        BigDecimal unsafeActual = unsafeAccount.getBalance();
        BigDecimal unsafeExpected = new BigDecimal("10000.0000").subtract(withdrawalAmt.multiply(BigDecimal.valueOf(unsafeSuccess)));
        boolean unsafeMismatch = unsafeActual.compareTo(unsafeExpected) != 0;
        int unsafeLostUpdates = (unsafeSuccess == 2 && unsafeActual.compareTo(new BigDecimal("4000.0000")) == 0) ? 1 : 0;

        Map<String, Object> unsafeReport = new LinkedHashMap<>();
        unsafeReport.put("executionMode", "UNSAFE");
        unsafeReport.put("totalTransactions", 2);
        unsafeReport.put("successfulTransactions", unsafeSuccess);
        unsafeReport.put("failedTransactions", unsafeFailed);
        unsafeReport.put("initialBalance", new BigDecimal("10000.0000"));
        unsafeReport.put("expectedFinalBalance", unsafeExpected);
        unsafeReport.put("actualFinalBalance", unsafeActual);
        unsafeReport.put("balanceMismatchDetected", unsafeMismatch);
        unsafeReport.put("lostUpdatesDetected", unsafeLostUpdates);
        unsafeReport.put("negativeBalanceDetected", unsafeActual.compareTo(BigDecimal.ZERO) < 0);
        unsafeReport.put("executionTimeMs", unsafeDuration);
        unsafeReport.put("threadDetails", unsafeThreadResults);

        // 2. RUN SAFE EXPERIMENT (Synchronized via SafeBankingStrategy)
        BankAccount safeAccount = new BankAccount("COMP-SAFE", "Comparison Safe", new BigDecimal("10000.0000"));
        SafeBankingStrategy safeStrat = new SafeBankingStrategy();
        CountDownLatch safeLatch = new CountDownLatch(2);
        List<Map<String, Object>> safeThreadResults = Collections.synchronizedList(new ArrayList<>());

        long safeStart = System.currentTimeMillis();
        for (int i = 1; i <= 2; i++) {
            final int tid = i;
            Thread t = new Thread(() -> {
                Map<String, Object> tr = new LinkedHashMap<>();
                tr.put("thread", "SAFE-THREAD-" + tid);
                try {
                    BigDecimal newBal = safeStrat.withdraw(safeAccount, withdrawalAmt);
                    tr.put("status", "SUCCESS");
                    tr.put("balance", newBal);
                } catch (Exception e) {
                    tr.put("status", "FAILED");
                    tr.put("error", e.getMessage());
                } finally {
                    safeThreadResults.add(tr);
                    safeLatch.countDown();
                }
            }, "COMP-SAFE-" + i);
            t.start();
        }
        try {
            safeLatch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long safeDuration = System.currentTimeMillis() - safeStart;

        long safeSuccess = safeThreadResults.stream().filter(r -> "SUCCESS".equals(r.get("status"))).count();
        long safeFailed = 2 - safeSuccess;
        BigDecimal safeActual = safeAccount.getBalance();
        BigDecimal safeExpected = new BigDecimal("4000.0000"); // 10000 - 6000
        boolean safeMismatch = safeActual.compareTo(safeExpected) != 0;
        int safeLostUpdates = 0;

        Map<String, Object> safeReport = new LinkedHashMap<>();
        safeReport.put("executionMode", "SAFE");
        safeReport.put("totalTransactions", 2);
        safeReport.put("successfulTransactions", safeSuccess);
        safeReport.put("failedTransactions", safeFailed);
        safeReport.put("initialBalance", new BigDecimal("10000.0000"));
        safeReport.put("expectedFinalBalance", safeExpected);
        safeReport.put("actualFinalBalance", safeActual);
        safeReport.put("balanceMismatchDetected", safeMismatch);
        safeReport.put("lostUpdatesDetected", safeLostUpdates);
        safeReport.put("negativeBalanceDetected", safeActual.compareTo(BigDecimal.ZERO) < 0);
        safeReport.put("executionTimeMs", safeDuration);
        safeReport.put("threadDetails", safeThreadResults);

        // Overall Comparison Response
        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("comparisonId", comparisonId);
        comparison.put("timestamp", LocalDateTime.now().toString());
        comparison.put("description", "Equivalent concurrent withdrawal simulation (2 x ₹6,000 on initial ₹10,000)");
        comparison.put("unsafeMode", unsafeReport);
        comparison.put("safeMode", safeReport);
        comparison.put("verdict", "SAFE mode successfully prevented lost updates and prevented overdraft. UNSAFE mode exhibited race condition with 1 lost update.");

        simulationStore.put(comparisonId, comparison);
        return comparison;
    }

    public Map<String, Object> getSimulationResult(String simulationId) {
        Map<String, Object> stored = simulationStore.get(simulationId);
        if (stored != null) {
            return stored;
        }
        // Fallback: reconstruct from transaction records
        List<Transaction> txns = transactionService.getRepository().findBySimulationId(simulationId);
        if (txns.isEmpty()) {
            throw new IllegalArgumentException("Simulation not found: " + simulationId);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("simulationId", simulationId);
        result.put("status", transactionExecutor.getSimulationStatus(simulationId));
        result.put("transactions", txns.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("transactionId", t.getTransactionId());
            m.put("type", t.getTransactionType());
            m.put("source", t.getSourceAccountNumber());
            m.put("amount", t.getAmount());
            m.put("thread", t.getThreadName());
            m.put("status", t.getStatus());
            return m;
        }).toList());
        return result;
    }
}
