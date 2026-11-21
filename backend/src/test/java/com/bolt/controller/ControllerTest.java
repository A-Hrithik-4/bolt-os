package com.bolt.controller;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the BOLT REST API controllers.
 *
 * Tests cover AccountController, TransactionController, ModeController,
 * DashboardController endpoints using MockMvc with JSON assertions.
 *
 * Tests are ordered to ensure deposit occurs before withdrawal so that
 * balance checks remain valid throughout the test run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ───────────────────────────── Account Tests ─────────────────────────────

    @Test
    @Order(1)
    void getAllAccounts_returnsAllFourAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @Order(2)
    void getAccountById_returnsAliceAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1001"))
                .andExpect(jsonPath("$.customerName").value("Alice"));
    }

    @Test
    @Order(3)
    void getAccountBalance_returnsInitialBalance() throws Exception {
        mockMvc.perform(get("/api/accounts/1001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000));
    }

    @Test
    @Order(4)
    void createAccount_returnsCreatedStatus() throws Exception {
        String newAccountJson = """
                {
                    "accountNumber": "1005",
                    "customerName": "Eve",
                    "balance": 5000
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAccountJson))
                .andExpect(status().isCreated());
    }

    // ──────────────────────────── Transaction Tests ──────────────────────────

    @Test
    @Order(5)
    void deposit_succeeds() throws Exception {
        String depositJson = """
                {
                    "accountNumber": "1001",
                    "amount": 500,
                    "channel": "ATM"
                }
                """;

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @Order(6)
    void withdraw_succeeds() throws Exception {
        // After the deposit of 500, account 1001 has 10500. Withdrawing 200 is safe.
        String withdrawJson = """
                {
                    "accountNumber": "1001",
                    "amount": 200,
                    "channel": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @Order(7)
    void withdraw_insufficientBalance_returnsBadRequest() throws Exception {
        // Account 1001 has 10300 at this point. Requesting 999999 should fail.
        String withdrawJson = """
                {
                    "accountNumber": "1001",
                    "amount": 999999,
                    "channel": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdrawJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    void transfer_succeeds() throws Exception {
        String transferJson = """
                {
                    "sourceAccountNumber": "1001",
                    "destinationAccountNumber": "1002",
                    "amount": 1000,
                    "channel": "ONLINE"
                }
                """;

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // ─────────────────────────────── Mode Tests ──────────────────────────────

    @Test
    @Order(9)
    void getMode_returnsDefaultSafeMode() throws Exception {
        mockMvc.perform(get("/api/mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SAFE"));
    }

    @Test
    @Order(10)
    void setMode_toUnsafe_succeeds() throws Exception {
        String modeJson = """
                {
                    "mode": "UNSAFE"
                }
                """;

        mockMvc.perform(put("/api/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("UNSAFE"));
    }

    @Test
    @Order(11)
    void setMode_backToSafe_succeeds() throws Exception {
        String modeJson = """
                {
                    "mode": "SAFE"
                }
                """;

        mockMvc.perform(put("/api/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SAFE"));
    }

    // ──────────────────────────── Dashboard Tests ────────────────────────────

    @Test
    @Order(12)
    void getDashboardSummary_returnsTotalAccounts() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").value(5)); // 4 initial + 1 created in test
    }

    @Test
    @Order(13)
    void getDashboardStatistics_returnsOk() throws Exception {
        mockMvc.perform(get("/api/dashboard/statistics"))
                .andExpect(status().isOk());
    }

    // ──────────────────────────── Simulation Tests ───────────────────────────

    @Test
    @Order(14)
    void runRaceCondition_returnsResultWithDetectedFlag() throws Exception {
        mockMvc.perform(post("/api/simulation/race-condition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").exists())
                .andExpect(jsonPath("$.raceConditionDetected").isBoolean())
                .andExpect(jsonPath("$.initialBalance").value(10000));
    }

    @Test
    @Order(15)
    void runDeadlock_returnsDeadlockDetected() throws Exception {
        mockMvc.perform(post("/api/simulation/deadlock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlockDetected").value(true));
    }

    @Test
    @Order(16)
    void runDeadlockPrevention_returnsDeadlockPrevented() throws Exception {
        mockMvc.perform(post("/api/simulation/deadlock-prevention"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlockPrevented").value(true));
    }

    @Test
    @Order(17)
    void runComparison_returnsSideBySideReports() throws Exception {
        mockMvc.perform(post("/api/simulation/compare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsafeMode").exists())
                .andExpect(jsonPath("$.safeMode").exists())
                .andExpect(jsonPath("$.unsafeMode.lostUpdatesDetected").value(1))
                .andExpect(jsonPath("$.safeMode.lostUpdatesDetected").value(0));
    }

    @Test
    @Order(18)
    void getNonExistentAccount_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
