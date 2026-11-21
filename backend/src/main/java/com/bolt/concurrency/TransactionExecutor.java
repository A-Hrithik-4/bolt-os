package com.bolt.concurrency;

import com.bolt.enums.ExecutionMode;
import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import com.bolt.model.Transaction;
import com.bolt.util.TransactionIdGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multithreaded transaction execution engine.
 * Submits banking operations to named thread pools (ATM / ONLINE)
 * and tracks simulation results by simulationId.
 */
@Component
public class TransactionExecutor {

    private static final Logger log = LoggerFactory.getLogger(TransactionExecutor.class);

    private final ExecutorService atmExecutor;
    private final ExecutorService onlineExecutor;

    /** Tracks running simulations: simulationId -> list of results */
    private final Map<String, List<Transaction>> simulationResults = new ConcurrentHashMap<>();
    private final Map<String, String> simulationStatuses = new ConcurrentHashMap<>();
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger completedThreads = new AtomicInteger(0);

    public TransactionExecutor(
            @Qualifier("atmExecutor") ExecutorService atmExecutor,
            @Qualifier("onlineExecutor") ExecutorService onlineExecutor) {
        this.atmExecutor = atmExecutor;
        this.onlineExecutor = onlineExecutor;
    }

    /**
     * Generate a unique simulation ID.
     */
    public String createSimulationId() {
        return "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Submit a banking operation to the appropriate thread pool.
     * Returns a Future containing the Transaction record with timing info.
     */
    public Future<Transaction> submit(String channel, Runnable operation,
                                       TransactionType type, String sourceAccount,
                                       String destAccount, BigDecimal amount,
                                       ExecutionMode mode, String simulationId) {
        ExecutorService executor = "ATM".equalsIgnoreCase(channel) ? atmExecutor : onlineExecutor;

        Callable<Transaction> task = () -> {
            String txnId = TransactionIdGenerator.generate();
            Transaction txn = new Transaction();
            txn.setTransactionId(txnId);
            txn.setTransactionType(type);
            txn.setSourceAccountNumber(sourceAccount);
            txn.setDestinationAccountNumber(destAccount);
            txn.setAmount(amount);
            txn.setExecutionMode(mode);
            txn.setThreadName(Thread.currentThread().getName());
            txn.setChannel(channel);
            txn.setTimestamp(LocalDateTime.now());
            txn.setSimulationId(simulationId);

            activeThreads.incrementAndGet();
            long start = System.currentTimeMillis();
            try {
                operation.run();
                txn.setStatus(TransactionStatus.SUCCESS);
                txn.setDescription(type + " of ₹" + amount.toPlainString() + " completed successfully");
            } catch (Exception e) {
                txn.setStatus(TransactionStatus.FAILED);
                txn.setErrorMessage(e.getMessage());
                txn.setDescription(type + " of ₹" + amount.toPlainString() + " failed: " + e.getMessage());
            } finally {
                long end = System.currentTimeMillis();
                txn.setDurationMs(end - start);
                activeThreads.decrementAndGet();
                completedThreads.incrementAndGet();
            }

            // Record in simulation results
            if (simulationId != null) {
                simulationResults.computeIfAbsent(simulationId, k -> new CopyOnWriteArrayList<>()).add(txn);
            }

            log.info("[{}] {} | {} | {} | ₹{} | {} | {}ms",
                    txn.getThreadName(), txn.getExecutionMode(), txn.getTransactionType(),
                    txn.getSourceAccountNumber(), txn.getAmount().toPlainString(),
                    txn.getStatus(), txn.getDurationMs());

            return txn;
        };

        return executor.submit(task);
    }

    /**
     * Submit a pre-built callable task (for simulations with custom logic).
     */
    public Future<Transaction> submitCallable(String channel, Callable<Transaction> task) {
        ExecutorService executor = "ATM".equalsIgnoreCase(channel) ? atmExecutor : onlineExecutor;
        return executor.submit(task);
    }

    public void markSimulationComplete(String simulationId) {
        simulationStatuses.put(simulationId, "COMPLETED");
    }

    public void markSimulationRunning(String simulationId) {
        simulationStatuses.put(simulationId, "RUNNING");
    }

    public List<Transaction> getSimulationResults(String simulationId) {
        return simulationResults.getOrDefault(simulationId, List.of());
    }

    public String getSimulationStatus(String simulationId) {
        return simulationStatuses.getOrDefault(simulationId, "UNKNOWN");
    }

    public int getActiveThreadCount() { return activeThreads.get(); }
    public int getCompletedThreadCount() { return completedThreads.get(); }

    public void recordSimulationTransaction(String simulationId, Transaction txn) {
        if (simulationId != null) {
            simulationResults.computeIfAbsent(simulationId, k -> new CopyOnWriteArrayList<>()).add(txn);
        }
    }
}
