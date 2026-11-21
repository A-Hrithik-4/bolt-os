package com.bolt.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String accountNumber, String requested, String available) {
        super("Insufficient balance in account " + accountNumber +
              ": requested=" + requested + ", available=" + available);
    }
}
