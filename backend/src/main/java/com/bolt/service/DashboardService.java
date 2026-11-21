package com.bolt.service;

import com.bolt.concurrency.TransactionExecutor;
import com.bolt.dto.DashboardResponse;
import com.bolt.dto.TransactionResponse;
import com.bolt.enums.ExecutionMode;
import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import com.bolt.model.BankAccount;
import com.bolt.model.Transaction;
import com.bolt.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates statistics for the dashboard.
 * All values are computed from actual stored data — nothing is fabricated.
 */
@Service
public class DashboardService {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final BankingService bankingService;
    private final TransactionExecutor transactionExecutor;
    private final DeadlockService deadlockService;

    public DashboardService(AccountService accountService,
                            TransactionService transactionService,
                            TransactionRepository transactionRepository,
                            BankingService bankingService,
                            TransactionExecutor transactionExecutor,
                            DeadlockService deadlockService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.bankingService = bankingService;
        this.transactionExecutor = transactionExecutor;
        this.deadlockService = deadlockService;
    }

    public DashboardResponse getSummary() {
        DashboardResponse resp = new DashboardResponse();

        resp.setTotalAccounts(accountService.getAccountCount());
        resp.setTotalTransactions(transactionRepository.count());
        resp.setSuccessfulTransactions(transactionRepository.countByStatus(TransactionStatus.SUCCESS));
        resp.setFailedTransactions(transactionRepository.countByStatus(TransactionStatus.FAILED));
        resp.setTotalDeposits(transactionRepository.countByTransactionType(TransactionType.DEPOSIT));
        resp.setTotalWithdrawals(transactionRepository.countByTransactionType(TransactionType.WITHDRAWAL));
        resp.setTotalTransfers(transactionRepository.countByTransactionType(TransactionType.TRANSFER));

        List<Transaction> allTxns = transactionService.getAllTransactionEntities();

        resp.setTotalDepositAmount(sumByType(allTxns, TransactionType.DEPOSIT));
        resp.setTotalWithdrawalAmount(sumByType(allTxns, TransactionType.WITHDRAWAL));
        resp.setTotalTransferAmount(sumByType(allTxns, TransactionType.TRANSFER));

        resp.setCurrentMode(bankingService.getCurrentMode());
        resp.setActiveThreadCount(transactionExecutor.getActiveThreadCount());
        resp.setCompletedThreadCount(transactionExecutor.getCompletedThreadCount());

        Double avgDuration = transactionRepository.findAverageDuration();
        resp.setAverageTransactionDurationMs(avgDuration != null ? avgDuration : 0.0);

        // Account balances
        resp.setAccountBalances(accountService.getAllAccounts().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accountNumber", a.getAccountNumber());
            m.put("customerName", a.getCustomerName());
            m.put("balance", a.getBalance());
            m.put("status", a.getAccountStatus());
            return m;
        }).collect(Collectors.toList()));

        // Recent transactions (last 20)
        resp.setRecentTransactions(allTxns.stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .limit(20)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("transactionId", t.getTransactionId());
                    m.put("type", t.getTransactionType());
                    m.put("source", t.getSourceAccountNumber());
                    m.put("destination", t.getDestinationAccountNumber());
                    m.put("amount", t.getAmount());
                    m.put("status", t.getStatus());
                    m.put("thread", t.getThreadName());
                    m.put("mode", t.getExecutionMode());
                    m.put("durationMs", t.getDurationMs());
                    m.put("timestamp", t.getTimestamp());
                    return m;
                }).collect(Collectors.toList()));

        // SAFE vs UNSAFE comparison
        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("safeTransactions", transactionRepository.countByExecutionMode(ExecutionMode.SAFE));
        comparison.put("unsafeTransactions", transactionRepository.countByExecutionMode(ExecutionMode.UNSAFE));
        comparison.put("safeSuccessful", transactionRepository.countByExecutionModeAndStatus(ExecutionMode.SAFE, TransactionStatus.SUCCESS));
        comparison.put("unsafeSuccessful", transactionRepository.countByExecutionModeAndStatus(ExecutionMode.UNSAFE, TransactionStatus.SUCCESS));
        comparison.put("safeFailed", transactionRepository.countByExecutionModeAndStatus(ExecutionMode.SAFE, TransactionStatus.FAILED));
        comparison.put("unsafeFailed", transactionRepository.countByExecutionModeAndStatus(ExecutionMode.UNSAFE, TransactionStatus.FAILED));
        Double safeAvg = transactionRepository.findAverageDurationByMode(ExecutionMode.SAFE);
        Double unsafeAvg = transactionRepository.findAverageDurationByMode(ExecutionMode.UNSAFE);
        comparison.put("safeAvgDurationMs", safeAvg != null ? safeAvg : 0.0);
        comparison.put("unsafeAvgDurationMs", unsafeAvg != null ? unsafeAvg : 0.0);
        resp.setSafeVsUnsafeStats(comparison);

        // Deadlock results
        Map<String, Object> deadlock = deadlockService.getLastDeadlockResult();
        Map<String, Object> prevention = deadlockService.getLastPreventionResult();
        if (deadlock != null || prevention != null) {
            Map<String, Object> dl = new LinkedHashMap<>();
            if (deadlock != null) dl.put("deadlockDemo", deadlock);
            if (prevention != null) dl.put("preventionDemo", prevention);
            resp.setDeadlockResults(dl);
        }

        return resp;
    }

    public List<TransactionResponse> getTransactions() {
        return transactionService.getAllTransactions();
    }

    public Map<String, Object> getThreadStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("activeThreads", transactionExecutor.getActiveThreadCount());
        status.put("completedThreads", transactionExecutor.getCompletedThreadCount());
        status.put("currentMode", bankingService.getCurrentMode());

        // Thread history from transactions
        List<Transaction> allTxns = transactionService.getAllTransactionEntities();
        Map<String, List<Map<String, Object>>> threadHistory = new LinkedHashMap<>();
        for (Transaction txn : allTxns) {
            threadHistory.computeIfAbsent(txn.getThreadName(), k -> new ArrayList<>()).add(Map.of(
                    "transactionId", txn.getTransactionId(),
                    "type", txn.getTransactionType().name(),
                    "status", txn.getStatus().name(),
                    "durationMs", txn.getDurationMs() != null ? txn.getDurationMs() : 0
            ));
        }
        status.put("threadHistory", threadHistory);
        return status;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAccounts", accountService.getAccountCount());
        stats.put("totalTransactions", transactionRepository.count());
        stats.put("successRate", calculateSuccessRate());
        stats.put("failureRate", calculateFailureRate());
        stats.put("avgDurationMs", transactionRepository.findAverageDuration());

        // Per-type breakdown
        for (TransactionType type : TransactionType.values()) {
            stats.put(type.name().toLowerCase() + "Count", transactionRepository.countByTransactionType(type));
        }

        // Per-mode breakdown
        for (ExecutionMode mode : ExecutionMode.values()) {
            stats.put(mode.name().toLowerCase() + "Count", transactionRepository.countByExecutionMode(mode));
        }

        return stats;
    }

    private BigDecimal sumByType(List<Transaction> txns, TransactionType type) {
        return txns.stream()
                .filter(t -> t.getTransactionType() == type && t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double calculateSuccessRate() {
        long total = transactionRepository.count();
        if (total == 0) return 0.0;
        long success = transactionRepository.countByStatus(TransactionStatus.SUCCESS);
        return (double) success / total * 100.0;
    }

    private double calculateFailureRate() {
        long total = transactionRepository.count();
        if (total == 0) return 0.0;
        long failed = transactionRepository.countByStatus(TransactionStatus.FAILED);
        return (double) failed / total * 100.0;
    }
}
