# Quick Start Guide - Banking Ledger Microservices

## 📋 Prerequisites

```bash
# Check Java version
java -version
# Expected: Java 21+

# Check Docker
docker --version
# Expected: Docker 20.10+

# Check Docker Compose
docker-compose --version
# Expected: Docker Compose 1.29+
```

## 🚀 Fast Start (5 minutes)

### Step 1: Navigate to Project
```bash
cd "Cloud-Native Banking Ledger Service"
```

### Step 2: Start All Services (Docker)
```bash
docker-compose up -d
```

### Step 3: Verify Services are Running
```bash
docker-compose ps
```

Expected output:
```
NAME                        STATUS
banking-ledger-eureka       Up
banking-ledger-postgres     Up
banking-ledger-kafka        Up
banking-ledger-zookeeper    Up
banking-ledger-mailhog      Up
banking-ledger-account-service     Up
banking-ledger-transfer-service    Up
banking-ledger-notification-service Up
banking-ledger-api-gateway Up
```

### Step 4: Wait for Services to be Ready
```bash
# Monitor logs
docker-compose logs -f api-gateway

# Wait for this message:
# "Starting ApiGatewayApplication v1.0.0"
# "Tomcat started on port 8080"
```

### Step 5: Test API (Create Account)

Using cURL:
```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "iban": "TR330006100519786457841326",
    "accountHolder": "Ahmet Yilmaz",
    "initialBalance": 10000,
    "currency": "TRY"
  }'
```

Or using Postman:
- **Method**: POST
- **URL**: `http://localhost:8080/accounts`
- **Body** (JSON):
  ```json
  {
    "iban": "TR330006100519786457841326",
    "accountHolder": "Ahmet Yilmaz",
    "initialBalance": 10000,
    "currency": "TRY"
  }
  ```

Expected Response:
```json
{
  "id": 1,
  "iban": "TR330006100519786457841326",
  "accountHolder": "Ahmet Yilmaz",
  "balance": 10000.00,
  "currency": "TRY",
  "status": "ACTIVE",
  "createdAt": "2024-02-13T10:30:00",
  "updatedAt": "2024-02-13T10:30:00"
}
```

---

## 🧪 Complete Test Scenario

### Scenario: Create Accounts & Transfer Money

**Request 1: Create Account A**
```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "iban": "TR330006100519786457841326",
    "accountHolder": "Ahmet",
    "initialBalance": 5000,
    "currency": "TRY"
  }'
```

**Request 2: Create Account B**
```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "iban": "TR440006100519786457841326",
    "accountHolder": "Fatih",
    "initialBalance": 2000,
    "currency": "TRY"
  }'
```

**Request 3: Transfer Money**
```bash
curl -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromIban": "TR330006100519786457841326",
    "toIban": "TR440006100519786457841326",
    "amount": 500,
    "currency": "TRY",
    "description": "Payment"
  }'
```

Expected Response:
```json
{
  "id": 1,
  "fromIban": "TR330006100519786457841326",
  "toIban": "TR440006100519786457841326",
  "amount": 500.00,
  "currency": "TRY",
  "status": "SUCCESS",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Payment",
  "createdAt": "2024-02-13T10:35:00",
  "updatedAt": "2024-02-13T10:35:00"
}
```

**Request 4: Verify Updated Balances**
```bash
# Account A should have 4500 (5000 - 500)
curl http://localhost:8080/accounts/TR330006100519786457841326 | grep balance

# Account B should have 2500 (2000 + 500)
curl http://localhost:8080/accounts/TR440006100519786457841326 | grep balance
```

**Request 5: Check Email Notification**
- Open browser: `http://localhost:8025`
- You should see the transfer notification email

---

## 🎛️ Service Dashboard URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Eureka | http://localhost:8761/eureka | Service Registry |
| MailHog | http://localhost:8025 | Email Testing |
| Account Service | http://localhost:8081/account-service/swagger-ui.html | API Docs |
| Transfer Service | http://localhost:8082/transfer-service/swagger-ui.html | API Docs |

---

## 📊 Monitoring Commands

### View All Running Containers
```bash
docker-compose ps
```

### View Logs from Specific Service
```bash
# Account Service
docker logs -f banking-ledger-account-service

# Transfer Service
docker logs -f banking-ledger-transfer-service

# Notification Service
docker logs -f banking-ledger-notification-service

# API Gateway
docker logs -f banking-ledger-api-gateway

# Follow all logs
docker-compose logs -f
```

### Check Service Health
```bash
# Eureka
curl http://localhost:8761/eureka/apps | head -20

# Account Service
curl http://localhost:8081/actuator/health | jq

# Transfer Service
curl http://localhost:8082/actuator/health | jq

# Notification Service
curl http://localhost:8083/actuator/health | jq

# API Gateway
curl http://localhost:8080/actuator/health | jq
```

### Database Queries
```bash
# Connect to PostgreSQL
docker exec -it banking-ledger-db psql -U postgres -d banking_ledger

# List all accounts
SELECT * FROM accounts;

# List all transfers
SELECT * FROM transfers;

# Check specific account balance
SELECT iban, balance FROM accounts WHERE iban = 'TR330006100519786457841326';
```

---

## 🛑 Common Issues & Solutions

### Issue: Services not starting
**Solution:**
```bash
# Check if ports are in use
docker ps

# Stop all containers
docker-compose down

# Remove volumes (fresh start)
docker-compose down -v

# Start again
docker-compose up -d
```

### Issue: Cannot connect to database
**Solution:**
```bash
# Check PostgreSQL container
docker logs banking-ledger-db

# Verify connection
docker exec banking-ledger-db psql -U postgres -c "SELECT 1"
```

### Issue: Kafka not working
**Solution:**
```bash
# Check Kafka logs
docker logs banking-ledger-kafka

# Verify Kafka is running
docker exec banking-ledger-kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### Issue: Email not sending
**Solution:**
```bash
# MailHog UI should be accessible
curl http://localhost:8025

# Check notification service logs
docker logs -f banking-ledger-notification-service
```

---

## 🧹 Cleanup

### Stop All Services
```bash
docker-compose down
```

### Stop and Remove Volumes
```bash
docker-compose down -v
```

### Remove Docker Images
```bash
docker-compose down --rmi all
```

---

## 📝 API Reference (Quick)

### Create Account
```
POST /accounts
{
  "iban": "TR...",
  "accountHolder": "Name",
  "initialBalance": 1000,
  "currency": "TRY"
}
```

### Get Account
```
GET /accounts/{iban}
GET /accounts/id/{id}
```

### List Accounts
```
GET /accounts
```

### Transfer Money
```
POST /transfers
{
  "fromIban": "TR...",
  "toIban": "TR...",
  "amount": 100,
  "currency": "TRY",
  "description": "..."
}
```

### Get Transfer
```
GET /transfers/{id}
GET /transfers/transaction/{transactionId}
```

---

## 🐛 Debugging Tips

### View Database State
```bash
# Connect to PostgreSQL
docker exec -it banking-ledger-db psql -U postgres -d banking_ledger

# Check accounts
\d accounts
SELECT * FROM accounts;

# Check transfers
\d transfers
SELECT * FROM transfers;
```

### Monitor Kafka
```bash
# List topics
docker exec banking-ledger-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 --list

# Monitor topic
docker exec banking-ledger-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic transfer-events \
  --from-beginning
```

### Check Service Registration
```bash
# Eureka dashboard
curl http://localhost:8761/eureka/apps | xmllint --format -

# Specific service
curl http://localhost:8761/eureka/apps/ACCOUNT-SERVICE
```

---

## ⚡ Performance Testing

### Basic Load Test (10 transfers)
```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/transfers \
    -H "Content-Type: application/json" \
    -d "{
      \"fromIban\": \"TR330006100519786457841326\",
      \"toIban\": \"TR440006100519786457841326\",
      \"amount\": $((RANDOM % 1000)),
      \"currency\": \"TRY\",
      \"description\": \"Test $i\"
    }" &
done
```

### Check Results
```bash
# Total transfers
curl http://localhost:8080/transfers | wc -l

# Account balances
curl http://localhost:8080/accounts | jq '.[] | {iban, balance}'
```

---

## 🎓 Next Steps

1. **Study the Code**: Review `ARCHITECTURE.md` for deep dive
2. **Experiment**: Try edge cases (insufficient balance, invalid IBAN)
3. **Scale Up**: Add more service instances
4. **Monitor**: Check logs and metrics
5. **Optimize**: Profile and improve performance
6. **Document**: Track findings and improvements

---

## 🆘 Getting Help

Check these files for more information:
- `README.md` - Project overview
- `API_DOCUMENTATION.md` - Detailed API reference
- `ARCHITECTURE.md` - System design & architecture
- `PROJECT_STRUCTURE.md` - File organization
- Logs: `docker-compose logs -f`

---

**You're all set! Happy testing! 🎉**
