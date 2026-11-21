package com.bolt.repository;

import com.bolt.enums.ExecutionMode;
import com.bolt.enums.TransactionStatus;
import com.bolt.enums.TransactionType;
import com.bolt.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findBySimulationId(String simulationId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByExecutionMode(ExecutionMode mode);

    long countByTransactionType(TransactionType type);

    long countByStatus(TransactionStatus status);

    long countByExecutionMode(ExecutionMode mode);

    long countByExecutionModeAndStatus(ExecutionMode mode, TransactionStatus status);

    @Query("SELECT t FROM Transaction t ORDER BY t.timestamp DESC")
    List<Transaction> findAllOrderByTimestampDesc();

    @Query("SELECT AVG(t.durationMs) FROM Transaction t WHERE t.durationMs IS NOT NULL")
    Double findAverageDuration();

    @Query("SELECT AVG(t.durationMs) FROM Transaction t WHERE t.executionMode = :mode AND t.durationMs IS NOT NULL")
    Double findAverageDurationByMode(ExecutionMode mode);

    List<Transaction> findBySourceAccountNumberOrDestinationAccountNumber(String source, String dest);
}
