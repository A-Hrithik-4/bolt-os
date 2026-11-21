package com.bolt.controller;

import com.bolt.service.DeadlockService;
import com.bolt.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;
    private final DeadlockService deadlockService;

    public SimulationController(SimulationService simulationService, DeadlockService deadlockService) {
        this.simulationService = simulationService;
        this.deadlockService = deadlockService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runSimulation() {
        return ResponseEntity.ok(simulationService.runSimulation());
    }

    @PostMapping("/race-condition")
    public ResponseEntity<Map<String, Object>> runRaceCondition() {
        return ResponseEntity.ok(simulationService.runRaceConditionDemo());
    }

    @PostMapping("/deadlock")
    public ResponseEntity<Map<String, Object>> runDeadlock() {
        return ResponseEntity.ok(deadlockService.runDeadlockDemo());
    }

    @PostMapping("/deadlock-prevention")
    public ResponseEntity<Map<String, Object>> runDeadlockPrevention() {
        return ResponseEntity.ok(deadlockService.runDeadlockPreventionDemo());
    }

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> runComparison() {
        return ResponseEntity.ok(simulationService.runComparisonDemo());
    }

    @GetMapping("/{simulationId}")
    public ResponseEntity<Map<String, Object>> getSimulationResult(@PathVariable String simulationId) {
        return ResponseEntity.ok(simulationService.getSimulationResult(simulationId));
    }
}
