# ⚡ BOLT — Banking Operations & Locking Technology

> **A Multithreaded Banking and ATM Simulation System**
> Academic Project for Operating Systems (CSC304A)

---

## 📋 Overview

BOLT is a professional Java-based simulation system that demonstrates fundamental operating systems concepts through a banking application:

- **Race Conditions** — What happens when threads share data without synchronization
- **Mutual Exclusion** — How locks prevent concurrent data corruption
- **Deadlocks** — How circular waiting freezes threads, and how to prevent it
- **Thread Synchronization** — ReentrantLock, CountDownLatch, CyclicBarrier

## 🏗️ Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.2.5 | Application Framework |
| Spring Data JPA | 3.2.x | Database Access |
| H2 Database | Runtime | In-Memory Database |
| Maven | 3.x | Build System |
| JUnit 5 | 5.x | Testing Framework |

## 🚀 Quick Start

### Prerequisites

- Java 21+ (JDK)
- Maven 3.6+

### Build & Run

```bash
cd backend

# Build the project
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Start the application
./mvnw spring-boot:run
```

The server starts at **http://localhost:8080**

### H2 Database Console

Access the H2 console at **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:boltdb`
- Username: `sa`
- Password: *(empty)*

## 🏦 Sample Accounts

| Account | Customer | Initial Balance |
|---------|----------|----------------|
| 1001 | Alice | ₹10,000 |
| 1002 | Bob | ₹15,000 |
| 1003 | Charlie | ₹20,000 |
| 1004 | David | ₹12,000 |

## 🔄 Execution Modes

### SAFE Mode (Default)
- Full synchronization with ReentrantLock
- Mutual exclusion on all account operations
- Consistent lock ordering for transfers (deadlock prevention)
- Atomicity guaranteed

### UNSAFE Mode
- No synchronization — threads access shared data freely
- Demonstrates race conditions and lost updates
- Intentionally flawed for educational purposes

## 📡 REST API Reference

### Account Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/accounts` | List all accounts |
| GET | `/api/accounts/{id}` | Get account by number |
| GET | `/api/accounts/{id}/balance` | Get account balance |
| POST | `/api/accounts` | Create new account |

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions/deposit` | Deposit money |
| POST | `/api/transactions/withdraw` | Withdraw money |
| POST | `/api/transactions/transfer` | Transfer between accounts |
| GET | `/api/transactions` | List all transactions |
| GET | `/api/transactions/{id}` | Get transaction by ID |

### Execution Mode
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/mode` | Get current mode |
| PUT | `/api/mode` | Switch mode (SAFE/UNSAFE) |

### Simulations
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/simulation/run` | Run concurrent banking simulation |
| POST | `/api/simulation/race-condition` | Race condition demonstration |
| POST | `/api/simulation/deadlock` | Deadlock demonstration |
| POST | `/api/simulation/deadlock-prevention` | Deadlock prevention demo |
| GET | `/api/simulation/{id}` | Get simulation results |

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard/summary` | Full dashboard data |
| GET | `/api/dashboard/transactions` | Transaction history |
| GET | `/api/dashboard/thread-status` | Thread execution status |
| GET | `/api/dashboard/statistics` | Statistical data |

## 🧪 API Usage Examples

### Deposit
```bash
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"1001","amount":5000,"channel":"ATM"}'
```

### Withdraw
```bash
curl -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"1002","amount":3000,"channel":"ONLINE"}'
```

### Transfer
```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber":"1001","destinationAccountNumber":"1002","amount":2000}'
```

### Switch to UNSAFE Mode
```bash
curl -X PUT http://localhost:8080/api/mode \
  -H "Content-Type: application/json" \
  -d '{"mode":"UNSAFE"}'
```

### Run Race Condition Demo
```bash
curl -X POST http://localhost:8080/api/simulation/race-condition
```

## 🏛️ Architecture

```
com.bolt/
├── BoltApplication.java              # Spring Boot entry point
├── config/
│   ├── CorsConfig.java               # CORS for frontend integration
│   └── ExecutorConfig.java            # Named thread pools (ATM/ONLINE)
├── controller/
│   ├── AccountController.java         # Account REST endpoints
│   ├── TransactionController.java     # Transaction REST endpoints
│   ├── SimulationController.java      # Simulation REST endpoints
│   ├── DashboardController.java       # Dashboard REST endpoints
│   └── ModeController.java            # Mode switching endpoint
├── service/
│   ├── AccountService.java            # Account management
│   ├── TransactionService.java        # Transaction persistence
│   ├── BankingService.java            # Core banking operations
│   ├── SimulationService.java         # Simulation orchestration
│   ├── DeadlockService.java           # Deadlock demonstration
│   └── DashboardService.java          # Dashboard statistics
├── concurrency/
│   ├── BankingExecutionStrategy.java  # Strategy interface
│   ├── SafeBankingStrategy.java       # Synchronized operations
│   ├── UnsafeBankingStrategy.java     # Unsynchronized operations
│   ├── TransactionExecutor.java       # Thread pool executor
│   └── DeadlockSimulator.java         # Deadlock scenarios
├── model/
│   ├── BankAccount.java               # Account entity (@Version)
│   └── Transaction.java               # Transaction entity
├── dto/                               # Request/Response DTOs
├── repository/                        # JPA repositories
├── enums/                             # ExecutionMode, TransactionType, etc.
├── exception/                         # Centralized error handling
└── util/
    └── TransactionIdGenerator.java    # Thread-safe ID generation
```

## 🔒 Concurrency Design

### In-Memory vs Database Concurrency

| Aspect | In-Memory (Simulation) | Database (Normal Ops) |
|--------|----------------------|----------------------|
| Protection | ReentrantLock | ReentrantLock + @Version |
| Scope | Single JVM | Survives DB conflicts |
| Race Condition Demo | Uses raw objects | Not applicable |
| Lost Updates | Visible in UNSAFE mode | Caught by optimistic lock |

The race-condition demonstration operates on **in-memory BankAccount objects** to show raw thread interleaving. Normal banking operations persist through JPA with **optimistic locking** (`@Version`), so even if the in-memory lock is bypassed, the database will reject stale writes.

## 📊 Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=BankingServiceTest

# Run with verbose output
./mvnw test -Dtest=ConcurrencyTest -Dsurefire.useFile=false
```

### Test Coverage
- Account CRUD operations
- Deposit, withdrawal, transfer
- Insufficient balance handling
- Concurrent deposits (SAFE mode)
- Concurrent withdrawals (SAFE mode)
- Race condition demonstration (UNSAFE mode)
- Lost update demonstration (UNSAFE mode)
- Deadlock demonstration
- Deadlock prevention
- Mode switching
- Transaction logging
- Dashboard statistics
- REST API endpoints

## 🎨 Frontend UI Design & Architecture (Prompt 2)

The frontend is an **Apple-inspired financial technology interface** incorporating modern glassmorphism, subtle blur reflections, and the exact color palette derived from [Hrithik's Portfolio](https://a-hrithik-4.github.io/Portfolio/):

| Color Token | Hex / Value | Semantic Role |
|---|---|---|
| `--c-sapphire` | `#3D52A0` | Royal Sapphire Tech Indigo (Brand Primary) |
| `--c-periwinkle` | `#7091E6` | Vibrant Periwinkle Sky (Accent & Glows) |
| `--c-heather` | `#8697C4` | Muted Blue Slate (Neutral Accents) |
| `--c-mist` | `#ADBBDA` | Pastel Ice Mist Blue (Borders & Highlights) |
| `--c-lilac-base` | `#EDE8F5` | Pale Lavender White Frost Base (Canvas) |
| `--text-heading` | `#1B2446` | Deep Royal Navy Charcoal |
| `--text-body` | `#374263` | Slate Indigo Charcoal |
| `--safe-mode` | `#10B981` | Emerald Green (Mutual Exclusion ON) |
| `--unsafe-mode`| `#EF4444` | Coral Red (Race Conditions Active) |

### Frontend Directory Structure

```
frontend/
├── index.html            # Dashboard Overview & Financial Charts
├── accounts.html         # Accounts Management, Balances & Details
├── transactions.html     # Real-time Filterable Transaction History
├── simulations.html      # Multithreading Batch Engine & Race Condition Lab
├── concurrency.html      # Critical Section & Mutual Exclusion Visualizer
├── deadlock.html         # Circular Wait & Deadlock Prevention Visualizer
├── settings.html         # Design Tokens, Palette Specs & System Profile
├── css/
│   ├── base.css          # Design tokens, typography & ambient glass orbs
│   ├── layout.css        # Glass sidebar dock, top navigation, macOS layout
│   ├── components.css    # Cards, buttons, tables, modals, badges, toggles
│   ├── dashboard.css     # Financial charts, metric cards & thread monitor
│   ├── animations.css    # Apple spring transitions & visualizer keyframes
│   └── responsive.css    # Breakpoints for desktop, tablet, and mobile
└── js/
    ├── app.js            # API client (http://localhost:8080/api) & offline mock fallback
    ├── navigation.js     # Sidebar collapsing, mobile drawer, search shortcuts
    ├── dashboard.js      # Dashboard data binding & chart rendering
    ├── charts.js         # Canvas-based smooth spline financial charts
    ├── ui.js             # Modal manager, toast notifications & mode switcher
    └── simulations.js    # Interactive race condition & deadlock simulations
```

### Running the Frontend

To view the frontend, simply open `frontend/index.html` in any web browser, or serve it locally:

```bash
cd frontend
python3 -m http.server 3000
```
Then visit **http://localhost:3000** in your browser.

*Note: The frontend operates seamlessly both with the active Spring Boot backend (at `http://localhost:8080`) and in standalone mode with realistic simulation data.*

---

## ⚠️ Disclaimer

**This is an academic simulation project, not production banking software.**
The UNSAFE mode intentionally introduces concurrency bugs for educational purposes.

---

**BOLT** — Banking Operations & Locking Technology
*Operating Systems (CSC304A)*
