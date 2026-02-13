# Contributing Guide - Banking Ledger Project

## Development Setup

### Prerequisites
- Java 21 JDK
- Maven 3.9+
- Docker & Docker Compose
- IDE (IntelliJ IDEA, VS Code, Eclipse)

### Local Development Environment

```bash
# 1. Clone repository
git clone <repo>
cd "Cloud-Native Banking Ledger Service"

# 2. Build all services
mvn clean package

# 3. Start infrastructure (Docker)
docker-compose up -d postgres kafka zookeeper mailhog

# 4. Start services in IDE (or via Maven)
cd eureka-server && mvn spring-boot:run
cd account-service && mvn spring-boot:run
cd transfer-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

## Project Guidelines

### Code Style
- Java 21 features encouraged
- Follow Spring Boot conventions
- Use Lombok for boilerplate reduction
- Annotate public API methods

### Service Development

#### When adding a new endpoint:
1. Create controller method
2. Add service business logic
3. Write integration test
4. Update API documentation
5. Run health checks

#### When adding a new entity:
1. Create JPA entity with @Entity
2. Add @PrePersist/@PreUpdate for timestamps
3. Create repository interface
4. Add service methods
5. Create DTOs for API

### Testing Requirements
```bash
# Unit tests required for:
- Service layer (business logic)
- Repository custom queries
- Utility methods

# Integration tests required for:
- Transaction boundaries
- API endpoints
- Message publishing

# Run tests
mvn test

# Run with coverage
mvn jacoco:report
```

## Database Schema Changes

### Adding a new table
1. Create JPA entity in appropriate service
2. Add to migration file (if using Flyway)
3. Update repository
4. Document in API docs

### Example: New Transaction Audit Table
```java
@Entity
@Table(name = "transaction_audit")
@Data
public class TransactionAudit {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(nullable = false)
    private String transactionId;
    
    @Column(nullable = false)
    private String action; // CREATED, COMPLETED, FAILED
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
```

## Kafka Topic Management

### Creating a new topic
```bash
docker exec banking-ledger-kafka kafka-topics.sh \
  --create \
  --bootstrap-server localhost:9092 \
  --topic new-topic-name \
  --partitions 3 \
  --replication-factor 1
```

### Monitoring a topic
```bash
docker exec banking-ledger-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic transfer-events \
  --from-beginning
```

## Adding a New Microservice

### Step 1: Create service module
```bash
mkdir new-service
cd new-service
```

### Step 2: Create pom.xml with dependencies

### Step 3: Create Spring Boot Application
```java
@SpringBootApplication
@EnableDiscoveryClient
public class NewServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewServiceApplication.class, args);
    }
}
```

### Step 4: Configure application.yml
```yaml
spring:
  application:
    name: new-service

server:
  port: 8084

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

### Step 5: Create Dockerfile

### Step 6: Update docker-compose.yml

### Step 7: Update parent pom.xml modules section

## Common Development Tasks

### Debug a service
```bash
# Add breakpoint in IDE
# Start with debug mode
mvn spring-boot:run -D maven.surefire.debug
```

### View database
```bash
docker exec -it banking-ledger-db psql -U postgres -d banking_ledger
```

### View Kafka messages
```bash
docker logs -f banking-ledger-kafka | grep transfer-events
```

### Restart specific service
```bash
docker-compose restart account-service
```

## Pull Request Process

1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes following guidelines
3. Add tests for new functionality
4. Update documentation
5. Run full test suite: `mvn clean test`
6. Push and create pull request

## Common Issues & Solutions

### Maven build fails
```bash
# Clear cache
mvn clean

# Update dependencies
mvn dependency:resolve

# Rebuild
mvn package
```

### Service won't start
```bash
# Check logs
docker logs <service-name>

# Check port availability
netstat -an | grep 8081

# Kill process on port
sudo lsof -ti:8081 | xargs kill -9
```

### Database connection issues
```bash
# Verify database running
docker ps | grep postgres

# Test connection
docker exec banking-ledger-db psql -U postgres -c "SELECT 1"

# Check environment variables
env | grep DATABASE
```

## Performance Tips

1. **Database**: Add indexes for frequently queried columns
2. **Caching**: Use @Cacheable for read-heavy operations
3. **Async**: Use @Async for non-blocking operations
4. **Batching**: Process multiple records efficiently
5. **Connection Pool**: Tune HikariCP settings

## Documentation Updates

When you make changes:
1. Update relevant markdown files
2. Update API documentation if endpoints change
3. Update architecture diagrams if design changes
4. Add comments for complex logic

## Release Process

1. Increment version in all pom.xml files
2. Update CHANGELOG.md
3. Create git tag
4. Build Docker images
5. Push to registry
6. Update deployment docs

---

**Thank you for contributing to Banking Ledger!**
