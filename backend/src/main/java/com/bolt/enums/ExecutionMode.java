package com.bolt.enums;

/**
 * Execution mode for banking operations.
 * SAFE mode uses synchronization; UNSAFE mode demonstrates race conditions.
 */
public enum ExecutionMode {
    SAFE,
    UNSAFE
}
