package com.bolt.controller;

import com.bolt.enums.ExecutionMode;
import com.bolt.service.BankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mode")
public class ModeController {

    private final BankingService bankingService;

    public ModeController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentMode() {
        return ResponseEntity.ok(Map.of("mode", bankingService.getCurrentMode().name()));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> setMode(@RequestBody Map<String, String> request) {
        String modeStr = request.get("mode");
        if (modeStr == null || modeStr.isBlank()) {
            throw new IllegalArgumentException("Mode is required. Valid values: SAFE, UNSAFE");
        }
        try {
            ExecutionMode mode = ExecutionMode.valueOf(modeStr.toUpperCase());
            bankingService.setCurrentMode(mode);
            return ResponseEntity.ok(Map.of(
                    "mode", mode.name(),
                    "message", "Execution mode changed to " + mode.name()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid mode: " + modeStr + ". Valid values: SAFE, UNSAFE");
        }
    }
}
