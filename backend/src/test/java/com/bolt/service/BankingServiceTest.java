package com.bolt.service;

import com.bolt.concurrency.SafeBankingStrategy;
import com.bolt.dto.DepositRequest;
import com.bolt.dto.TransactionResponse;
import com.bolt.dto.TransferRequest;
import com.bolt.dto.WithdrawalRequest;
import com.bolt.enums.ExecutionMode;
import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import com.bolt.exception.AccountNotFoundException;
import com.bolt.exception.InsufficientBalanceException;
import com.bolt.model.BankAccount;
import com.bolt.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for the BOLT Banking Service.
 *
 * Uses @SpringBootTest to bring up the full application context with H2 in-memory database.
 * Sample accounts are auto-initialized via CommandLineRunner:
 *   1001 → Alice   → ₹10,000
 *   1002 → Bob     → ₹15,000
 *   1003 → Charlie → ₹20,000
 *   1004 → David   → ₹12,000
 */
@SpringBootTest
class BankingServiceTest {

    @Autowired
    private BankingService bankingService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private SafeBankingStrategy safeStrategy;

    @BeforeEach
    void ensureSafeMode() {
        bankingService.setCurrentMode(ExecutionMode.SAFE);
    }

    // ══════════════════════════════════════════════════════════════
    //  1. Account Creation Tests
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Account Creation")
    class AccountCreationTests {

        @Test
        @DisplayName("Sample accounts are initialized on startup")
        void sampleAccountsExist() {
            BankAccount alice = accountService.getAccount("1001");
            assertEquals("Alice", alice.getCustomerName());
            assertEquals(0, new BigDecimal("10000.0000").compareTo(alice.getBalance()),
                    "Alice's initial balance should be ₹10,000");

            BankAccount bob = accountService.getAccount("1002");
            assertEquals("Bob", bob.getCustomerName());
            assertEquals(0, new BigDecimal("15000.0000").compareTo(bob.getBalance()),
                    "Bob's initial balance should be ₹15,000");

            BankAccount charlie = accountService.getAccount("1003");
            assertEquals("Charlie", charlie.getCustomerName());
            assertEquals(0, new BigDecimal("20000.0000").compareTo(charlie.getBalance()),
                    "Charlie's initial balance should be ₹20,000");

            BankAccount david = accountService.getAccount("1004");
            assertEquals("David", david.getCustomerName());
            assertEquals(0, new BigDecimal("12000.0000").compareTo(david.getBalance()),
                    "David's initial balance should be ₹12,000");
        }

        @Test
        @DisplayName("Create a new account with initial balance")
        @Transactional
        void createNewAccount() {
            BankAccount newAccount = accountService.createAccount("9999", "TestUser",
                    new BigDecimal("5000.00"));
            assertEquals("9999", newAccount.getAccountNumber());
            assertEquals("TestUser", newAccount.getCustomerName());
            assertEquals(0, new BigDecimal("5000.00").compareTo(newAccount.getBalance()));
            assertEquals("ACTIVE", newAccount.getAccountStatus());
        }

        @Test
        @DisplayName("Create account with zero initial balance")
        @Transactional
        void createAccountWithZeroBalance() {
            BankAccount account = accountService.createAccount("8888", "ZeroUser", BigDecimal.ZERO);
            assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        }

        @Test
        @DisplayName("Create account with null balance defaults to zero")
        @Transactional
        void createAccountWithNullBalanceDefaultsToZero() {
            BankAccount account = accountService.createAccount("7777", "NullBal", null);
            assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
        }

        @Test
        @DisplayName("Duplicate account number is rejected")
        @Transactional
        void duplicateAccountRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    accountService.createAccount("1001", "Duplicate", new BigDecimal("1000")));
            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Negative initial balance is rejected")
        @Transactional
        void negativeBalanceRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    accountService.createAccount("6666", "Negative", new BigDecimal("-500")));
            assertTrue(ex.getMessage().contains("negative"));
        }

        @Test
        @DisplayName("Non-existent account throws AccountNotFoundException")
        void nonExistentAccountThrows() {
            assertThrows(AccountNotFoundException.class, () ->
                    accountService.getAccount("0000"));
        }

        @Test
        @DisplayName("getBalance returns correct balance for existing account")
        void getBalanceWorks() {
            BigDecimal balance = accountService.getBalance("1001");
            assertEquals(0, new BigDecimal("10000.0000").compareTo(balance));
        }

        @Test
        @DisplayName("getAllAccounts returns at least the 4 sample accounts")
        void getAllAccountsReturnsAll() {
            List<BankAccount> accounts = accountService.getAllAccounts();
            assertTrue(accounts.size() >= 4,
                    "Should have at least 4 sample accounts, found: " + accounts.size());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  2. Deposit in SAFE Mode
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Deposit (SAFE Mode)")
    class DepositSafeModeTests {

        @Test
        @DisplayName("Deposit adds amount to account balance")
        @Transactional
        void depositAddsToBalance() {
            BigDecimal initialBalance = accountService.getBalance("1001");
            BigDecimal depositAmount = new BigDecimal("2500.00");

            TransactionResponse response = bankingService.deposit(
                    new DepositRequest("1001", depositAmount));

            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
            assertEquals(0, depositAmount.compareTo(response.getAmount()));
            assertEquals(ExecutionMode.SAFE, response.getExecutionMode());

            BigDecimal expectedBalance = initialBalance.add(depositAmount);
            BigDecimal actualBalance = accountService.getBalance("1001");
            assertEquals(0, expectedBalance.compareTo(actualBalance),
                    "Balance after deposit should be " + expectedBalance + " but was " + actualBalance);
        }

        @Test
        @DisplayName("Deposit to non-existent account fails")
        void depositToNonExistentAccount() {
            assertThrows(AccountNotFoundException.class, () ->
                    bankingService.deposit(new DepositRequest("0000", new BigDecimal("100"))));
        }

        @Test
        @DisplayName("Deposit of zero or negative amount fails")
        void depositInvalidAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.deposit(new DepositRequest("1001", BigDecimal.ZERO)));
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.deposit(new DepositRequest("1001", new BigDecimal("-100"))));
        }

        @Test
        @DisplayName("Deposit of null amount fails")
        void depositNullAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.deposit(new DepositRequest("1001", null)));
        }

        @Test
        @DisplayName("Multiple sequential deposits accumulate correctly")
        @Transactional
        void multipleDepositsAccumulate() {
            BigDecimal initial = accountService.getBalance("1002");

            bankingService.deposit(new DepositRequest("1002", new BigDecimal("1000")));
            bankingService.deposit(new DepositRequest("1002", new BigDecimal("2000")));
            bankingService.deposit(new DepositRequest("1002", new BigDecimal("3000")));

            BigDecimal expected = initial.add(new BigDecimal("6000"));
            BigDecimal actual = accountService.getBalance("1002");
            assertEquals(0, expected.compareTo(actual),
                    "After 3 deposits totalling ₹6000, balance should be " + expected);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  3. Withdrawal in SAFE Mode
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Withdrawal (SAFE Mode)")
    class WithdrawalSafeModeTests {

        @Test
        @DisplayName("Withdrawal subtracts amount from account balance")
        @Transactional
        void withdrawalSubtractsFromBalance() {
            BigDecimal initialBalance = accountService.getBalance("1003");
            BigDecimal withdrawalAmount = new BigDecimal("5000.00");

            TransactionResponse response = bankingService.withdraw(
                    new WithdrawalRequest("1003", withdrawalAmount));

            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
            assertEquals(ExecutionMode.SAFE, response.getExecutionMode());

            BigDecimal expectedBalance = initialBalance.subtract(withdrawalAmount);
            BigDecimal actualBalance = accountService.getBalance("1003");
            assertEquals(0, expectedBalance.compareTo(actualBalance),
                    "Balance after withdrawal should be " + expectedBalance);
        }

        @Test
        @DisplayName("Withdraw exact balance leaves zero")
        @Transactional
        void withdrawExactBalanceLeavesZero() {
            BigDecimal fullBalance = accountService.getBalance("1004");

            TransactionResponse response = bankingService.withdraw(
                    new WithdrawalRequest("1004", fullBalance));

            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            BigDecimal remaining = accountService.getBalance("1004");
            assertEquals(0, BigDecimal.ZERO.compareTo(remaining),
                    "Balance should be zero after withdrawing full amount");
        }

        @Test
        @DisplayName("Withdrawal of zero or negative amount fails")
        void withdrawInvalidAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.withdraw(new WithdrawalRequest("1001", BigDecimal.ZERO)));
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.withdraw(new WithdrawalRequest("1001", new BigDecimal("-50"))));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  4. Insufficient Balance Rejection
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Insufficient Balance Rejection")
    class InsufficientBalanceTests {

        @Test
        @DisplayName("Withdrawal exceeding balance throws InsufficientBalanceException")
        void withdrawalExceedingBalance() {
            BigDecimal currentBalance = accountService.getBalance("1001");
            BigDecimal excessiveAmount = currentBalance.add(new BigDecimal("1.00"));

            InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class, () ->
                    bankingService.withdraw(new WithdrawalRequest("1001", excessiveAmount)));

            assertTrue(ex.getMessage().contains("1001"));
            assertTrue(ex.getMessage().contains("Insufficient balance"));
        }

        @Test
        @DisplayName("Transfer exceeding source balance throws InsufficientBalanceException")
        void transferExceedingSourceBalance() {
            BigDecimal currentBalance = accountService.getBalance("1001");
            BigDecimal excessiveAmount = currentBalance.add(new BigDecimal("5000"));

            InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class, () ->
                    bankingService.transfer(
                            new TransferRequest("1001", "1002", excessiveAmount)));

            assertTrue(ex.getMessage().contains("1001"));
        }

        @Test
        @DisplayName("Balance is unchanged after failed withdrawal")
        @Transactional
        void balanceUnchangedAfterFailedWithdrawal() {
            BigDecimal balanceBefore = accountService.getBalance("1001");

            assertThrows(InsufficientBalanceException.class, () ->
                    bankingService.withdraw(new WithdrawalRequest("1001",
                            balanceBefore.add(new BigDecimal("1")))));

            BigDecimal balanceAfter = accountService.getBalance("1001");
            assertEquals(0, balanceBefore.compareTo(balanceAfter),
                    "Balance should remain unchanged after a rejected withdrawal");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  5. Fund Transfer in SAFE Mode
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Fund Transfer (SAFE Mode)")
    class TransferSafeModeTests {

        @Test
        @DisplayName("Transfer moves funds from source to destination")
        @Transactional
        void transferMovesFunds() {
            BigDecimal sourceInitial = accountService.getBalance("1001");
            BigDecimal destInitial = accountService.getBalance("1002");
            BigDecimal transferAmount = new BigDecimal("3000.00");

            TransactionResponse response = bankingService.transfer(
                    new TransferRequest("1001", "1002", transferAmount));

            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals(TransactionType.TRANSFER, response.getTransactionType());
            assertEquals("1001", response.getSourceAccountNumber());
            assertEquals("1002", response.getDestinationAccountNumber());
            assertEquals(ExecutionMode.SAFE, response.getExecutionMode());

            BigDecimal sourceAfter = accountService.getBalance("1001");
            BigDecimal destAfter = accountService.getBalance("1002");

            assertEquals(0, sourceInitial.subtract(transferAmount).compareTo(sourceAfter),
                    "Source balance should decrease by transfer amount");
            assertEquals(0, destInitial.add(transferAmount).compareTo(destAfter),
                    "Destination balance should increase by transfer amount");
        }

        @Test
        @DisplayName("Transfer preserves total balance across both accounts")
        @Transactional
        void transferPreservesTotalBalance() {
            BigDecimal totalBefore = accountService.getBalance("1003")
                    .add(accountService.getBalance("1004"));

            bankingService.transfer(new TransferRequest("1003", "1004", new BigDecimal("7500")));

            BigDecimal totalAfter = accountService.getBalance("1003")
                    .add(accountService.getBalance("1004"));

            assertEquals(0, totalBefore.compareTo(totalAfter),
                    "Total balance across source and destination must remain constant");
        }

        @Test
        @DisplayName("Self-transfer is rejected")
        void selfTransferRejected() {
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.transfer(
                            new TransferRequest("1001", "1001", new BigDecimal("100"))));
        }

        @Test
        @DisplayName("Transfer with invalid amount is rejected")
        void transferInvalidAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.transfer(
                            new TransferRequest("1001", "1002", BigDecimal.ZERO)));
            assertThrows(IllegalArgumentException.class, () ->
                    bankingService.transfer(
                            new TransferRequest("1001", "1002", new BigDecimal("-100"))));
        }

        @Test
        @DisplayName("Transfer with non-existent source account fails")
        void transferNonExistentSource() {
            assertThrows(AccountNotFoundException.class, () ->
                    bankingService.transfer(
                            new TransferRequest("0000", "1002", new BigDecimal("100"))));
        }

        @Test
        @DisplayName("Transfer with non-existent destination account fails")
        void transferNonExistentDest() {
            assertThrows(AccountNotFoundException.class, () ->
                    bankingService.transfer(
                            new TransferRequest("1001", "0000", new BigDecimal("100"))));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  6. Concurrent Deposits in SAFE Mode
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Concurrent Deposits (SAFE Mode)")
    class ConcurrentDepositTests {

        @Test
        @DisplayName("10 threads each depositing ₹100 to same account yields correct total")
        void concurrentDepositsAreSerializedBySafeLock() throws InterruptedException {
            // Use an in-memory BankAccount to test SafeBankingStrategy's ReentrantLock
            BigDecimal initialBalance = new BigDecimal("10000.0000");
            BankAccount sharedAccount = new BankAccount("CONC-DEP", "ConcurrentUser", initialBalance);

            int threadCount = 10;
            BigDecimal depositAmount = new BigDecimal("100.00");
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await(); // All threads start simultaneously
                        safeStrategy.deposit(sharedAccount, depositAmount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown(); // Release all threads
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "All deposit threads should finish within 10 seconds");

            BigDecimal expectedBalance = initialBalance.add(
                    depositAmount.multiply(new BigDecimal(threadCount)));
            assertEquals(0, expectedBalance.compareTo(sharedAccount.getBalance()),
                    "After " + threadCount + " concurrent deposits of ₹" + depositAmount +
                            ", balance should be " + expectedBalance +
                            " but was " + sharedAccount.getBalance());

            // Verify persistence: save and reload
            accountService.createAccount("CONC-DEP", "ConcurrentUser", sharedAccount.getBalance());
            BigDecimal persistedBalance = accountService.getBalance("CONC-DEP");
            assertEquals(0, expectedBalance.compareTo(persistedBalance),
                    "Persisted balance should match in-memory balance");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  7. Concurrent Withdrawals in SAFE Mode
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. Concurrent Withdrawals (SAFE Mode)")
    class ConcurrentWithdrawalTests {

        @Test
        @DisplayName("10 threads each withdrawing ₹500 from ₹5000 — all 10 succeed, balance = 0")
        void concurrentWithdrawalsExactlyDrainAccount() throws InterruptedException {
            BigDecimal initialBalance = new BigDecimal("5000.0000");
            BankAccount sharedAccount = new BankAccount("CONC-WD", "WithdrawUser", initialBalance);

            int threadCount = 10;
            BigDecimal withdrawAmount = new BigDecimal("500.00");
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        safeStrategy.withdraw(sharedAccount, withdrawAmount);
                        successCount.incrementAndGet();
                    } catch (InsufficientBalanceException e) {
                        failCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "All withdrawal threads should finish within 10 seconds");

            // ₹5000 / ₹500 = exactly 10 successful withdrawals
            assertEquals(10, successCount.get(),
                    "Exactly 10 withdrawals of ₹500 should succeed from ₹5000 balance");
            assertEquals(0, failCount.get(),
                    "No withdrawals should fail when there's exactly enough balance");
            assertEquals(0, BigDecimal.ZERO.compareTo(sharedAccount.getBalance()),
                    "Balance should be ₹0 after 10 × ₹500 withdrawals from ₹5000");
        }

        @Test
        @DisplayName("Concurrent withdrawals with insufficient total — some fail, balance stays non-negative")
        void concurrentWithdrawalsPartialSuccess() throws InterruptedException {
            // ₹3000 balance, 10 threads × ₹500 = only 6 can succeed
            BigDecimal initialBalance = new BigDecimal("3000.0000");
            BankAccount sharedAccount = new BankAccount("CONC-WD2", "PartialUser", initialBalance);

            int threadCount = 10;
            BigDecimal withdrawAmount = new BigDecimal("500.00");
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        safeStrategy.withdraw(sharedAccount, withdrawAmount);
                        successCount.incrementAndGet();
                    } catch (InsufficientBalanceException e) {
                        failCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "All threads should finish within 10 seconds");

            // ₹3000 / ₹500 = exactly 6 successful, 4 failed
            assertEquals(6, successCount.get(),
                    "Exactly 6 withdrawals should succeed (₹3000 / ₹500)");
            assertEquals(4, failCount.get(),
                    "Exactly 4 withdrawals should fail due to insufficient balance");
            assertEquals(0, BigDecimal.ZERO.compareTo(sharedAccount.getBalance()),
                    "Balance should be ₹0 after all possible withdrawals");

            // Verify total consistency
            int totalOps = successCount.get() + failCount.get();
            assertEquals(threadCount, totalOps,
                    "Total operations should equal thread count");
        }

        @Test
        @DisplayName("SAFE Mode: Two concurrent ₹6,000 withdrawals on ₹10,000 account — exactly one succeeds, second rejected with InsufficientBalanceException")
        void twoWithdrawalsOf6000On10000Account() throws InterruptedException {
            // Setup fresh account in DB with ₹10,000
            String accNum = "CONC-6K-10K";
            accountService.createAccount(accNum, "TwoWithdrawalsUser", new BigDecimal("10000.0000"));
            bankingService.setCurrentMode(ExecutionMode.SAFE);

            int threadCount = 2;
            BigDecimal withdrawAmount = new BigDecimal("6000.0000");
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        bankingService.withdraw(new WithdrawalRequest(accNum, withdrawAmount));
                        successCount.incrementAndGet();
                    } catch (InsufficientBalanceException e) {
                        failCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(completed, "Both threads should complete within 10 seconds");
            assertEquals(1, successCount.get(), "Exactly one withdrawal of ₹6,000 must succeed");
            assertEquals(1, failCount.get(), "Second withdrawal of ₹6,000 must be rejected");

            BankAccount updated = accountService.getAccount(accNum);
            assertEquals(0, new BigDecimal("4000.0000").compareTo(updated.getBalance()),
                    "Final account balance must be exactly ₹4,000");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  8. Execution Mode Switching
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Execution Mode Switching")
    class ExecutionModeSwitchingTests {

        @Test
        @DisplayName("Default mode is SAFE")
        void defaultModeIsSafe() {
            bankingService.setCurrentMode(ExecutionMode.SAFE);
            assertEquals(ExecutionMode.SAFE, bankingService.getCurrentMode());
        }

        @Test
        @DisplayName("Switching to UNSAFE mode and back")
        void switchToUnsafeAndBack() {
            bankingService.setCurrentMode(ExecutionMode.UNSAFE);
            assertEquals(ExecutionMode.UNSAFE, bankingService.getCurrentMode());

            bankingService.setCurrentMode(ExecutionMode.SAFE);
            assertEquals(ExecutionMode.SAFE, bankingService.getCurrentMode());
        }

        @Test
        @DisplayName("getStrategy returns correct strategy for each mode")
        void strategyMatchesMode() {
            bankingService.setCurrentMode(ExecutionMode.SAFE);
            assertEquals("SAFE", bankingService.getStrategy().getModeName());

            bankingService.setCurrentMode(ExecutionMode.UNSAFE);
            assertEquals("UNSAFE", bankingService.getStrategy().getModeName());

            // Reset to safe for other tests
            bankingService.setCurrentMode(ExecutionMode.SAFE);
        }

        @Test
        @DisplayName("getStrategy(mode) returns strategy for specified mode regardless of current")
        void getStrategyByModeParameter() {
            bankingService.setCurrentMode(ExecutionMode.SAFE);

            assertEquals("UNSAFE", bankingService.getStrategy(ExecutionMode.UNSAFE).getModeName());
            assertEquals("SAFE", bankingService.getStrategy(ExecutionMode.SAFE).getModeName());
        }

        @Test
        @DisplayName("Deposit records the execution mode in the transaction response")
        @Transactional
        void depositRecordsMode() {
            bankingService.setCurrentMode(ExecutionMode.SAFE);
            TransactionResponse safeResponse = bankingService.deposit(
                    new DepositRequest("1001", new BigDecimal("100")));
            assertEquals(ExecutionMode.SAFE, safeResponse.getExecutionMode());
        }

        @Test
        @DisplayName("Rapid mode toggling remains consistent")
        void rapidToggling() {
            for (int i = 0; i < 50; i++) {
                ExecutionMode expected = (i % 2 == 0) ? ExecutionMode.SAFE : ExecutionMode.UNSAFE;
                bankingService.setCurrentMode(expected);
                assertEquals(expected, bankingService.getCurrentMode(),
                        "Mode mismatch on iteration " + i);
            }
            bankingService.setCurrentMode(ExecutionMode.SAFE);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  9. Transaction Logging Verification
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("9. Transaction Logging")
    class TransactionLoggingTests {

        @Test
        @DisplayName("Successful deposit creates a transaction record with correct fields")
        @Transactional
        void depositCreatesTransactionRecord() {
            TransactionResponse response = bankingService.deposit(
                    new DepositRequest("1003", new BigDecimal("777.50")));

            assertNotNull(response.getTransactionId(), "Transaction ID should be generated");
            assertFalse(response.getTransactionId().isBlank(), "Transaction ID should not be blank");

            assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals("1003", response.getSourceAccountNumber());
            assertNull(response.getDestinationAccountNumber(),
                    "Deposit should have no destination account");
            assertEquals(0, new BigDecimal("777.50").compareTo(response.getAmount()));
            assertEquals(ExecutionMode.SAFE, response.getExecutionMode());
            assertNotNull(response.getThreadName(), "Thread name should be recorded");
            assertNotNull(response.getTimestamp(), "Timestamp should be recorded");
            assertNotNull(response.getDurationMs(), "Duration should be recorded");
            assertTrue(response.getDurationMs() >= 0, "Duration should be non-negative");
            assertTrue(response.getDescription().contains("777.5"),
                    "Description should contain the deposit amount");
            assertNull(response.getErrorMessage(),
                    "Successful transaction should have no error message");
        }

        @Test
        @DisplayName("Successful withdrawal creates a transaction record")
        @Transactional
        void withdrawalCreatesTransactionRecord() {
            TransactionResponse response = bankingService.withdraw(
                    new WithdrawalRequest("1003", new BigDecimal("1000")));

            assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals("1003", response.getSourceAccountNumber());
            assertTrue(response.getDescription().contains("1000"));
        }

        @Test
        @DisplayName("Successful transfer creates a transaction record with both accounts")
        @Transactional
        void transferCreatesTransactionRecord() {
            TransactionResponse response = bankingService.transfer(
                    new TransferRequest("1001", "1002", new BigDecimal("500")));

            assertEquals(TransactionType.TRANSFER, response.getTransactionType());
            assertEquals(TransactionStatus.SUCCESS, response.getStatus());
            assertEquals("1001", response.getSourceAccountNumber());
            assertEquals("1002", response.getDestinationAccountNumber());
            assertTrue(response.getDescription().contains("1001"));
            assertTrue(response.getDescription().contains("1002"));
        }

        @Test
        @DisplayName("Failed transaction still records a FAILED transaction with error message")
        void failedTransactionIsLogged() {
            BigDecimal currentBalance = accountService.getBalance("1004");
            BigDecimal overAmount = currentBalance.add(new BigDecimal("9999"));

            try {
                bankingService.withdraw(new WithdrawalRequest("1004", overAmount));
                fail("Should have thrown InsufficientBalanceException");
            } catch (InsufficientBalanceException ignored) {
                // expected
            }

            // Check that a FAILED transaction was persisted
            List<TransactionResponse> allTxns = transactionService.getAllTransactions();
            boolean foundFailed = allTxns.stream()
                    .anyMatch(t -> t.getStatus() == TransactionStatus.FAILED
                            && "1004".equals(t.getSourceAccountNumber())
                            && t.getErrorMessage() != null
                            && t.getErrorMessage().contains("Insufficient balance"));

            assertTrue(foundFailed,
                    "A FAILED transaction with error message should have been persisted for account 1004");
        }

        @Test
        @DisplayName("Transaction channel defaults to ONLINE")
        @Transactional
        void defaultChannelIsOnline() {
            TransactionResponse response = bankingService.deposit(
                    new DepositRequest("1001", new BigDecimal("100")));
            assertEquals("ONLINE", response.getChannel());
        }

        @Test
        @DisplayName("Custom channel is recorded in the transaction")
        @Transactional
        void customChannelIsRecorded() {
            DepositRequest request = new DepositRequest("1001", new BigDecimal("100"));
            request.setChannel("ATM");
            TransactionResponse response = bankingService.deposit(request);
            assertEquals("ATM", response.getChannel());
        }

        @Test
        @DisplayName("TransactionResponse.fromEntity maps all fields correctly")
        void fromEntityMapsAllFields() {
            Transaction entity = new Transaction();
            entity.setTransactionId("TXN-TEST-001");
            entity.setTransactionType(TransactionType.DEPOSIT);
            entity.setSourceAccountNumber("1001");
            entity.setDestinationAccountNumber(null);
            entity.setAmount(new BigDecimal("250.00"));
            entity.setExecutionMode(ExecutionMode.SAFE);
            entity.setThreadName("test-thread-1");
            entity.setChannel("MOBILE");
            entity.setStatus(TransactionStatus.SUCCESS);
            entity.setTimestamp(java.time.LocalDateTime.of(2026, 11, 20, 12, 0, 0));
            entity.setDurationMs(42L);
            entity.setDescription("Test deposit");
            entity.setErrorMessage(null);
            entity.setSimulationId("SIM-001");

            TransactionResponse dto = TransactionResponse.fromEntity(entity);

            assertEquals("TXN-TEST-001", dto.getTransactionId());
            assertEquals(TransactionType.DEPOSIT, dto.getTransactionType());
            assertEquals("1001", dto.getSourceAccountNumber());
            assertNull(dto.getDestinationAccountNumber());
            assertEquals(0, new BigDecimal("250.00").compareTo(dto.getAmount()));
            assertEquals(ExecutionMode.SAFE, dto.getExecutionMode());
            assertEquals("test-thread-1", dto.getThreadName());
            assertEquals("MOBILE", dto.getChannel());
            assertEquals(TransactionStatus.SUCCESS, dto.getStatus());
            assertEquals(java.time.LocalDateTime.of(2026, 11, 20, 12, 0, 0), dto.getTimestamp());
            assertEquals(42L, dto.getDurationMs());
            assertEquals("Test deposit", dto.getDescription());
            assertNull(dto.getErrorMessage());
            assertEquals("SIM-001", dto.getSimulationId());
        }
    }
}
