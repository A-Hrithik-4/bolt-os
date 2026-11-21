package com.bolt.service;

import com.bolt.concurrency.BankingExecutionStrategy;
import com.bolt.concurrency.SafeBankingStrategy;
import com.bolt.concurrency.UnsafeBankingStrategy;
import com.bolt.dto.DepositRequest;
import com.bolt.dto.TransferRequest;
import com.bolt.dto.TransactionResponse;
import com.bolt.dto.WithdrawalRequest;
import com.bolt.enums.ExecutionMode;
import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import com.bolt.model.BankAccount;
import com.bolt.model.Transaction;
import com.bolt.util.TransactionIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core banking operations service.
 * Delegates to the currently active BankingExecutionStrategy (SAFE or UNSAFE).
 *
 * Database Concurrency Note:
 * ---
 * The in-memory race-condition demonstration uses UnsafeBankingStrategy to show
 * how thread interleaving causes lost updates on a shared in-memory object.
 * For normal database-backed operations, BankAccount has a @Version field
 * (optimistic locking). In SAFE mode, ReentrantLock serializes threads in the
 * JVM, and @Version provides a database-level safety net — if a stale write
 * slips through, JPA throws OptimisticLockException. In UNSAFE mode, we
 * deliberately skip both mechanisms to demonstrate the problem.
 */
@Service
public class BankingService {

    private static final Logger log = LoggerFactory.getLogger(BankingService.class);

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final SafeBankingStrategy safeStrategy;
    private final UnsafeBankingStrategy unsafeStrategy;
    private final AtomicReference<ExecutionMode> currentMode = new AtomicReference<>(ExecutionMode.SAFE);

    public BankingService(AccountService accountService,
                          TransactionService transactionService,
                          SafeBankingStrategy safeStrategy,
                          UnsafeBankingStrategy unsafeStrategy) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.safeStrategy = safeStrategy;
        this.unsafeStrategy = unsafeStrategy;
    }

    public ExecutionMode getCurrentMode() {
        return currentMode.get();
    }

    public void setCurrentMode(ExecutionMode mode) {
        ExecutionMode oldMode = currentMode.getAndSet(mode);
        log.info("Execution mode changed: {} → {}", oldMode, mode);
    }

    /**
     * Returns the strategy for the current mode.
     * The mode is captured at the start of a transaction and used throughout.
     */
    public BankingExecutionStrategy getStrategy() {
        return currentMode.get() == ExecutionMode.SAFE ? safeStrategy : unsafeStrategy;
    }

    public BankingExecutionStrategy getStrategy(ExecutionMode mode) {
        return mode == ExecutionMode.SAFE ? safeStrategy : unsafeStrategy;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        // Capture mode at transaction start
        ExecutionMode mode = currentMode.get();
        validateAmount(request.getAmount());

        long start = System.currentTimeMillis();
        Transaction txn = createTransaction(TransactionType.DEPOSIT, request.getAccountNumber(),
                null, request.getAmount(), mode, request.getChannel());

        ReentrantLock lock = (mode == ExecutionMode.SAFE) ? safeStrategy.getLock(request.getAccountNumber()) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            BankingExecutionStrategy strategy = getStrategy(mode);
            BankAccount account = accountService.getAccount(request.getAccountNumber());
            strategy.deposit(account, request.getAmount());
            accountService.saveAccount(account);
            txn.setStatus(TransactionStatus.SUCCESS);
            txn.setDescription("Deposit of ₹" + request.getAmount().toPlainString() +
                    " to account " + request.getAccountNumber() + " completed");
        } catch (Exception e) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setErrorMessage(e.getMessage());
            txn.setDescription("Deposit failed: " + e.getMessage());
            throw e;
        } finally {
            if (lock != null) {
                lock.unlock();
            }
            txn.setDurationMs(System.currentTimeMillis() - start);
            transactionService.saveTransaction(txn);
        }
        return TransactionResponse.fromEntity(txn);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request) {
        ExecutionMode mode = currentMode.get();
        validateAmount(request.getAmount());

        long start = System.currentTimeMillis();
        Transaction txn = createTransaction(TransactionType.WITHDRAWAL, request.getAccountNumber(),
                null, request.getAmount(), mode, request.getChannel());

        ReentrantLock lock = (mode == ExecutionMode.SAFE) ? safeStrategy.getLock(request.getAccountNumber()) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            BankingExecutionStrategy strategy = getStrategy(mode);
            BankAccount account = accountService.getAccount(request.getAccountNumber());
            strategy.withdraw(account, request.getAmount());
            accountService.saveAccount(account);
            txn.setStatus(TransactionStatus.SUCCESS);
            txn.setDescription("Withdrawal of ₹" + request.getAmount().toPlainString() +
                    " from account " + request.getAccountNumber() + " completed");
        } catch (Exception e) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setErrorMessage(e.getMessage());
            txn.setDescription("Withdrawal failed: " + e.getMessage());
            throw e;
        } finally {
            if (lock != null) {
                lock.unlock();
            }
            txn.setDurationMs(System.currentTimeMillis() - start);
            transactionService.saveTransaction(txn);
        }
        return TransactionResponse.fromEntity(txn);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        ExecutionMode mode = currentMode.get();
        validateAmount(request.getAmount());
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        long start = System.currentTimeMillis();
        Transaction txn = createTransaction(TransactionType.TRANSFER, request.getSourceAccountNumber(),
                request.getDestinationAccountNumber(), request.getAmount(), mode, request.getChannel());

        ReentrantLock firstLock = null;
        ReentrantLock secondLock = null;

        if (mode == ExecutionMode.SAFE) {
            String src = request.getSourceAccountNumber();
            String dst = request.getDestinationAccountNumber();
            if (src.compareTo(dst) < 0) {
                firstLock = safeStrategy.getLock(src);
                secondLock = safeStrategy.getLock(dst);
            } else {
                firstLock = safeStrategy.getLock(dst);
                secondLock = safeStrategy.getLock(src);
            }
            firstLock.lock();
            secondLock.lock();
        }

        try {
            BankingExecutionStrategy strategy = getStrategy(mode);
            BankAccount source = accountService.getAccount(request.getSourceAccountNumber());
            BankAccount destination = accountService.getAccount(request.getDestinationAccountNumber());

            strategy.transfer(source, destination, request.getAmount());
            accountService.saveAccount(source);
            accountService.saveAccount(destination);
            txn.setStatus(TransactionStatus.SUCCESS);
            txn.setDescription("Transfer of ₹" + request.getAmount().toPlainString() +
                    " from " + request.getSourceAccountNumber() +
                    " to " + request.getDestinationAccountNumber() + " completed");
        } catch (Exception e) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setErrorMessage(e.getMessage());
            txn.setDescription("Transfer failed: " + e.getMessage());
            throw e;
        } finally {
            if (secondLock != null) {
                secondLock.unlock();
            }
            if (firstLock != null) {
                firstLock.unlock();
            }
            txn.setDurationMs(System.currentTimeMillis() - start);
            transactionService.saveTransaction(txn);
        }
        return TransactionResponse.fromEntity(txn);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private Transaction createTransaction(TransactionType type, String source, String dest,
                                           BigDecimal amount, ExecutionMode mode, String channel) {
        Transaction txn = new Transaction();
        txn.setTransactionId(TransactionIdGenerator.generate());
        txn.setTransactionType(type);
        txn.setSourceAccountNumber(source);
        txn.setDestinationAccountNumber(dest);
        txn.setAmount(amount);
        txn.setExecutionMode(mode);
        txn.setThreadName(Thread.currentThread().getName());
        txn.setChannel(channel != null ? channel : "ONLINE");
        txn.setTimestamp(LocalDateTime.now());
        txn.setStatus(TransactionStatus.PENDING);
        return txn;
    }
}
