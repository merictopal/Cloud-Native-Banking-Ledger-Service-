# Distributed Banking Ledger - Architecture Documentation

## 🏛️ System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CLIENT / BROWSER / POSTMAN                      │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ HTTP/REST
                    ┌────────────▼──────────────┐
                    │     API Gateway           │
                    │   (Port 8080)             │
                    │ Spring Cloud Gateway      │
                    │ - Route Management        │
                    │ - Request Tracing         │
                    │ - Load Balancing          │
                    └────┬──────────┬───────┬──────────┘
                         │          │       │
            ┌────────────┴─┐    ┌──┴───┐ ┌─┴──────────┐
            │              │    │      │ │            │
            ▼              ▼    ▼      ▼ ▼            ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────────┐ ┌──────────┐
    │ Account Service │ │Transfer      │ │Notification      │ │  Eureka  │
    │ (Port 8081)     │ │Service       │ │Service           │ │ Discovery│
    │                 │ │(Port 8082)   │ │(Port 8083)       │ │(8761)    │
    │ - Create Acct   │ │              │ │                  │ └──────────┘
    │ - Get Acct      │ │- Execute Xfer│ │- Email Notif     │
    │ - List Accts    │ │- Debit/Credit│ │- SMS Simulation  │
    │ - Debit/Credit  │ │- ACID Safe   │ │- Kafka Consumer  │
    │   (Internal)    │ │- Transactional│ │                  │
    └────────┬────────┘ └──────┬──────┘ └────────┬─────────┘
             │                 │                 │
             │                 └────────┬────────┘
             │                          │
    ┌────────┴──────────────────────────┴──────────────┐
    │                                                  │
    │          Shared Infrastructure                  │
    │                                                  │
    ├──────────────┬─────────────────┬───────────────┤
    │              │                 │               │
    ▼              ▼                 ▼               ▼
┌────────────┐ ┌──────────────┐ ┌────────────┐ ┌──────────┐
│PostgreSQL  │ │Apache Kafka  │ │MailHog    │ │Docker    │
│ Database   │ │ Message      │ │Email Test │ │Compose   │
│            │ │ Broker       │ │Server     │ │          │
│- Accounts  │ │              │ │           │ │Orchestr- │
│- Transfers │ │Topics:       │ │- SMTP     │ │ation     │
│            │ │transfer-     │ │- HTTP UI  │ │          │
│Port: 5432  │ │events        │ │Port: 1025 │ │          │
│            │ │Port: 9092    │ │Port: 8025 │ │          │
└────────────┘ └──────────────┘ └────────────┘ └──────────┘
```

## 🔄 Request Flow - Transfer Scenario

```
START: Client initiates transfer
│
├─ [1] API Gateway receives POST /transfers
│       ├─ Validates request format
│       ├─ Adds X-Request-ID header (tracing)
│       └─ Routes to Transfer Service
│
├─ [2] Transfer Service receives request
│       ├─ Validates IBAN format
│       ├─ Generates unique Transaction ID (UUID)
│       ├─ Creates PENDING transfer record in DB
│       └─ @Transactional START
│
├─ [3] Verify source & destination accounts
│       ├─ Feign call to Account Service
│       ├─ Check account existence
│       └─ Validate accounts are ACTIVE
│
├─ [4] DEBIT operation (FROM account)
│       ├─ Check balance >= transfer amount
│       ├─ SQL UPDATE: balance - amount
│       ├─ DB commit
│       └─ If FAIL → ROLLBACK entire transaction
│
├─ [5] CREDIT operation (TO account)
│       ├─ SQL UPDATE: balance + amount
│       ├─ DB commit
│       └─ If FAIL → ROLLBACK (debit also reversed!)
│
├─ [6] Update Transfer status: SUCCESS
│       ├─ SQL UPDATE transfer status
│       └─ @Transactional COMMIT
│
├─ [7] Publish event to Kafka (NON-BLOCKING)
│       ├─ Create TransferEvent object
│       ├─ Send to Kafka topic: transfer-events
│       └─ Return response immediately to client
│
├─ [8] Notification Service (Async - Different Thread)
│       ├─ Kafka Consumer detects event
│       ├─ Send email notification
│       ├─ Log SMS (simulated)
│       └─ Complete
│
└─ END: Client receives response with transaction ID
```

## 💾 Database Transaction Flow (ACID)

```
SCENARIO: Transfer 500 TRY from Ahmet to Fatih

TIME    ACCOUNT_SERVICE              TRANSFER_SERVICE         DB STATE
────    ───────────────────          ────────────────────     ────────

T0                                                             Ahmet: 5000
                                                               Fatih: 2000

T1      [Feign Call] ──────────>     Verify Accounts
        Gets account details    <──  Are valid?

T2                               START TRANSACTION

T3      Await DEBIT ────────────>    Lock Ahmet's Account

T4      Balance Check: 5000 >= 500                             Locked

T5      UPDATE balance = 4500        DEBIT Success            Ahmet: 4500*
                                                               (*locked)

T6      Await CREDIT ────────────>   Lock Fatih's Account

T7      UPDATE balance = 2500        CREDIT Success           Locked

T8                               COMMIT TRANSACTION            Ahmet: 4500
                                                               Fatih: 2500

T9      Event Published ─────>       To Kafka
        
T10                             Async ─> Notification          (Parallel)
                                       Email sent
```

## 🔐 ACID Principles Implementation

### A - Atomicity ✓
```
@Transactional // Spring-managed transaction
public TransferResponse executeTransfer(TransferRequest request) {
    // ALL steps execute or NONE
    debitFromAccountService();      // Step 1
    creditToAccountService();       // Step 2
    updateTransferStatus();         // Step 3
    // If any step fails → entire transaction rolled back
}
```

### C - Consistency ✓
```
Rules enforced:
- Account balance can never be negative
- Transfer amount must be > 0
- Both accounts must exist
- Transfer status: PENDING → SUCCESS/FAILED (only)
```

### I - Isolation ✓
```
PostgreSQL Configuration:
- Isolation Level: READ_COMMITTED (default)
- Prevents dirty reads
- Prevents non-repeatable reads
- Concurrent transfers on different accounts: Safe
- Concurrent transfers on same account: Serialized
```

### D - Durability ✓
```
PostgreSQL WAL (Write-Ahead Logging):
1. Write to WAL log (immediate disk write)
2. Apply change to data pages (in memory)
3. Periodic checkpoint flushes to disk

→ If system crashes after COMMIT:
  Recovery process reads WAL and reconstructs state
→ Data is NEVER lost
```

## 📡 Message-Driven Architecture (Kafka)

```
TRANSFER_SERVICE (Producer)          KAFKA BROKER              NOTIFICATION_SERVICE
─────────────────────                ────────────               ──────────────────

Transfer completed
    ├─ Create TransferEvent
    ├─ Serialize to JSON
    ├─ Send to Kafka ──────────────> transfer-events topic ───────> @KafkaListener
    │                                                               Process event
    │                                                               Send Email
    │                                               (Non-blocking)
    └─ Return response to client                    ← Service continues
       (Transfer Service doesn't wait)              ← Notification processing
```

**Benefits:**
- ✅ Decoupled services (Transfer doesn't depend on Notification)
- ✅ Scalable (can add more Notification instances)
- ✅ Resilient (Kafka persists events)
- ✅ Non-blocking (Transfer response immediate)

## 🌐 Service Discovery (Eureka)

```
STARTUP SEQUENCE:

1. Eureka Server starts
   └─ Listens on port 8761
   └─ Dashboard at http://localhost:8761/eureka

2. Each microservice starts
   ├─ Reads eureka.client.serviceUrl.defaultZone
   ├─ Registers itself with Service Name (from spring.application.name)
   ├─ Registers with instance metadata (port, IP, status)
   └─ Sends heartbeat every 30 seconds

3. Service-to-Service Communication
   ├─ Transfer Service needs Account Service
   ├─ Uses AccountServiceClient (Feign)
   ├─ Feign contacts Eureka
   ├─ Eureka returns available instances
   ├─ Feign picks one (load balanced)
   └─ Communication established

4. If service goes down
   ├─ Heartbeat stops
   ├─ Eureka marks as DOWN after 90 seconds
   ├─ Clients stop sending traffic
   ├─ After 5 minutes → instance removed
```

## 🚀 API Gateway Routing

```
CLIENT REQUEST

┌─ GET /accounts ──────┐
│ GET /accounts/id/1   │
└─ POST /accounts ─────┘
        │
        ▼
   API GATEWAY
   (Port 8080)
        │
        ├─ Path: /accounts/* ?
        │   YES ──> RewritePath to /account-service/api/v1/accounts/*
        │          Route to http://account-service:8081
        │
        ├─ Path: /transfers/* ?
        │   YES ──> RewritePath to /transfer-service/api/v1/transfers/*
        │          Route to http://transfer-service:8082
        │
        ├─ Path: /notifications/* ?
        │   YES ──> RewritePath to /notification-service/api/v1/notifications/*
        │          Route to http://notification-service:8083
        │
        └─ Path matches?
           NO ──> 404 Not Found

All routes include:
- X-Request-ID header (unique per request)
- Circuit breaker protection
- Retry logic
- Timeout management
```

## 📊 Data Flow - Complete Picture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     CLIENT APPLICATION                              │
│               (Postman, Frontend, Mobile App)                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ REST API
                               ▼
                    ┌──────────────────────┐
                    │    API GATEWAY       │
                    │   Port 8080          │
                    │ Spring Cloud Gateway │
                    └────┬──────────┬─────┬┘
                         │          │     │
          ┌──────────────┴─┐    ┌──┴──┐  └─────────────┐
          │                │    │     │                │
          ▼                ▼    ▼     ▼                ▼
    ┌─────────────────┐ ┌──────────────┐ ┌───────────────────┐ ┌────────┐
    │ ACCOUNT SERVICE │ │ TRANSFER     │ │ NOTIFICATION      │ │ EUREKA │
    │ (Synchronous)   │ │ SERVICE      │ │ SERVICE           │ │ (Reg)  │
    └────────┬────────┘ │(Sync/Async)  │ │ (Async Consumer)  │ └────────┘
             │          │              │ │                   │
             │          └──┬───────────┬┘ └────────┬──────────┘
             │             │           │          │
          ┌──┴──┐        ┌──┴──┐   ┌──┴──┐     ┌──┴──┐
          │  DB │        │  DB │   │Kafka│     │Email│
          │(R/W)│        │(R/W)│   │Topic│     │Serv │
          └─────┘        └─────┘   └─────┘     └─────┘

Key Points:
1. Account Service: Direct DB access via JPA (Transactional safe)
2. Transfer Service: 
   - Calls Account Service via Feign (sync)
   - Publishes events to Kafka (async)
   - All within @Transactional boundary
3. Notification Service: Only consumes from Kafka (one-way)
4. All services auto-register in Eureka
5. API Gateway handles routing & cross-cutting concerns
```

## 🔄 Deployment Pipeline

```
Source Code (Git)
       │
       ├─ Clone repository
       │
       ▼
   Maven Build
       ├─ Compile
       ├─ Test
       └─ Package (JAR)
       │
       ├─ Docker Build
       │   └─ Dockerfile (multi-stage build)
       │       ├─ Builder stage (Maven)
       │       └─ Runtime stage (JRE)
       │
       ├─ Docker Compose
       │   ├─ Pull base images
       │   ├─ Create network
       │   ├─ Start PostgreSQL
       │   ├─ Start Kafka + Zookeeper
       │   ├─ Start all microservices
       │   ├─ Wait for health checks
       │   └─ Services ready
       │
       ▼
   Running System
       ├─ API Gateway on 8080
       ├─ Services on 8081-8083
       ├─ Eureka on 8761
       ├─ Database on 5432
       └─ Kafka on 9092
```

## 🛡️ Error Handling Strategy

```
Error Scenarios:

1. Account doesn't exist
   ├─ Transfer Service queries Account Service
   ├─ Feign client receives 404
   ├─ RuntimeException thrown
   ├─ @Transactional rolls back transfer
   └─ Client receives 500 Internal Error

2. Insufficient balance
   ├─ Debit operation checks balance
   ├─ IllegalArgumentException thrown
   ├─ Transaction rolls back
   ├─ Both accounts unchanged
   └─ Client receives 400 Bad Request

3. Network error (Kafka down)
   ├─ Transfer completes successfully
   ├─ Notification attempt fails
   ├─ Kafka sends to dead-letter queue
   ├─ Retry mechanism kicks in
   └─ Client receives success (eventual notification)

4. Service unavailable (Account Service down)
   ├─ Feign client fails
   ├─ Transfer Service returns error
   ├─ Eureka marks service down
   ├─ API Gateway routes to healthy instance (if any)
   └─ Circuit breaker prevents cascading failure
```

## 🎯 Scalability Considerations

```
Horizontal Scaling:

Account Service
    ├─ Stateless (can run multiple instances)
    ├─ Load balanced by API Gateway
    ├─ All read from same database
    ├─ Scaling factor: Database connections
    └─ Max instances: CPU / Connection pool

Transfer Service
    ├─ Stateless (can run multiple instances)
    ├─ Database lock ensures consistency
    ├─ Transactions serialized at DB level
    ├─ Scaling factor: Number of transfers
    └─ Max instances: Limited by DB transaction speed

Notification Service
    ├─ Stateless (can run multiple instances)
    ├─ Kafka consumer group: Each gets partition
    ├─ Parallel email sending
    ├─ Scaling factor: Email throughput
    └─ Max instances: Number of Kafka partitions

Database (PostgreSQL)
    ├─ Single master (not distributed)
    ├─ Can replicate to read-only replicas
    ├─ Write throughput limited
    └─ Scaling strategy: Connection pooling, indexing

Message Broker (Kafka)
    ├─ Distributed by nature
    ├─ Multiple partitions: Parallel consumption
    ├─ Replication: High availability
    └─ Topics auto-create on demand
```

## 📋 Deployment Checklist

- [ ] Clone repository
- [ ] Install Java 21
- [ ] Install Docker & Docker Compose
- [ ] Build all services: `mvn clean package`
- [ ] Start infrastructure: `docker-compose up -d`
- [ ] Wait for services to be healthy
- [ ] Test account creation endpoint
- [ ] Test transfer endpoint
- [ ] Verify email in MailHog
- [ ] Check Eureka dashboard
- [ ] Monitor logs for errors
- [ ] Run load tests
- [ ] Verify rollback behavior
- [ ] Document findings

---

**This architecture provides enterprise-grade reliability, scalability, and maintainability.**
