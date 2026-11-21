package com.bolt.concurrency;

import com.bolt.exception.InsufficientBalanceException;
import com.bolt.model.BankAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SAFE banking strategy — full synchronization using ReentrantLock.
 *
 * Key design decisions:
 * 1. Each account gets its own ReentrantLock (fine-grained locking).
 * 2. For transfers involving two accounts, locks are acquired in a consistent
 *    order (sorted by account number) to prevent deadlocks.
 * 3. All balance reads/writes happen while holding the appropriate lock,
 *    guaranteeing mutual exclusion and preventing lost updates.
 */
@Component("safeStrategy")
public class SafeBankingStrategy implements BankingExecutionStrategy {

    /** Per-account locks, keyed by account number. */
    private final Map<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    public ReentrantLock getLock(String accountNumber) {
        return accountLocks.computeIfAbsent(accountNumber, k -> new ReentrantLock(true));
    }

    @Override
    public BigDecimal deposit(BankAccount account, BigDecimal amount) {
        ReentrantLock lock = getLock(account.getAccountNumber());
        lock.lock();
        try {
            BigDecimal newBalance = account.getBalance().add(amount);
            account.setBalance(newBalance);
            return newBalance;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BigDecimal withdraw(BankAccount account, BigDecimal amount) {
        ReentrantLock lock = getLock(account.getAccountNumber());
        lock.lock();
        try {
            BigDecimal currentBalance = account.getBalance();
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        account.getAccountNumber(),
                        amount.toPlainString(),
                        currentBalance.toPlainString());
            }
            BigDecimal newBalance = currentBalance.subtract(amount);
            account.setBalance(newBalance);
            return newBalance;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BigDecimal transfer(BankAccount source, BankAccount destination, BigDecimal amount) {
        // Consistent lock ordering by account number to prevent deadlocks
        String sourceNum = source.getAccountNumber();
        String destNum = destination.getAccountNumber();

        ReentrantLock firstLock, secondLock;
        if (sourceNum.compareTo(destNum) < 0) {
            firstLock = getLock(sourceNum);
            secondLock = getLock(destNum);
        } else {
            firstLock = getLock(destNum);
            secondLock = getLock(sourceNum);
        }

        firstLock.lock();
        try {
            secondLock.lock();
            try {
                BigDecimal currentBalance = source.getBalance();
                if (currentBalance.compareTo(amount) < 0) {
                    throw new InsufficientBalanceException(
                            source.getAccountNumber(),
                            amount.toPlainString(),
                            currentBalance.toPlainString());
                }

                source.setBalance(currentBalance.subtract(amount));
                destination.setBalance(destination.getBalance().add(amount));

                return source.getBalance();
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }

    @Override
    public BigDecimal getBalance(BankAccount account) {
        ReentrantLock lock = getLock(account.getAccountNumber());
        lock.lock();
        try {
            return account.getBalance();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getModeName() {
        return "SAFE";
    }

    /** Clear all locks — useful in tests. */
    public void clearLocks() {
        accountLocks.clear();
    }
}
