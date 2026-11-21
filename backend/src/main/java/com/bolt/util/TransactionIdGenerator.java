package com.bolt.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique transaction IDs of the form TXN-000001.
 * Thread-safe via AtomicLong.
 */
public final class TransactionIdGenerator {

    private static final AtomicLong COUNTER = new AtomicLong(0);

    private TransactionIdGenerator() {}

    public static String generate() {
        return String.format("TXN-%06d", COUNTER.incrementAndGet());
    }

    /** Reset counter — used only in tests. */
    public static void reset() {
        COUNTER.set(0);
    }
}
