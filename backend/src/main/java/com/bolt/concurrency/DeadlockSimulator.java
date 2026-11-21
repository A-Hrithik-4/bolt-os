package com.bolt.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demonstrates deadlock scenarios and deadlock prevention techniques.
 *
 * IMPORTANT: Intentional deadlock experiments run in isolated daemon threads
 * with bounded execution time. Normal banking operations are never affected.
 */
@Component
public class DeadlockSimulator {

    private static final Logger log = LoggerFactory.getLogger(DeadlockSimulator.class);
    private static final int DEADLOCK_TIMEOUT_SECONDS = 5;

    /**
     * Demonstrates a deadlock caused by circular waiting.
     *
     * Thread A: locks account1 → tries to lock account2
     * Thread B: locks account2 → tries to lock account1
     *
     * Returns a detailed log of the deadlock demonstration.
     */
    public Map<String, Object> demonstrateDeadlock() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> eventLog = Collections.synchronizedList(new ArrayList<>());

        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();

        String account1 = "ACC-1001";
        String account2 = "ACC-1002";

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch completionLatch = new CountDownLatch(2);

        // Track if deadlock was detected
        final boolean[] deadlockDetected = {false};

        Thread threadA = new Thread(() -> {
            try {
                logEvent(eventLog, "DEADLOCK-THREAD-A", "Attempting to acquire lock on " + account1);
                lock1.lock();
                logEvent(eventLog, "DEADLOCK-THREAD-A", "✅ Acquired lock on " + account1);

                // Wait for Thread B to also acquire its first lock
                barrier.await(DEADLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Small delay to ensure both threads are ready
                Thread.sleep(100);

                logEvent(eventLog, "DEADLOCK-THREAD-A", "Attempting to acquire lock on " + account2 + " (Thread B holds this lock)");
                // Try to acquire with timeout to prevent permanent freeze
                boolean acquired = lock2.tryLock(DEADLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!acquired) {
                    logEvent(eventLog, "DEADLOCK-THREAD-A", "❌ DEADLOCK: Could not acquire lock on " + account2 + " — timed out");
                    deadlockDetected[0] = true;
                } else {
                    logEvent(eventLog, "DEADLOCK-THREAD-A", "Acquired lock on " + account2);
                    lock2.unlock();
                }
            } catch (Exception e) {
                logEvent(eventLog, "DEADLOCK-THREAD-A", "Exception: " + e.getMessage());
            } finally {
                if (lock1.isHeldByCurrentThread()) lock1.unlock();
                completionLatch.countDown();
            }
        }, "DEADLOCK-THREAD-A");

        Thread threadB = new Thread(() -> {
            try {
                logEvent(eventLog, "DEADLOCK-THREAD-B", "Attempting to acquire lock on " + account2);
                lock2.lock();
                logEvent(eventLog, "DEADLOCK-THREAD-B", "✅ Acquired lock on " + account2);

                // Wait for Thread A to also acquire its first lock
                barrier.await(DEADLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                Thread.sleep(100);

                logEvent(eventLog, "DEADLOCK-THREAD-B", "Attempting to acquire lock on " + account1 + " (Thread A holds this lock)");
                boolean acquired = lock1.tryLock(DEADLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!acquired) {
                    logEvent(eventLog, "DEADLOCK-THREAD-B", "❌ DEADLOCK: Could not acquire lock on " + account1 + " — timed out");
                    deadlockDetected[0] = true;
                } else {
                    logEvent(eventLog, "DEADLOCK-THREAD-B", "Acquired lock on " + account1);
                    lock1.unlock();
                }
            } catch (Exception e) {
                logEvent(eventLog, "DEADLOCK-THREAD-B", "Exception: " + e.getMessage());
            } finally {
                if (lock2.isHeldByCurrentThread()) lock2.unlock();
                completionLatch.countDown();
            }
        }, "DEADLOCK-THREAD-B");

        threadA.setDaemon(true);
        threadB.setDaemon(true);

        threadA.start();
        threadB.start();

        try {
            boolean completed = completionLatch.await(DEADLOCK_TIMEOUT_SECONDS * 2 + 2, TimeUnit.SECONDS);
            if (!completed) {
                logEvent(eventLog, "SYSTEM", "Deadlock demonstration timed out — interrupting threads");
                threadA.interrupt();
                threadB.interrupt();
                deadlockDetected[0] = true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        result.put("deadlockDetected", deadlockDetected[0]);
        result.put("account1", account1);
        result.put("account2", account2);
        result.put("threadA", "DEADLOCK-THREAD-A");
        result.put("threadB", "DEADLOCK-THREAD-B");
        result.put("explanation", deadlockDetected[0]
                ? "Circular wait demonstrated: Thread A held " + account1 + " and waited for " + account2 +
                  ", while Thread B held " + account2 + " and waited for " + account1 +
                  ". Neither could proceed — classic deadlock."
                : "No deadlock occurred in this run.");
        result.put("events", eventLog);

        log.info("Deadlock demonstration complete. Deadlock detected: {}", deadlockDetected[0]);
        return result;
    }

    /**
     * Demonstrates deadlock prevention using consistent lock ordering.
     *
     * Both threads acquire locks in the same order (sorted by account number),
     * eliminating circular wait and preventing deadlock.
     */
    public Map<String, Object> demonstrateDeadlockPrevention() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> eventLog = Collections.synchronizedList(new ArrayList<>());

        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();

        String account1 = "ACC-1001";
        String account2 = "ACC-1002";

        // Determine consistent lock order (alphabetical by account number)
        ReentrantLock firstLock = account1.compareTo(account2) < 0 ? lock1 : lock2;
        ReentrantLock secondLock = account1.compareTo(account2) < 0 ? lock2 : lock1;
        String firstAccount = account1.compareTo(account2) < 0 ? account1 : account2;
        String secondAccount = account1.compareTo(account2) < 0 ? account2 : account1;

        CountDownLatch completionLatch = new CountDownLatch(2);
        final boolean[] success = {true};

        Thread threadA = new Thread(() -> {
            try {
                logEvent(eventLog, "PREVENTION-THREAD-A",
                        "Using consistent lock ordering: " + firstAccount + " → " + secondAccount);
                logEvent(eventLog, "PREVENTION-THREAD-A", "Attempting to acquire lock on " + firstAccount);
                firstLock.lock();
                logEvent(eventLog, "PREVENTION-THREAD-A", "✅ Acquired lock on " + firstAccount);

                Thread.sleep(100);

                logEvent(eventLog, "PREVENTION-THREAD-A", "Attempting to acquire lock on " + secondAccount);
                secondLock.lock();
                logEvent(eventLog, "PREVENTION-THREAD-A", "✅ Acquired lock on " + secondAccount);

                logEvent(eventLog, "PREVENTION-THREAD-A",
                        "✅ Transfer completed successfully — both locks acquired in order");

                secondLock.unlock();
                firstLock.unlock();
            } catch (Exception e) {
                success[0] = false;
                logEvent(eventLog, "PREVENTION-THREAD-A", "Exception: " + e.getMessage());
            } finally {
                if (firstLock.isHeldByCurrentThread()) firstLock.unlock();
                if (secondLock.isHeldByCurrentThread()) secondLock.unlock();
                completionLatch.countDown();
            }
        }, "PREVENTION-THREAD-A");

        Thread threadB = new Thread(() -> {
            try {
                logEvent(eventLog, "PREVENTION-THREAD-B",
                        "Using consistent lock ordering: " + firstAccount + " → " + secondAccount);
                logEvent(eventLog, "PREVENTION-THREAD-B", "Attempting to acquire lock on " + firstAccount);
                firstLock.lock();
                logEvent(eventLog, "PREVENTION-THREAD-B", "✅ Acquired lock on " + firstAccount);

                Thread.sleep(100);

                logEvent(eventLog, "PREVENTION-THREAD-B", "Attempting to acquire lock on " + secondAccount);
                secondLock.lock();
                logEvent(eventLog, "PREVENTION-THREAD-B", "✅ Acquired lock on " + secondAccount);

                logEvent(eventLog, "PREVENTION-THREAD-B",
                        "✅ Transfer completed successfully — both locks acquired in order");

                secondLock.unlock();
                firstLock.unlock();
            } catch (Exception e) {
                success[0] = false;
                logEvent(eventLog, "PREVENTION-THREAD-B", "Exception: " + e.getMessage());
            } finally {
                if (firstLock.isHeldByCurrentThread()) firstLock.unlock();
                if (secondLock.isHeldByCurrentThread()) secondLock.unlock();
                completionLatch.countDown();
            }
        }, "PREVENTION-THREAD-B");

        threadA.setDaemon(true);
        threadB.setDaemon(true);

        threadA.start();
        threadB.start();

        try {
            boolean completed = completionLatch.await(DEADLOCK_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);
            if (!completed) {
                threadA.interrupt();
                threadB.interrupt();
                success[0] = false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        result.put("deadlockPrevented", success[0]);
        result.put("technique", "Consistent Lock Ordering");
        result.put("lockOrder", firstAccount + " → " + secondAccount);
        result.put("explanation", success[0]
                ? "Both threads acquired locks in the same order (" + firstAccount + " → " + secondAccount +
                  "), eliminating circular wait. No deadlock occurred."
                : "Unexpected failure during prevention demonstration.");
        result.put("events", eventLog);

        log.info("Deadlock prevention demonstration complete. Success: {}", success[0]);
        return result;
    }

    private void logEvent(List<Map<String, Object>> eventLog, String threadName, String message) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", System.currentTimeMillis());
        event.put("thread", threadName);
        event.put("message", message);
        eventLog.add(event);
        log.info("[{}] {}", threadName, message);
    }
}
