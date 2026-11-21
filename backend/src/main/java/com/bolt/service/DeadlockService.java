package com.bolt.service;

import com.bolt.concurrency.DeadlockSimulator;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service wrapping deadlock demonstration and prevention.
 * Stores results for later retrieval.
 */
@Service
public class DeadlockService {

    private final DeadlockSimulator deadlockSimulator;
    private final Map<String, Map<String, Object>> deadlockResults = new ConcurrentHashMap<>();
    private Map<String, Object> lastDeadlockResult;
    private Map<String, Object> lastPreventionResult;

    public DeadlockService(DeadlockSimulator deadlockSimulator) {
        this.deadlockSimulator = deadlockSimulator;
    }

    public Map<String, Object> runDeadlockDemo() {
        Map<String, Object> result = deadlockSimulator.demonstrateDeadlock();
        this.lastDeadlockResult = result;
        return result;
    }

    public Map<String, Object> runDeadlockPreventionDemo() {
        Map<String, Object> result = deadlockSimulator.demonstrateDeadlockPrevention();
        this.lastPreventionResult = result;
        return result;
    }

    public Map<String, Object> getLastDeadlockResult() {
        return lastDeadlockResult;
    }

    public Map<String, Object> getLastPreventionResult() {
        return lastPreventionResult;
    }
}
