# Architecture Documentation

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Spring Boot Application                            │
│                         (Port 8081)                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │                    REST Controller Layer                           │    │
│  │  BookingController                                                 │    │
│  │  ├─ POST /api/bookings/demo-problem                                │    │
│  │  ├─ POST /api/bookings/demo-solved                                 │    │
│  │  ├─ POST /api/bookings/demo-multiple                               │    │
│  │  └─ POST /api/bookings/demo-comparison                             │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│           │                                       │                          │
│           ▼                                       ▼                          │
│  ┌──────────────────────┐           ┌──────────────────────────┐           │
│  │ BookingProducerProblem│          │ BookingProducerSolved    │           │
│  ├──────────────────────┤           ├──────────────────────────┤           │
│  │ - send(event)        │           │ - send(bookingId, event) │           │
│  │ - NO partition key   │           │ - WITH partition key     │           │
│  │ - Random distribution│           │ - Consistent hashing     │           │
│  └──────────┬───────────┘           └───────────┬──────────────┘           │
│             │                                    │                          │
└─────────────┼────────────────────────────────────┼──────────────────────────┘
              │                                    │
              │ KafkaTemplate                      │ KafkaTemplate
              ▼                                    ▼
     ╔════════════════════╗              ╔════════════════════════╗
     ║ Kafka Cluster      ║              ║ Kafka Cluster          ║
     ║ (localhost:9092)   ║              ║ (localhost:9092)       ║
     ╠════════════════════╣              ╠════════════════════════╣
     ║                    ║              ║                        ║
     ║ Topic:             ║              ║ Topic:                 ║
     ║ airline-bookings-  ║              ║ airline-bookings-      ║
     ║ problem            ║              ║ solved                 ║
     ║                    ║              ║                        ║
     ║ ┌────────────────┐ ║              ║ ┌────────────────────┐║
     ║ │ Partition 0    │ ║              ║ │ Partition 0        │║
     ║ │ msg1, msg4     │ ║              ║ │ BK003 events       │║
     ║ │ (random)       │ ║              ║ │ (ordered)          │║
     ║ └────────────────┘ ║              ║ └────────────────────┘║
     ║                    ║              ║ ┌────────────────────┐║
     ║                    ║              ║ │ Partition 1        │║
     ║ Messages scattered ║              ║ │ BK001 events       │║
     ║ across partitions  ║              ║ │ (ordered)          │║
     ║ without logic      ║              ║ └────────────────────┘║
     ║                    ║              ║ ┌────────────────────┐║
     ║ ⚠️  NO ORDERING     ║              ║ │ Partition 2        │║
     ║ GUARANTEE          ║              ║ │ BK002 events       │║
     ║                    ║              ║ │ (ordered)          │║
     ║                    ║              ║ └────────────────────┘║
     ║                    ║              ║                        ║
     ║                    ║              ║ ✅ ORDERING GUARANTEED ║
     ║                    ║              ║ PER BOOKING ID         ║
     ╚═════════┬══════════╝              ╚════════┬═══════════════╝
               │                                  │
               │ @KafkaListener                   │ @KafkaListener
               ▼                                  ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          Spring Boot Application                             │
│                         (Consumer Side)                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────┐         ┌───────────────────────────┐         │
│  │ BookingConsumerProblem   │         │ BookingConsumerSolved     │         │
│  ├──────────────────────────┤         ├───────────────────────────┤         │
│  │ - Receives out of order  │         │ - Receives in order       │         │
│  │ - Detects sequence gaps  │         │ - Validates sequence      │         │
│  │ - Shows validation errors│         │ - Business logic passes   │         │
│  │                          │         │                           │         │
│  │ ❌ PAYMENT before CREATE  │         │ ✅ CREATE → SEAT → PAY    │         │
│  │ ❌ CONFIRM before PAY     │         │ ✅ PAY → CONFIRM          │         │
│  └──────────────────────────┘         └───────────────────────────┘         │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Message Flow Diagrams

### PROBLEM Flow (Without Partition Key)

```
Time →

Producer                     Kafka (3 Partitions)              Consumer
   │                                                              │
   │  1. CREATED (BK001)                                         │
   ├────────────────────────► Partition 0: [CREATED]            │
   │                                                              │
   │  2. SEAT_SELECTED (BK001)                                   │
   ├────────────────────────► Partition 2: [SEAT_SELECTED] ─────┼──► ❌ Received BEFORE needed!
   │                                                              │
   │  3. PAYMENT_INITIATED (BK001)                               │
   ├────────────────────────► Partition 1: [PAYMENT_INITIATED]  │
   │                                                              │
   │  4. PAYMENT_COMPLETED (BK001)                               │
   ├────────────────────────► Partition 0: [PAYMENT_COMPLETED]  │
   │                                                              │
   │  5. CONFIRMED (BK001)                                       │
   ├────────────────────────► Partition 2: [CONFIRMED] ─────────┼──► ❌ Payment not done yet!
   │                                                              │
   │                         Different partitions consumed at     │
   │                         different rates = OUT OF ORDER!      │
```

**Result:** Events arrive as: SEAT_SELECTED → CONFIRMED → CREATED → PAYMENT_INITIATED → PAYMENT_COMPLETED ❌

---

### SOLUTION Flow (With Partition Key)

```
Time →

Producer                     Kafka (3 Partitions)              Consumer
   │                                                              │
   │  key=BK001, 1. CREATED                                      │
   ├────────────────────────► Partition 1: [CREATED] ───────────┼──► ✅ First event
   │                                                              │
   │  key=BK001, 2. SEAT_SELECTED                                │
   ├────────────────────────► Partition 1: [SEAT_SELECTED] ─────┼──► ✅ In order!
   │                                                              │
   │  key=BK001, 3. PAYMENT_INITIATED                            │
   ├────────────────────────► Partition 1: [PAYMENT_INITIATED] ─┼──► ✅ In order!
   │                                                              │
   │  key=BK001, 4. PAYMENT_COMPLETED                            │
   ├────────────────────────► Partition 1: [PAYMENT_COMPLETED] ─┼──► ✅ In order!
   │                                                              │
   │  key=BK001, 5. CONFIRMED                                    │
   ├────────────────────────► Partition 1: [CONFIRMED] ─────────┼──► ✅ Perfect!
   │                                                              │
   │                         All to Partition 1 = ORDERED!        │
   │                         hash(BK001) % 3 = 1                  │
```

**Result:** Events arrive as: CREATED → SEAT_SELECTED → PAYMENT_INITIATED → PAYMENT_COMPLETED → CONFIRMED ✅

---

## Partition Assignment Logic

### Hash-Based Partitioning

```
Partition = hash(partitionKey) % numberOfPartitions

Examples with 3 partitions:

hash("BK001") = 7846353  →  7846353 % 3 = 1  →  Partition 1
hash("BK002") = 9234782  →  9234782 % 3 = 2  →  Partition 2
hash("BK003") = 4521098  →  4521098 % 3 = 0  →  Partition 0
hash("BK001") = 7846353  →  7846353 % 3 = 1  →  Partition 1 (same!)

Key Insight: Same key ALWAYS goes to same partition!
```

### Distribution Example

```
3 Bookings with partition keys:

BK001: hash % 3 = 1  →  All events to Partition 1
BK002: hash % 3 = 2  →  All events to Partition 2
BK003: hash % 3 = 0  →  All events to Partition 3

Partition 0: [BK003-CREATE, BK003-SEAT, BK003-PAY, BK003-CONFIRM]
Partition 1: [BK001-CREATE, BK001-SEAT, BK001-PAY, BK001-CONFIRM]
Partition 2: [BK002-CREATE, BK002-SEAT, BK002-PAY, BK002-CONFIRM]

Benefits:
✅ Each booking processed in order
✅ Different bookings processed in parallel
✅ No blocking between different bookings
✅ Scalable and fast
```

---

## State Machine Validation

### Booking State Transitions

```
         START
           ↓
      ┌─────────┐
      │ CREATED │ ← Initial state
      └────┬────┘
           │
           ▼
   ┌──────────────┐
   │SEAT_SELECTED │ ← Must come after CREATED
   └──────┬───────┘
          │
          ▼
  ┌─────────────────┐
  │PAYMENT_INITIATED│ ← Must come after SEAT_SELECTED
  └────────┬────────┘
           │
           ▼
  ┌─────────────────┐
  │PAYMENT_COMPLETED│ ← Must come after PAYMENT_INITIATED
  └────────┬────────┘
           │
           ▼
      ┌──────────┐
      │CONFIRMED │ ← Final state
      └──────────┘
```

### Invalid Transitions (PROBLEM scenario)

```
❌ PAYMENT_COMPLETED → CREATED
   (Can't complete payment for non-existent booking)

❌ CONFIRMED → SEAT_SELECTED
   (Can't select seat after confirmation)

❌ PAYMENT_INITIATED → CREATED
   (Can't pay before creating booking)
```

### Valid Transitions (SOLUTION scenario)

```
✅ CREATED → SEAT_SELECTED
   (Booking exists, can select seat)

✅ SEAT_SELECTED → PAYMENT_INITIATED
   (Seat chosen, can start payment)

✅ PAYMENT_COMPLETED → CONFIRMED
   (Payment done, can confirm booking)
```

---

## Performance Characteristics

### Without Partition Keys (PROBLEM)

| Metric | Value | Notes |
|--------|-------|-------|
| Ordering Guarantee | ❌ None | Messages scattered |
| Throughput | High | All partitions utilized |
| Latency | Low | Parallel processing |
| Data Integrity | ❌ Poor | Out-of-order issues |
| Use Case | Logs, Metrics | Independent events only |

### With Partition Keys (SOLUTION)

| Metric | Value | Notes |
|--------|-------|-------|
| Ordering Guarantee | ✅ Per Key | Same key = ordered |
| Throughput | High | Different keys parallel |
| Latency | Low | Parallel per entity |
| Data Integrity | ✅ Excellent | Correct sequence |
| Use Case | Transactions | State-dependent events |

---

## Consumer Group Behavior

### Single Consumer

```
Topic: airline-bookings-solved (3 partitions)
Consumer Group: booking-group (1 consumer)

Consumer 1: [P0, P1, P2] ← Handles all partitions
```

### Multiple Consumers

```
Topic: airline-bookings-solved (3 partitions)
Consumer Group: booking-group (3 consumers)

Consumer 1: [P0] ← Handles partition 0
Consumer 2: [P1] ← Handles partition 1
Consumer 3: [P2] ← Handles partition 2

Benefits:
✅ Parallel processing
✅ Each booking still processed in order
✅ Scalable to number of partitions
```

### More Consumers Than Partitions

```
Topic: airline-bookings-solved (3 partitions)
Consumer Group: booking-group (5 consumers)

Consumer 1: [P0]
Consumer 2: [P1]
Consumer 3: [P2]
Consumer 4: []  ← IDLE (no partitions)
Consumer 5: []  ← IDLE (no partitions)

Note: Extra consumers sit idle
Max parallelism = number of partitions
```

---

## Configuration Details

### Producer Configuration

```properties
# Acknowledgment
spring.kafka.producer.acks=all  # Wait for all replicas

# Retries
spring.kafka.producer.retries=3  # Retry up to 3 times

# Serialization
spring.kafka.producer.key-serializer=StringSerializer
spring.kafka.producer.value-serializer=JsonSerializer
```

### Consumer Configuration

```properties
# Offset management
spring.kafka.consumer.auto-offset-reset=earliest  # Read from beginning

# Commit
spring.kafka.consumer.enable-auto-commit=false  # Manual commit
spring.kafka.listener.ack-mode=manual  # Control acknowledgment

# Deserialization
spring.kafka.consumer.key-deserializer=StringDeserializer
spring.kafka.consumer.value-deserializer=JsonDeserializer
```

### Topic Configuration

```java
// Problem topic - demonstrates issue
new NewTopic("airline-bookings-problem", 1, (short) 1)
  - 1 partition (but still has ordering issues without keys)
  - Replication factor: 1

// Solution topic - demonstrates fix
new NewTopic("airline-bookings-solved", 3, (short) 1)
  - 3 partitions (for parallel processing)
  - Replication factor: 1
```

---

## Error Handling

### Producer Side

```java
future.whenComplete((result, ex) -> {
    if (ex == null) {
        // Success - log partition and offset
        log.info("Sent to partition {} offset {}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    } else {
        // Failure - log error
        log.error("Failed to send event", ex);
        // Could implement retry logic here
    }
});
```

### Consumer Side

```java
try {
    // Process message
    processEvent(event);
    
    // Only acknowledge if successful
    acknowledgment.acknowledge();
    
} catch (Exception ex) {
    log.error("Processing failed", ex);
    // Don't acknowledge - message will be redelivered
    // Could implement dead letter queue here
}
```

---

## Monitoring Points

### Key Metrics to Monitor

1. **Producer Metrics**
   - Messages sent per second
   - Partition distribution
   - Send failures
   - Latency

2. **Consumer Metrics**
   - Messages consumed per second
   - Lag (difference between produced and consumed)
   - Processing time
   - Error rate

3. **Kafka Metrics**
   - Partition count
   - Replication status
   - Disk usage
   - Network throughput

### Using Kafka UI

Access http://localhost:8080 to view:
- Topic list and configuration
- Message count per partition
- Consumer group status
- Lag monitoring
- Message browsing

---

## Best Practices Applied

✅ **Partition Key Strategy**
- Use business identifier (bookingId)
- Ensures consistent routing
- Maintains order per entity

✅ **Manual Acknowledgment**
- Control when messages are marked as consumed
- Allows retry on failure
- Prevents data loss

✅ **Idempotent Processing**
- Handle duplicate messages gracefully
- Use sequence numbers for validation
- Implement deduplication if needed

✅ **Error Handling**
- Comprehensive try-catch blocks
- Detailed logging
- Graceful degradation

✅ **Monitoring**
- Log partition assignments
- Track sequence numbers
- Alert on out-of-order messages

---

## Scaling Considerations

### Horizontal Scaling

```
Add more partitions:
- Increase parallelism
- Balance load
- Maintain order per key

Add more consumers:
- Up to number of partitions
- Each partition to one consumer
- Automatic rebalancing
```

### Vertical Scaling

```
Increase resources per consumer:
- More CPU for faster processing
- More memory for buffering
- Better I/O for persistence
```

---

This architecture provides a robust, scalable solution for ordered message processing in Kafka!
