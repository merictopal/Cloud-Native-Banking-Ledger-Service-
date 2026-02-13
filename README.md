# Distributed Banking Ledger - Microservices Architecture

## 📋 Proje Özeti

**Distributed Banking Ledger**, kurumsal bankacılık sistemlerinde kullanılan dağıtık mimarinin tam uygulamasıdır. ACID prensipleri, transactional güvenlik, asenkron iletişim ve microservices pattern'ları içeren production-grade bir çözümdür.

Bu proje, Java/Spring Boot ekosisteminde:
- ✅ **Microservices Architecture** (Monolitik değil)
- ✅ **ACID Compliance** (Transaction güvenliği)
- ✅ **Asenkron iletişim** (Kafka ile event-driven)
- ✅ **Service Discovery** (Eureka)
- ✅ **API Gateway Pattern**
- ✅ **Containerized Deployment** (Docker & Docker Compose)

## 🏗️ Mimarı

```
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway (Port 8080)                    │
│                  (Spring Cloud Gateway)                         │
└──────────┬──────────────┬──────────────┬──────────────┬─────────┘
           │              │              │              │
    ┌──────▼──────┐ ┌────▼──────┐ ┌───▼────────┐ ┌──▼────────┐
    │   Account   │ │  Transfer │ │Notification│ │  Eureka   │
    │  Service    │ │  Service  │ │  Service   │ │  Server   │
    │ (8081)      │ │ (8082)    │ │  (8083)    │ │ (8761)    │
    └──────┬──────┘ └────┬──────┘ └───┬────────┘ └──────────┘
           │              │            │
           └──────────────┼────────────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
     ┌────▼────┐  ┌──────▼──────┐  ┌────▼─────┐
     │PostgreSQL│  │   Kafka    │  │ MailHog  │
     │Database  │  │   Broker   │  │(Email)   │
     └──────────┘  └────────────┘  └──────────┘
```

## 🔧 Teknolojiler

- **Java 21** - Modern Java features
- **Spring Boot 3.2** - Lightweight framework
- **Spring Cloud** - Microservices patterns
- **PostgreSQL 16** - Relational database
- **Apache Kafka 7.5** - Message broker
- **Docker & Docker Compose** - Containerization
- **Maven** - Build tool
- **Eureka** - Service discovery

## 📦 Servisler

### 1️⃣ Account Service (Port 8081)
Müşteri hesaplarını yönetir.

**Endpoints:**
```bash
# Hesap oluştur
POST /accounts
{
  "iban": "TR330006100519786457841326",
  "accountHolder": "Ahmet Yilmaz",
  "initialBalance": 10000.00,
  "currency": "TRY"
}

# Hesap görüntüle
GET /accounts/{iban}

# Tüm hesapları listele
GET /accounts
```

**Database Schema:**
```sql
CREATE TABLE accounts (
  id BIGSERIAL PRIMARY KEY,
  iban VARCHAR(255) UNIQUE NOT NULL,
  account_holder VARCHAR(255) NOT NULL,
  balance DECIMAL(19,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

### 2️⃣ Transfer Service (Port 8082)
Para transferleri gerçekleştirir. **KRITIK**: ACID prensipleri ile transactional güvenlik sağlar.

**Endpoints:**
```bash
# Transfer yap (ATOMIK İŞLEM)
POST /transfers
{
  "fromIban": "TR330006100519786457841326",
  "toIban": "TR440006100519786457841326",
  "amount": 100.00,
  "currency": "TRY",
  "description": "Monthly payment"
}

# Transfer detayı görüntüle
GET /transfers/{id}
GET /transfers/transaction/{transactionId}
```

**Key Features:**
- ✅ **Transactional (@Transactional)** - Spring tarafından yönetilen transaction
- ✅ **Rollback garantisi** - Hata durumunda otomatik geri alma
- ✅ **Debit/Credit atomicity** - Para birinden çıkıp diğerine gitmesi garantili
- ✅ **Kafka event publishing** - Transfer sonrası async notification

**Database Schema:**
```sql
CREATE TABLE transfers (
  id BIGSERIAL PRIMARY KEY,
  from_iban VARCHAR(255) NOT NULL,
  to_iban VARCHAR(255) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(50) NOT NULL,
  transaction_id VARCHAR(255) UNIQUE NOT NULL,
  description VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

### 3️⃣ Notification Service (Port 8083)
Kafka'dan gelen transfer event'lerini dinler ve email/SMS gönderir.

**Features:**
- 🔔 **Kafka Consumer** - transfer-events topic'ini dinler
- 📧 **Email notifications** - Transfer başarısı/başarısızlığını haber verir
- 📱 **SMS simulation** - SMS gönderimi simüle eder
- ⚡ **Non-blocking** - Asenkron işlem, Transfer Service'yi engellemiyor

**Kafka Topic:**
```
Topic: transfer-events
Partition: 1
Replication: 1

Event Schema:
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "fromIban": "TR330006100519786457841326",
  "toIban": "TR440006100519786457841326",
  "amount": 100.00,
  "status": "SUCCESS",
  "description": "Monthly payment",
  "timestamp": "2024-02-13T10:30:00",
  "recipientEmail": "user@bank.com",
  "recipientPhone": "+90555555555"
}
```

### 4️⃣ API Gateway (Port 8080)
Tüm servislere tek noktadan erişim sağlar.

**Routing:**
```
/accounts/* -> Account Service (8081)
/transfers/* -> Transfer Service (8082)
/notifications/* -> Notification Service (8083)
```

**Request tracing:**
- ✅ Tüm requestlere X-Request-ID header'ı eklenir
- ✅ Microservices arasında request tracking

### 5️⃣ Eureka Server (Port 8761)
Service discovery ve health monitoring.

**Dashboard:**
```
http://localhost:8761/eureka
```

## 🚀 Kurulum ve Çalıştırma

### Prerequisite
- Docker & Docker Compose
- Java 21 (local development için)
- Maven 3.9+

### Docker ile Çalıştırma (Önerilen)

```bash
# Project root'a git
cd Cloud-Native\ Banking\ Ledger\ Service

# Tüm servisleri başlat
docker-compose up -d

# Servislerin durumunu kontrol et
docker-compose ps

# Logs'u takip et
docker-compose logs -f

# Durdur
docker-compose down
```

### Local Development (IDE'de)

**Sırasıyla başlat:**

1. **Eureka Server:**
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```

2. **Account Service:**
   ```bash
   cd account-service
   mvn spring-boot:run
   ```

3. **Transfer Service:**
   ```bash
   cd transfer-service
   mvn spring-boot:run
   ```

4. **Notification Service:**
   ```bash
   cd notification-service
   mvn spring-boot:run
   ```

5. **API Gateway:**
   ```bash
   cd api-gateway
   mvn spring-boot:run
   ```

## 📊 Test Senaryosu

### Senaryo: Para Transfer ve Notification

```bash
# 1. Hesap oluştur (Gönderici)
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "iban": "TR330006100519786457841326",
    "accountHolder": "Ahmet Yilmaz",
    "initialBalance": 5000,
    "currency": "TRY"
  }'

# 2. Hesap oluştur (Alıcı)
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "iban": "TR440006100519786457841326",
    "accountHolder": "Fatih Kaya",
    "initialBalance": 2000,
    "currency": "TRY"
  }'

# 3. Para transfer yap
curl -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromIban": "TR330006100519786457841326",
    "toIban": "TR440006100519786457841326",
    "amount": 500,
    "currency": "TRY",
    "description": "Payment for invoice #123"
  }'

# 4. Transfer durumunu kontrol et
curl http://localhost:8080/transfers/{id}

# 5. Email'i MailHog'da kontrol et
# Browser: http://localhost:8025
```

## 🔐 ACID Prensipleri

### Atomicity
```java
@Transactional
public TransferResponse executeTransfer(TransferRequest request) {
    // Tüm operasyonlar veya hiçbiri
    debitFromAccount();      // Başarısız olsa
    creditToAccount();       // otomatik rollback
}
```

### Consistency
- Account balance her zaman pozitif
- Transfer amount tutarlılığı
- Status transitions valid

### Isolation
- PostgreSQL Transaction Isolation Level
- Concurrent transfer'lar güvenli

### Durability
- PostgreSQL WAL (Write-Ahead Logging)
- Data persistence garantisi

## 📈 Monitoring ve Observability

### Health Checks
```bash
# Account Service health
curl http://localhost:8081/actuator/health

# Transfer Service health
curl http://localhost:8082/actuator/health

# Notification Service health
curl http://localhost:8083/actuator/health
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8081/actuator/metrics
```

### Logs
```bash
# Docker logs
docker logs -f banking-ledger-account-service
docker logs -f banking-ledger-transfer-service
docker logs -f banking-ledger-notification-service
```

## 🎯 Proje Yapısı

```
Cloud-Native Banking Ledger Service/
├── account-service/
│   ├── src/main/java/
│   │   └── com/bankingledger/accountservice/
│   │       ├── controller/      (REST endpoints)
│   │       ├── service/         (business logic)
│   │       ├── entity/          (JPA models)
│   │       ├── repository/      (data access)
│   │       └── dto/             (data transfer objects)
│   ├── pom.xml
│   └── Dockerfile
├── transfer-service/
│   ├── src/main/java/
│   │   └── com/bankingledger/transferservice/
│   │       ├── controller/
│   │       ├── service/         (TRANSACTIONAL LOGIC)
│   │       ├── entity/
│   │       ├── repository/
│   │       ├── dto/
│   │       ├── client/          (Feign client)
│   │       ├── event/           (Kafka events)
│   │       └── config/
│   ├── pom.xml
│   └── Dockerfile
├── notification-service/
│   ├── src/main/java/
│   │   └── com/bankingledger/notificationservice/
│   │       ├── service/         (Email/SMS)
│   │       ├── consumer/        (Kafka consumer)
│   │       ├── event/
│   │       └── config/
│   ├── pom.xml
│   └── Dockerfile
├── api-gateway/
│   ├── src/main/java/
│   │   └── com/bankingledger/apigateway/
│   │       ├── filter/          (request filters)
│   │       └── config/
│   ├── pom.xml
│   └── Dockerfile
├── eureka-server/
│   ├── src/main/java/
│   └── pom.xml
├── docker-compose.yml
└── README.md
```

## 🎓 Learning Outcomes

Bu projeden kazanılan yetkinlikler:

1. **Microservices Architecture**
   - Service boundaries tanımlama
   - Inter-service communication
   - Service discovery patterns

2. **Transaction Management (ACID)**
   - Spring @Transactional
   - Distributed transactions
   - Rollback strategies

3. **Asenkron İletişim**
   - Message-driven architecture
   - Event sourcing
   - Kafka producer/consumer

4. **Spring Cloud**
   - Eureka service discovery
   - Spring Cloud Gateway
   - Feign clients

5. **Container Orchestration**
   - Docker & Docker Compose
   - Multi-container application
   - Service networking

6. **Database Design**
   - Relational modeling
   - JPA/Hibernate
   - Concurrent access patterns

## 🔄 CI/CD Ready

Bu proje, production'a deploy etmeye hazır:

- ✅ Docker support
- ✅ Health checks
- ✅ Graceful shutdown
- ✅ Actuator endpoints
- ✅ Structured logging
- ✅ Error handling

## 📝 Kaynaklar

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Kafka Documentation](https://kafka.apache.org/documentation)
- [PostgreSQL Documentation](https://www.postgresql.org/docs)
- [Microservices Patterns](https://microservices.io/patterns)

## 👤 Yazar

Distributed Banking Ledger - Java Microservices Architecture Project

---

**Bu proje, kurumsal bankacılık sistemlerinde geçerli olan tüm prensipleri içeren, production-ready bir çözümdür.**
