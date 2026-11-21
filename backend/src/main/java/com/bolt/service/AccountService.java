package com.bolt.service;

import com.bolt.exception.AccountNotFoundException;
import com.bolt.model.BankAccount;
import com.bolt.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for managing bank accounts.
 * Handles CRUD operations and sample data initialization.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<BankAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    public BankAccount getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public BigDecimal getBalance(String accountNumber) {
        return getAccount(accountNumber).getBalance();
    }

    @Transactional
    public BankAccount createAccount(String accountNumber, String customerName, BigDecimal initialBalance) {
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new IllegalArgumentException("Account already exists: " + accountNumber);
        }
        if (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        BankAccount account = new BankAccount(accountNumber, customerName,
                initialBalance != null ? initialBalance : BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    @Transactional
    public BankAccount saveAccount(BankAccount account) {
        return accountRepository.save(account);
    }

    public long getAccountCount() {
        return accountRepository.count();
    }

    /**
     * Initialize sample accounts if they don't already exist.
     * Prevents duplicates after application restarts.
     */
    @Transactional
    public void initializeSampleAccounts() {
        createIfNotExists("1001", "Alice", new BigDecimal("10000.0000"));
        createIfNotExists("1002", "Bob", new BigDecimal("15000.0000"));
        createIfNotExists("1003", "Charlie", new BigDecimal("20000.0000"));
        createIfNotExists("1004", "David", new BigDecimal("12000.0000"));
    }

    private void createIfNotExists(String accountNumber, String customerName, BigDecimal balance) {
        if (!accountRepository.existsByAccountNumber(accountNumber)) {
            accountRepository.save(new BankAccount(accountNumber, customerName, balance));
        }
    }
}
