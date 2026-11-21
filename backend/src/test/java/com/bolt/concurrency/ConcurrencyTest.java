package com.bolt.concurrency;

import com.bolt.model.BankAccount;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for BOLT banking operations.
 *
 * <p>Demonstrates race conditions with the unsafe (lock-free) strategy,
 * correctness with the safe (ReentrantLock-based) strategy, and
 * deadlock detection / prevention via {@link DeadlockSimulator}.
 */
@SpringBootTest
class ConcurrencyTest {

    @Autowired
    @Qualifier("unsafeStrategy")
    private BankingExecutionStrategy unsafeStrategy;

    @Autowired
    @Qualifier("safeStrategy")
    private SafeBankingStrategy safeStrategy;

    @Autowired
    private DeadlockSimulator deadlockSimulator;

    @AfterEach
    void cleanup() {
        safeStrategy.clearLocks();
    }

    // ──────────────────────────────────────────────────────────────
    // 1. UNSAFE – Race condition on simultaneous withdrawals
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UNSAFE: Race condition – two threads withdraw ₹6000 from ₹10000, both succeed (lost update)")
    void raceConditionDemo_unsafeWithdrawals() throws Exception {
        BankAccount account = new BankAccount("TEST-RC-001", "Test Race", new BigDecimal("10000"));

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    unsafeStrategy.withdraw(account, new BigDecimal("6000"));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // InsufficientBalanceException or barrier exception
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Executor did not terminate in time");

        // Both withdrawals succeed because there is no synchronisation —
        // each thread reads ₹10000, decides it is sufficient, and subtracts ₹6000.
        assertEquals(2, successCount.get(),
                "Both withdrawals should succeed due to the race condition");

        // Only one withdrawal is reflected in the balance (lost update).
        assertEquals(new BigDecimal("4000"), unsafeStrategy.getBalance(account),
                "Final balance should be ₹4000 — one withdrawal's effect is lost");
    }

    // ──────────────────────────────────────────────────────────────
    // 2. UNSAFE – Lost updates on concurrent deposits
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UNSAFE: Lost updates – 5 concurrent deposits of ₹1000 do NOT produce correct ₹10000 balance")
    void incorrectBalanceDemo_unsafeDeposits() throws Exception {
        BankAccount account = new BankAccount("TEST-LU-001", "Test Lost Update", new BigDecimal("5000"));

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    unsafeStrategy.deposit(account, new BigDecimal("1000"));
                } catch (Exception ignored) {
                    // swallow
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads at once to maximise contention.
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
                "Threads did not finish in time");

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Executor did not terminate in time");

        BigDecimal expectedCorrectBalance = new BigDecimal("10000");
        BigDecimal actualBalance = unsafeStrategy.getBalance(account);

        // The 50 ms processing delay in UnsafeBankingStrategy makes lost updates
        // virtually certain with 5 concurrent threads.
        assertNotEquals(expectedCorrectBalance, actualBalance,
                "Balance should NOT be ₹10000 — lost updates prove the unsafe strategy is broken. Actual: ₹" + actualBalance);
    }

    // ──────────────────────────────────────────────────────────────
    // 3. SAFE – Concurrent deposits produce exact result
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SAFE: 10 concurrent deposits of ₹500 on ₹10000 account yield exactly ₹15000")
    void safeConcurrentDeposits() throws Exception {
        BankAccount account = new BankAccount("TEST-SD-001", "Test Safe Deposit", new BigDecimal("10000"));

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    safeStrategy.deposit(account, new BigDecimal("500"));
                } catch (Exception ignored) {
                    // swallow
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
                "Threads did not finish in time");

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Executor did not terminate in time");

        assertEquals(new BigDecimal("15000"), safeStrategy.getBalance(account),
                "Safe strategy must produce exactly ₹15000 after 10 × ₹500 deposits");
    }

    // ──────────────────────────────────────────────────────────────
    // 4. SAFE – Concurrent withdrawals with insufficient-balance guards
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SAFE: 5 concurrent withdrawals of ₹1500 from ₹10000 — balance invariant holds")
    void safeConcurrentWithdrawals() throws Exception {
        BankAccount account = new BankAccount("TEST-SW-001", "Test Safe Withdraw", new BigDecimal("10000"));

        int threadCount = 5;
        BigDecimal withdrawalAmount = new BigDecimal("1500");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    safeStrategy.withdraw(account, withdrawalAmount);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS),
                "Threads did not finish in time");

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Executor did not terminate in time");

        // At most floor(10000 / 1500) = 6 can succeed, but we only have 5 threads.
        int successfulWithdrawals = successes.get();
        int failedWithdrawals = failures.get();

        assertEquals(threadCount, successfulWithdrawals + failedWithdrawals,
                "All threads must report either success or failure");

        assertTrue(successfulWithdrawals <= 6,
                "At most 6 withdrawals of ₹1500 can succeed from ₹10000");

        // Invariant: successfulWithdrawals × 1500 + finalBalance = 10000
        BigDecimal finalBalance = safeStrategy.getBalance(account);
        BigDecimal totalWithdrawn = withdrawalAmount.multiply(BigDecimal.valueOf(successfulWithdrawals));

        assertEquals(new BigDecimal("10000"), totalWithdrawn.add(finalBalance),
                "Balance invariant violated: withdrawn (" + totalWithdrawn + ") + remaining (" + finalBalance + ") must equal ₹10000");
    }

    // ──────────────────────────────────────────────────────────────
    // 5. Deadlock demonstration
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deadlock simulator detects a deadlock scenario")
    @SuppressWarnings("unchecked")
    void deadlockDemonstration() {
        Map<String, Object> result = deadlockSimulator.demonstrateDeadlock();

        assertNotNull(result, "Deadlock result map must not be null");

        assertTrue((Boolean) result.get("deadlockDetected"),
                "Deadlock should be detected in the demonstration");

        List<String> events = (List<String>) result.get("events");
        assertNotNull(events, "Events list must be present");
        assertFalse(events.isEmpty(), "Events list must not be empty");

        assertNotNull(result.get("explanation"),
                "An explanation of the deadlock should be provided");
    }

    // ──────────────────────────────────────────────────────────────
    // 6. Deadlock prevention
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deadlock simulator demonstrates deadlock prevention via consistent lock ordering")
    @SuppressWarnings("unchecked")
    void deadlockPrevention() {
        Map<String, Object> result = deadlockSimulator.demonstrateDeadlockPrevention();

        assertNotNull(result, "Prevention result map must not be null");

        assertTrue((Boolean) result.get("deadlockPrevented"),
                "Deadlock prevention should succeed");

        List<String> events = (List<String>) result.get("events");
        assertNotNull(events, "Events list must be present");
        assertFalse(events.isEmpty(), "Events list must not be empty");

        assertNotNull(result.get("explanation"),
                "An explanation of the prevention strategy should be provided");
    }
}
