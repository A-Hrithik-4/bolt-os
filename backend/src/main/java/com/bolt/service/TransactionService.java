package com.bolt.service;

import com.bolt.dto.TransactionResponse;
import com.bolt.model.Transaction;
import com.bolt.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for persisting and retrieving transaction records.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Transactional
    public List<Transaction> saveAll(List<Transaction> transactions) {
        return transactionRepository.saveAll(transactions);
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllOrderByTimestampDesc().stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransaction(String transactionId) {
        Transaction txn = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        return TransactionResponse.fromEntity(txn);
    }

    public List<TransactionResponse> getTransactionsBySimulation(String simulationId) {
        return transactionRepository.findBySimulationId(simulationId).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<Transaction> getAllTransactionEntities() {
        return transactionRepository.findAll();
    }

    public TransactionRepository getRepository() {
        return transactionRepository;
    }
}
