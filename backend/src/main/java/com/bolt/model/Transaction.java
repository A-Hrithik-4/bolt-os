package com.bolt.model;

import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import com.bolt.enums.ExecutionMode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a banking transaction in the BOLT system.
 * Records all details including thread info, timing, and execution mode.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_source", columnList = "sourceAccountNumber"),
    @Index(name = "idx_transaction_dest", columnList = "destinationAccountNumber"),
    @Index(name = "idx_transaction_type", columnList = "transactionType"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_mode", columnList = "executionMode"),
    @Index(name = "idx_transaction_timestamp", columnList = "timestamp")
})
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(name = "sourceAccountNumber")
    private String sourceAccountNumber;

    @Column(name = "destinationAccountNumber")
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "executionMode", nullable = false)
    private ExecutionMode executionMode;

    @Column(nullable = false)
    private String threadName;

    @Column(nullable = false)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column
    private Long durationMs;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String errorMessage;

    @Column
    private String simulationId;

    public Transaction() {}

    // Getters and setters

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public ExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(ExecutionMode executionMode) { this.executionMode = executionMode; }

    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getSimulationId() { return simulationId; }
    public void setSimulationId(String simulationId) { this.simulationId = simulationId; }
}
