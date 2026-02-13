# API Dokümantasyonu - Banking Ledger Services

## Base URLs

- **API Gateway**: `http://localhost:8080`
- **Account Service** (Direct): `http://localhost:8081/account-service`
- **Transfer Service** (Direct): `http://localhost:8082/transfer-service`
- **Notification Service** (Direct): `http://localhost:8083/notification-service`
- **Eureka Dashboard**: `http://localhost:8761/eureka`
- **MailHog (Email Testing)**: `http://localhost:8025`

---

## Account Service API

### Hesap Oluştur
```http
POST /accounts
Content-Type: application/json

{
  "iban": "TR330006100519786457841326",
  "accountHolder": "Ahmet Yilmaz",
  "initialBalance": 10000.00,
  "currency": "TRY"
}
```

**Response (201 Created):**
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

### Hesap Görüntüle (IBAN ile)
```http
GET /accounts/TR330006100519786457841326
```

**Response (200 OK):**
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

### Hesap Görüntüle (ID ile)
```http
GET /accounts/id/1
```

### Tüm Hesapları Listele
```http
GET /accounts
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "iban": "TR330006100519786457841326",
    "accountHolder": "Ahmet Yilmaz",
    "balance": 10000.00,
    "currency": "TRY",
    "status": "ACTIVE",
    "createdAt": "2024-02-13T10:30:00",
    "updatedAt": "2024-02-13T10:30:00"
  },
  {
    "id": 2,
    "iban": "TR440006100519786457841326",
    "accountHolder": "Fatih Kaya",
    "balance": 5000.00,
    "currency": "TRY",
    "status": "ACTIVE",
    "createdAt": "2024-02-13T10:31:00",
    "updatedAt": "2024-02-13T10:31:00"
  }
]
```

---

## Transfer Service API

### Para Transfer Yap (ATOMIK İŞLEM)
```http
POST /transfers
Content-Type: application/json

{
  "fromIban": "TR330006100519786457841326",
  "toIban": "TR440006100519786457841326",
  "amount": 500.00,
  "currency": "TRY",
  "description": "Payment for invoice #123"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "fromIban": "TR330006100519786457841326",
  "toIban": "TR440006100519786457841326",
  "amount": 500.00,
  "currency": "TRY",
  "status": "SUCCESS",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Payment for invoice #123",
  "createdAt": "2024-02-13T10:35:00",
  "updatedAt": "2024-02-13T10:35:00"
}
```

**Possible Status Values:**
- `PENDING` - Transfer bekleniyor
- `SUCCESS` - Transfer başarılı
- `FAILED` - Transfer başarısız
- `ROLLED_BACK` - Rollback gerçekleştirildi

### Transfer Detayı Görüntüle (ID ile)
```http
GET /transfers/1
```

### Transfer Detayı Görüntüle (Transaction ID ile)
```http
GET /transfers/transaction/550e8400-e29b-41d4-a716-446655440000
```

---

## Error Handling

### Yetersiz Bakiye Hatası
```json
{
  "error": "Insufficient balance. Current balance: 1000.00",
  "status": 400
}
```

### Hesap Bulunamadı
```json
{
  "error": "Account not found with IBAN: TR330006100519786457841326",
  "status": 404
}
```

### Transfer Başarısız
```json
{
  "error": "Transfer failed: Debit operation failed",
  "status": 500
}
```

---

## Asenkron Flow - Kafka Integration

### Diagram
```
[Transfer API] 
    ↓
[Transfer Service] (Thread A)
    ├─ Debit Account
    ├─ Credit Account
    ├─ Update Transfer Status
    ├─ Publish Event
    └─ Return Response (200ms)
           ↓
[Kafka Topic: transfer-events]
           ↓
[Notification Service] (Thread B - Asenkron)
    ├─ Read Event
    ├─ Send Email
    ├─ Log SMS
    └─ Complete (non-blocking)
```

---

## Örnek cURL Komutları

### 1. Hesap Oluştur
```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "iban": "TR330006100519786457841326",
    "accountHolder": "Ahmet Yilmaz",
    "initialBalance": 5000,
    "currency": "TRY"
  }' | jq
```

### 2. Başka Hesap Oluştur
```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "iban": "TR440006100519786457841326",
    "accountHolder": "Fatih Kaya",
    "initialBalance": 2000,
    "currency": "TRY"
  }' | jq
```

### 3. Para Transfer Yap
```bash
curl -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromIban": "TR330006100519786457841326",
    "toIban": "TR440006100519786457841326",
    "amount": 500,
    "currency": "TRY",
    "description": "Monthly payment"
  }' | jq
```

### 4. Hesap Bakiyelerini Kontrol Et
```bash
curl http://localhost:8080/accounts/TR330006100519786457841326 | jq '.balance'
curl http://localhost:8080/accounts/TR440006100519786457841326 | jq '.balance'
```

### 5. Transfer Geçmişini Kontrol Et
```bash
curl http://localhost:8080/transfers/1 | jq
```

### 6. Email'i Kontrol Et (MailHog)
```
Browser: http://localhost:8025
```

---

## Database Queries

### Tüm Transferleri Listele
```sql
SELECT * FROM transfers ORDER BY created_at DESC;
```

### Belirli Hesaptan Çıkan Transferler
```sql
SELECT * FROM transfers 
WHERE from_iban = 'TR330006100519786457841326'
ORDER BY created_at DESC;
```

### Başarılı Transferler Toplamı
```sql
SELECT SUM(amount) as total_transferred
FROM transfers
WHERE status = 'SUCCESS';
```

### Başarısız Transferleri Listele
```sql
SELECT * FROM transfers 
WHERE status = 'FAILED'
ORDER BY created_at DESC;
```

---

## Performance Tips

1. **Bulk Operations**: Toplu işlemler için batch processing kullanın
2. **Connection Pooling**: HikariCP otomatik konfigüre edilmiş
3. **Transaction Size**: Transfer işlemleri minimum tutulmuş
4. **Kafka Partitions**: Scalability için partition'ları artırın

---

## Security Considerations

- [ ] Authentication (JWT) eklenecek
- [ ] Rate limiting eklenmesi gerekli
- [ ] HTTPS/TLS konfigürasyonu
- [ ] IBAN validation
- [ ] PCI DSS compliance

---

## Versioning

- **API Version**: v1
- **Java Version**: 21
- **Spring Boot**: 3.2.0
- **Kafka**: 7.5.0
- **PostgreSQL**: 16

---

## Support

Problemler veya sorular için lütfen logs'u kontrol edin:

```bash
docker logs -f banking-ledger-transfer-service
docker logs -f banking-ledger-notification-service
```
