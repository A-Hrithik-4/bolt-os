package com.bolt.dto;

import com.bolt.enums.ExecutionMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for the dashboard summary, supplying all statistics the frontend needs.
 */
public class DashboardResponse {

    private long totalAccounts;
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long totalDeposits;
    private long totalWithdrawals;
    private long totalTransfers;
    private BigDecimal totalDepositAmount;
    private BigDecimal totalWithdrawalAmount;
    private BigDecimal totalTransferAmount;
    private ExecutionMode currentMode;
    private int activeThreadCount;
    private int completedThreadCount;
    private List<Map<String, Object>> accountBalances;
    private List<Map<String, Object>> recentTransactions;
    private List<Map<String, Object>> threadHistory;
    private Map<String, Object> raceConditionResults;
    private Map<String, Object> deadlockResults;
    private Map<String, Object> safeVsUnsafeStats;
    private double averageTransactionDurationMs;

    public DashboardResponse() {}

    // Getters and setters
    public long getTotalAccounts() { return totalAccounts; }
    public void setTotalAccounts(long totalAccounts) { this.totalAccounts = totalAccounts; }

    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

    public long getSuccessfulTransactions() { return successfulTransactions; }
    public void setSuccessfulTransactions(long successfulTransactions) { this.successfulTransactions = successfulTransactions; }

    public long getFailedTransactions() { return failedTransactions; }
    public void setFailedTransactions(long failedTransactions) { this.failedTransactions = failedTransactions; }

    public long getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(long totalDeposits) { this.totalDeposits = totalDeposits; }

    public long getTotalWithdrawals() { return totalWithdrawals; }
    public void setTotalWithdrawals(long totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }

    public long getTotalTransfers() { return totalTransfers; }
    public void setTotalTransfers(long totalTransfers) { this.totalTransfers = totalTransfers; }

    public BigDecimal getTotalDepositAmount() { return totalDepositAmount; }
    public void setTotalDepositAmount(BigDecimal totalDepositAmount) { this.totalDepositAmount = totalDepositAmount; }

    public BigDecimal getTotalWithdrawalAmount() { return totalWithdrawalAmount; }
    public void setTotalWithdrawalAmount(BigDecimal totalWithdrawalAmount) { this.totalWithdrawalAmount = totalWithdrawalAmount; }

    public BigDecimal getTotalTransferAmount() { return totalTransferAmount; }
    public void setTotalTransferAmount(BigDecimal totalTransferAmount) { this.totalTransferAmount = totalTransferAmount; }

    public ExecutionMode getCurrentMode() { return currentMode; }
    public void setCurrentMode(ExecutionMode currentMode) { this.currentMode = currentMode; }

    public int getActiveThreadCount() { return activeThreadCount; }
    public void setActiveThreadCount(int activeThreadCount) { this.activeThreadCount = activeThreadCount; }

    public int getCompletedThreadCount() { return completedThreadCount; }
    public void setCompletedThreadCount(int completedThreadCount) { this.completedThreadCount = completedThreadCount; }

    public List<Map<String, Object>> getAccountBalances() { return accountBalances; }
    public void setAccountBalances(List<Map<String, Object>> accountBalances) { this.accountBalances = accountBalances; }

    public List<Map<String, Object>> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<Map<String, Object>> recentTransactions) { this.recentTransactions = recentTransactions; }

    public List<Map<String, Object>> getThreadHistory() { return threadHistory; }
    public void setThreadHistory(List<Map<String, Object>> threadHistory) { this.threadHistory = threadHistory; }

    public Map<String, Object> getRaceConditionResults() { return raceConditionResults; }
    public void setRaceConditionResults(Map<String, Object> raceConditionResults) { this.raceConditionResults = raceConditionResults; }

    public Map<String, Object> getDeadlockResults() { return deadlockResults; }
    public void setDeadlockResults(Map<String, Object> deadlockResults) { this.deadlockResults = deadlockResults; }

    public Map<String, Object> getSafeVsUnsafeStats() { return safeVsUnsafeStats; }
    public void setSafeVsUnsafeStats(Map<String, Object> safeVsUnsafeStats) { this.safeVsUnsafeStats = safeVsUnsafeStats; }

    public double getAverageTransactionDurationMs() { return averageTransactionDurationMs; }
    public void setAverageTransactionDurationMs(double averageTransactionDurationMs) { this.averageTransactionDurationMs = averageTransactionDurationMs; }
}
