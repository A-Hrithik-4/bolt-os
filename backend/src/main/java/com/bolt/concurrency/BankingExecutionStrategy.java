package com.bolt.concurrency;

import com.bolt.model.BankAccount;
import java.math.BigDecimal;

/**
 * Strategy interface for banking operations.
 * Both SAFE and UNSAFE modes implement this interface,
 * allowing mode switching without duplicating the banking application.
 */
public interface BankingExecutionStrategy {

    /**
     * Deposit money into an account.
     * @return the updated balance
     */
    BigDecimal deposit(BankAccount account, BigDecimal amount);

    /**
     * Withdraw money from an account.
     * @return the updated balance
     * @throws com.bolt.exception.InsufficientBalanceException if balance is insufficient
     */
    BigDecimal withdraw(BankAccount account, BigDecimal amount);

    /**
     * Transfer money from source to destination atomically.
     * @return the source account's updated balance
     */
    BigDecimal transfer(BankAccount source, BankAccount destination, BigDecimal amount);

    /**
     * Retrieve the current balance.
     */
    BigDecimal getBalance(BankAccount account);

    /**
     * Returns the name of this strategy for logging.
     */
    String getModeName();
}
