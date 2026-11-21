package com.bolt;

import com.bolt.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * BOLT — Banking Operations & Locking Technology
 *
 * A multithreaded banking and ATM simulation system demonstrating
 * OS concurrency concepts: race conditions, synchronization,
 * mutual exclusion, and deadlocks.
 *
 * Course: Operating Systems (CSC304A)
 */
@SpringBootApplication
public class BoltApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoltApplication.class, args);
    }

    /**
     * Initialize sample accounts on startup.
     * Uses AccountService.initializeSampleAccounts() which checks for
     * existing accounts to prevent duplicates after restarts.
     */
    @Bean
    CommandLineRunner initData(AccountService accountService) {
        return args -> {
            accountService.initializeSampleAccounts();
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║      BOLT — Banking Operations & Locking        ║");
            System.out.println("║               Technology v1.0                   ║");
            System.out.println("║      OS Concurrency Simulation System           ║");
            System.out.println("╠══════════════════════════════════════════════════╣");
            System.out.println("║  Sample Accounts Initialized:                   ║");
            System.out.println("║    1001 – Alice   – ₹10,000                     ║");
            System.out.println("║    1002 – Bob     – ₹15,000                     ║");
            System.out.println("║    1003 – Charlie – ₹20,000                     ║");
            System.out.println("║    1004 – David   – ₹12,000                     ║");
            System.out.println("║                                                 ║");
            System.out.println("║  API: http://localhost:8080/api                  ║");
            System.out.println("║  H2 Console: http://localhost:8080/h2-console    ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
        };
    }
}
