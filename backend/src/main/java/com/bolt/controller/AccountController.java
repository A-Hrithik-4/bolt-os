package com.bolt.controller;

import com.bolt.model.BankAccount;
import com.bolt.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<BankAccount>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccount> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String accountId) {
        BigDecimal balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(Map.of(
                "accountNumber", accountId,
                "balance", balance));
    }

    @PostMapping
    public ResponseEntity<BankAccount> createAccount(@RequestBody Map<String, Object> request) {
        String accountNumber = (String) request.get("accountNumber");
        String customerName = (String) request.get("customerName");
        BigDecimal initialBalance = request.get("initialBalance") != null
                ? new BigDecimal(request.get("initialBalance").toString())
                : BigDecimal.ZERO;

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        BankAccount account = accountService.createAccount(accountNumber, customerName, initialBalance);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }
}
