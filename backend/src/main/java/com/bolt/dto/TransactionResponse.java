package com.bolt.dto;

import com.bolt.enums.ExecutionMode;
import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for returning transaction details to API clients.
 */
public class TransactionResponse {

    private String transactionId;
    private TransactionType transactionType;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private ExecutionMode executionMode;
    private String threadName;
    private String channel;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private Long durationMs;
    private String description;
    private String errorMessage;
    private String simulationId;

    public TransactionResponse() {}

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

    /**
     * Factory method to convert a Transaction entity to a response DTO.
     */
    public static TransactionResponse fromEntity(com.bolt.model.Transaction tx) {
        TransactionResponse resp = new TransactionResponse();
        resp.setTransactionId(tx.getTransactionId());
        resp.setTransactionType(tx.getTransactionType());
        resp.setSourceAccountNumber(tx.getSourceAccountNumber());
        resp.setDestinationAccountNumber(tx.getDestinationAccountNumber());
        resp.setAmount(tx.getAmount());
        resp.setExecutionMode(tx.getExecutionMode());
        resp.setThreadName(tx.getThreadName());
        resp.setChannel(tx.getChannel());
        resp.setStatus(tx.getStatus());
        resp.setTimestamp(tx.getTimestamp());
        resp.setDurationMs(tx.getDurationMs());
        resp.setDescription(tx.getDescription());
        resp.setErrorMessage(tx.getErrorMessage());
        resp.setSimulationId(tx.getSimulationId());
        return resp;
    }
}
