package com.bolt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Entity representing a bank account in the BOLT system.
 * Uses optimistic locking via @Version for database-level concurrency control.
 */
@Entity
@Table(name = "bank_accounts", indexes = {
    @Index(name = "idx_account_number", columnList = "accountNumber", unique = true)
})
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @Column(nullable = false)
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false)
    private String accountStatus = "ACTIVE";

    /**
     * Optimistic locking version field.
     * JPA increments this on every UPDATE; a stale write throws
     * OptimisticLockException, keeping DB-level data consistent in SAFE mode.
     */
    @Version
    private Long version;

    public BankAccount() {}

    public BankAccount(String accountNumber, String customerName, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.balance = balance;
        this.accountStatus = "ACTIVE";
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
