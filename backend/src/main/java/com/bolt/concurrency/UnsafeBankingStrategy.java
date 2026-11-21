package com.bolt.concurrency;

import com.bolt.model.BankAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * UNSAFE banking strategy — no synchronization.
 *
 * This deliberately omits all locks and synchronization to demonstrate
 * race conditions, lost updates, and inconsistent balances when multiple
 * threads access the same account concurrently.
 *
 * NOT for production use — this is an educational demonstration.
 */
@Component("unsafeStrategy")
public class UnsafeBankingStrategy implements BankingExecutionStrategy {

    @Override
    public BigDecimal deposit(BankAccount account, BigDecimal amount) {
        // Read current balance (no lock — another thread can read the same value)
        BigDecimal currentBalance = account.getBalance();

        // Simulate processing delay to widen the race window
        simulateProcessingDelay();

        // Write updated balance (may overwrite another thread's update — lost update)
        BigDecimal newBalance = currentBalance.add(amount);
        account.setBalance(newBalance);
        return newBalance;
    }

    @Override
    public BigDecimal withdraw(BankAccount account, BigDecimal amount) {
        // Read current balance (no lock)
        BigDecimal currentBalance = account.getBalance();

        // Simulate processing delay to widen the race window
        simulateProcessingDelay();

        // Check balance — but another thread may have already withdrawn
        // Both threads see sufficient balance and proceed
        if (currentBalance.compareTo(amount) < 0) {
            throw new com.bolt.exception.InsufficientBalanceException(
                    account.getAccountNumber(),
                    amount.toPlainString(),
                    currentBalance.toPlainString());
        }

        // Write updated balance (may allow double withdrawal)
        BigDecimal newBalance = currentBalance.subtract(amount);
        account.setBalance(newBalance);
        return newBalance;
    }

    @Override
    public BigDecimal transfer(BankAccount source, BankAccount destination, BigDecimal amount) {
        // No synchronization — operations are not atomic
        BigDecimal sourceBalance = source.getBalance();

        simulateProcessingDelay();

        if (sourceBalance.compareTo(amount) < 0) {
            throw new com.bolt.exception.InsufficientBalanceException(
                    source.getAccountNumber(),
                    amount.toPlainString(),
                    sourceBalance.toPlainString());
        }

        source.setBalance(sourceBalance.subtract(amount));

        simulateProcessingDelay();

        BigDecimal destBalance = destination.getBalance();
        destination.setBalance(destBalance.add(amount));

        return source.getBalance();
    }

    @Override
    public BigDecimal getBalance(BankAccount account) {
        return account.getBalance();
    }

    @Override
    public String getModeName() {
        return "UNSAFE";
    }

    /**
     * Introduces a small delay to increase the probability of thread interleaving.
     * This makes race conditions more reliably observable.
     */
    private void simulateProcessingDelay() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
