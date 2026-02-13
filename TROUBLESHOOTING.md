# Troubleshooting Guide

## Service Startup Issues

### ❌ Eureka Server won't start
```bash
# Check if port 8761 is available
lsof -i :8761

# Check logs
docker logs banking-ledger-eureka

# Solution: Kill process on port 8761
sudo lsof -ti:8761 | xargs kill -9
```

### ❌ Account Service fails to connect to database
**Error**: `Connection refused at 5432`

```bash
# Verify PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs banking-ledger-db

# Verify environment variables
docker inspect banking-ledger-account-service | grep -A 10 Env

# Restart database
docker-compose restart postgres
```

### ❌ Transfer Service Feign client fails
**Error**: `FeignException: 404 Not Found`

```bash
# Check if Account Service is running
curl http://localhost:8081/account-service/actuator/health

# Check Eureka registration
curl http://localhost:8761/eureka/apps/ACCOUNT-SERVICE

# Restart Transfer Service
docker-compose restart transfer-service
```

---

## Database Issues

### ❌ Database tables not created
```bash
# Connect to database
docker exec -it banking-ledger-db psql -U postgres -d banking_ledger

# Check tables
\dt

# Check logs
docker logs banking-ledger-db

# Force Hibernate to create tables
# Set in application.yml: spring.jpa.hibernate.ddl-auto: create-drop
```

### ❌ Transaction isolation issues
**Symptom**: Concurrent transfers cause deadlocks

```sql
-- Check active connections
SELECT * FROM pg_stat_activity;

-- Kill blocking process
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE ...;

-- Check locks
SELECT * FROM pg_locks;
```

### ❌ Connection pool exhausted
**Error**: `HikariPool - Connection is not available`

```yaml
# Increase connection pool in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase from 10
      minimum-idle: 5
```

---

## Kafka Issues

### ❌ Kafka broker not responding
```bash
# Check if Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs banking-ledger-kafka

# Verify broker connectivity
docker exec banking-ledger-kafka kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092
```

### ❌ Messages not being consumed
```bash
# Check topic exists
docker exec banking-ledger-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list

# Check consumer group
docker exec banking-ledger-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --describe

# Restart Notification Service
docker-compose restart notification-service
```

### ❌ Messages stuck in queue
```bash
# Reset consumer group offset
docker exec banking-ledger-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --reset-offsets --to-earliest --execute

# Or skip offsets
--reset-offsets --to-latest --execute
```

---

## API/Gateway Issues

### ❌ API Gateway returns 502 Bad Gateway
```bash
# Check if services are registered
curl http://localhost:8761/eureka/apps

# Check if service is healthy
curl http://localhost:8082/actuator/health

# Restart API Gateway
docker-compose restart api-gateway
```

### ❌ Cross-origin (CORS) errors
```java
// Add to API Gateway config
@Bean
public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.addAllowedMethod("*");
    corsConfig.addAllowedHeader("*");
    corsConfig.addAllowedOrigin("*");
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    
    return new CorsWebFilter(source);
}
```

### ❌ Request timeout
```yaml
# Increase timeout in application.yml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000  # 5 seconds
        response-timeout: 10s  # 10 seconds
```

---

## Performance Issues

### ⚠️ High database CPU
```sql
-- Find slow queries
SELECT query, mean_exec_time, calls 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC LIMIT 10;

-- Add index
CREATE INDEX idx_transfers_from_iban ON transfers(from_iban);
CREATE INDEX idx_transfers_to_iban ON transfers(to_iban);
CREATE INDEX idx_transfers_transaction_id ON transfers(transaction_id);
```

### ⚠️ Memory usage increasing
```bash
# Monitor service memory
docker stats banking-ledger-account-service

# Increase heap size
export JAVA_OPTS="-Xmx512m -Xms256m"

# Add to docker-compose.yml
environment:
  JAVA_OPTS: "-Xmx512m -Xms256m"
```

### ⚠️ Slow API responses
```bash
# Enable SQL logging
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# Profile the service
# Add -Dspring.profiles.active=debug
```

---

## Network Issues

### ❌ Services can't communicate
```bash
# Check Docker network
docker network ls

# Inspect network
docker network inspect banking-network

# Verify service DNS
docker exec banking-ledger-account-service \
  nslookup banking-ledger-db

# Check firewall
sudo ufw status
```

### ❌ Port conflicts
```bash
# Find process using port
lsof -i :8080
netstat -tlnp | grep 8080

# Kill process
sudo kill -9 <PID>

# Or change port in application.yml
server:
  port: 8085
```

---

## Data/State Issues

### 🔄 Wrong account balances
```sql
-- Check account state
SELECT * FROM accounts WHERE iban = 'TR330006100519786457841326';

-- Check all transfers
SELECT * FROM transfers ORDER BY created_at DESC;

-- Calculate correct balance
SELECT SUM(amount) as outgoing FROM transfers 
WHERE from_iban = 'TR330006100519786457841326' AND status = 'SUCCESS';

SELECT SUM(amount) as incoming FROM transfers 
WHERE to_iban = 'TR330006100519786457841326' AND status = 'SUCCESS';
```

### 🔄 Duplicate transfers
```sql
-- Find duplicates
SELECT transaction_id, COUNT(*) 
FROM transfers 
GROUP BY transaction_id 
HAVING COUNT(*) > 1;

-- Find and fix
SELECT * FROM transfers WHERE transaction_id = '...';
DELETE FROM transfers WHERE id = ... AND ... ;  -- Manual cleanup
```

### 🔄 Failed transfers stuck in PENDING
```sql
-- Find stuck transfers
SELECT * FROM transfers WHERE status = 'PENDING' 
AND created_at < NOW() - INTERVAL '5 minutes';

-- Mark as failed
UPDATE transfers SET status = 'FAILED' 
WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL '5 minutes';
```

---

## Monitoring & Debugging

### 📊 View Real-time Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f account-service

# Filter by service and level
docker logs -f banking-ledger-transfer-service | grep ERROR

# Follow last 100 lines
docker logs --tail 100 -f banking-ledger-account-service
```

### 🔍 Check Service Health
```bash
# Account Service
curl http://localhost:8081/actuator/health | jq

# Transfer Service
curl http://localhost:8082/actuator/health | jq

# All endpoints
curl http://localhost:8081/actuator | jq
```

### 📈 View Metrics
```bash
# Get metrics
curl http://localhost:8081/actuator/metrics | jq

# Specific metric
curl http://localhost:8081/actuator/metrics/http.server.requests | jq

# Custom metric
curl http://localhost:8082/actuator/metrics/transfer.count | jq
```

---

## Full System Restart

### 🔧 Clean Restart
```bash
# Stop all services
docker-compose down

# Remove volumes (fresh database)
docker-compose down -v

# Remove images
docker-compose down --rmi all

# Rebuild and start
docker-compose up -d

# Wait for services
sleep 30

# Verify
docker-compose ps
curl http://localhost:8761/eureka/apps
```

### ⚡ Quick Restart (keep data)
```bash
# Restart all services
docker-compose restart

# Or specific service
docker-compose restart account-service
```

---

## Emergency Fixes

### 🚨 Account balance corrupted
```sql
-- Backup
COPY accounts TO '/tmp/accounts_backup.csv' WITH (FORMAT csv);

-- Recalculate balance
UPDATE accounts a 
SET balance = (
  SELECT COALESCE(initial_balance, 0) - 
         COALESCE((SELECT SUM(amount) FROM transfers 
                   WHERE from_iban = a.iban AND status = 'SUCCESS'), 0) +
         COALESCE((SELECT SUM(amount) FROM transfers 
                   WHERE to_iban = a.iban AND status = 'SUCCESS'), 0)
);
```

### 🚨 Database locked
```bash
# Restart database
docker-compose restart postgres

# Or connect and force cleanup
docker exec banking-ledger-db psql -U postgres -c \
  "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'banking_ledger';"
```

### 🚨 Services in deadlock
```bash
# Kill and restart
docker-compose restart account-service transfer-service

# Check status
sleep 10
docker-compose ps
```

---

## Getting Help

If issues persist:

1. **Check logs first**
   ```bash
   docker-compose logs | tail -200
   ```

2. **Verify all services running**
   ```bash
   docker-compose ps
   ```

3. **Test connectivity**
   ```bash
   docker exec banking-ledger-api-gateway \
     curl http://account-service:8081/actuator/health
   ```

4. **Check resource constraints**
   ```bash
   docker stats
   ```

5. **Review documentation**
   - See `ARCHITECTURE.md` for design
   - See `API_DOCUMENTATION.md` for endpoints
   - See `QUICKSTART.md` for setup

---

**Still need help? Check the logs! Most issues are revealed there.** 🔍
