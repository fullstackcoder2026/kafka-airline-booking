# Kafka Ordering Problem Demo - Airline Ticket Booking ✈️

A comprehensive Spring Boot application demonstrating **Kafka message ordering problems** and their solutions using airline ticket booking as a real-world example.

---

## 📋 Table of Contents

- [Problem Overview](#problem-overview)
- [Solution Overview](#solution-overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Running the Demo](#running-the-demo)
- [Understanding the Results](#understanding-the-results)
- [Technical Details](#technical-details)
- [Troubleshooting](#troubleshooting)

---

## 🚨 Problem Overview

### The Ordering Problem

In Kafka, when messages are sent **without a partition key**:

1. **Random Distribution**: Messages are distributed using round-robin or random strategy across partitions
2. **Independent Consumption**: Different partitions are consumed independently
3. **No Order Guarantee**: Events for the same entity can arrive out of order
4. **Business Logic Failure**: Dependent operations fail validation

### Real-World Impact: Airline Booking

Imagine booking a flight with these events:

```
1. CREATED          → Booking initialized
2. SEAT_SELECTED    → Customer picks seat 12A
3. PAYMENT_INITIATED → Payment of $299.99 started
4. PAYMENT_COMPLETED → Payment confirmed
5. CONFIRMED        → Booking confirmed
```

**Without proper ordering:**
- Event #4 (PAYMENT_COMPLETED) might arrive before Event #1 (CREATED)
- System tries to confirm payment for non-existent booking → **FAILURE**
- Customer charged but no booking exists → **CRITICAL BUG**

---

## ✅ Solution Overview

### Using Partition Keys

When messages are sent **with a partition key** (e.g., `bookingId`):

1. **Consistent Hashing**: `hash(key) % num_partitions` determines partition
2. **Same Key → Same Partition**: All events for `BK001` go to partition 2
3. **Ordered Processing**: Messages in a partition are strictly ordered
4. **Guaranteed Sequence**: Events processed in correct order

### How It Solves the Problem

```java
// ❌ PROBLEM: No partition key
kafkaTemplate.send(topic, event);  // Random partition

// ✅ SOLUTION: With partition key
kafkaTemplate.send(topic, bookingId, event);  // Same partition for same bookingId
```

**Result:**
- All events for booking `BK001` go to partition 2
- Consumer processes: CREATED → SEAT → PAYMENT → CONFIRMED
- Business logic validations pass ✓
- Customer gets confirmed booking ✓

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  REST Controllers                                                │
│  ┌──────────────┐                                               │
│  │   POST /api  │ ─────┐                                        │
│  └──────────────┘      │                                        │
│                        │                                        │
│  Producers             ▼                                        │
│  ┌─────────────────────────────┐  ┌──────────────────────────┐ │
│  │  BookingProducerProblem     │  │  BookingProducerSolved   │ │
│  │  (No partition key)         │  │  (With partition key)    │ │
│  └────────────┬────────────────┘  └───────────┬──────────────┘ │
└───────────────┼───────────────────────────────┼────────────────┘
                │                               │
                ▼                               ▼
        ┌───────────────┐           ┌───────────────────┐
        │ Kafka Topic   │           │  Kafka Topic      │
        │ (problem)     │           │  (solved)         │
        │               │           │                   │
        │ Partition 0   │           │ Partition 0 ─────┼─── BK002
        │               │           │ Partition 1 ─────┼─── BK001
        │ Messages      │           │ Partition 2 ─────┼─── BK003
        │ randomly      │           │                   │
        │ distributed   │           │ Same bookingId    │
        │               │           │ → Same partition  │
        └───────┬───────┘           └────────┬──────────┘
                │                            │
                ▼                            ▼
┌───────────────┴───────────────┬───────────┴────────────────────┐
│  BookingConsumerProblem       │  BookingConsumerSolved         │
│  - Detects out-of-order msgs  │  - Validates correct sequence  │
│  - Shows validation failures  │  - Processes in order          │
└───────────────────────────────┴────────────────────────────────┘
```

---

## 📦 Prerequisites

### Required Software

1. **Java 17+**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **Docker & Docker Compose**
   ```bash
   docker --version
   docker-compose --version
   ```

---

## 🚀 Quick Start

### Step 1: Start Kafka Infrastructure

```bash
# Start Kafka, Zookeeper, and Kafka UI
docker-compose up -d

# Verify containers are running
docker ps

# You should see:
# - zookeeper (port 2181)
# - kafka (port 9092)
# - kafka-ui (port 8080)
```

**Kafka UI**: Open http://localhost:8080 in your browser

### Step 2: Build the Application

```bash
# Install dependencies and build
mvn clean install

# Or skip tests for faster build
mvn clean install -DskipTests
```

### Step 3: Run the Application

```bash
# Run Spring Boot application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/kafka-airline-booking-1.0.0.jar
```

**Application**: http://localhost:8081

---

## 🎯 Running the Demo

### Option 1: Using cURL

#### Demo the PROBLEM (Out-of-order messages)

```bash
curl -X POST http://localhost:8081/api/bookings/demo-problem
```

**What happens:**
- Events sent WITHOUT partition key
- Messages go to random partitions
- Consumer receives events OUT OF ORDER
- Validation errors appear in logs

#### Demo the SOLUTION (Ordered messages)

```bash
curl -X POST http://localhost:8081/api/bookings/demo-solved
```

**What happens:**
- Events sent WITH partition key (`bookingId`)
- All messages for same booking → same partition
- Consumer receives events IN CORRECT ORDER
- All validations pass

#### Demo Multiple Bookings

```bash
curl -X POST http://localhost:8081/api/bookings/demo-multiple
```

**What happens:**
- Creates 3 different bookings
- Each booking's events go to consistent partition
- Shows how different bookings can be on different partitions
- All bookings processed correctly in order

#### Side-by-Side Comparison

```bash
curl -X POST http://localhost:8081/api/bookings/demo-comparison
```

**What happens:**
- Runs problem demo first
- Then runs solution demo
- Easy to compare the difference in logs

### Option 2: Using Postman/Browser

1. Import the following endpoints into Postman:
   - `POST http://localhost:8081/api/bookings/demo-problem`
   - `POST http://localhost:8081/api/bookings/demo-solved`
   - `POST http://localhost:8081/api/bookings/demo-multiple`
   - `POST http://localhost:8081/api/bookings/demo-comparison`

2. Click "Send" on any endpoint

3. Check application logs in terminal

---

## 📊 Understanding the Results

### PROBLEM Demo Output

```log
⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: CREATED
📤 Sent (NO KEY): CREATED to partition 0 offset 1

⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: PAYMENT_COMPLETED
📤 Sent (NO KEY): PAYMENT_COMPLETED to partition 1 offset 2

📥 PROBLEM CONSUMER - Received from partition 1: PAYMENT_COMPLETED
❌ ❌ ❌ OUT OF ORDER DETECTED! ❌ ❌ ❌
   Booking: BK001 | Expected seq > 1, but got seq 4
   Last event: CREATED | Current event: PAYMENT_COMPLETED
   This breaks business logic! Payment before booking creation?
```

**Key Indicators:**
- ⚠️ `WITHOUT partition key`
- Different partitions (0, 1, 2)
- ❌ `OUT OF ORDER DETECTED`
- Sequence jumps (1 → 4)

### SOLUTION Demo Output

```log
✅ SOLUTION PRODUCER - Sending WITH partition key: CREATED [BookingID: BK002]
📤 Sent (KEY=BK002): CREATED to partition 2 offset 1

✅ SOLUTION PRODUCER - Sending WITH partition key: SEAT_SELECTED [BookingID: BK002]
📤 Sent (KEY=BK002): SEAT_SELECTED to partition 2 offset 2

📥 SOLUTION CONSUMER - Received from partition 2 (Key=BK002): CREATED
✅ First event for booking BK002: seq 1

📥 SOLUTION CONSUMER - Received from partition 2 (Key=BK002): SEAT_SELECTED
✅ ✅ ✅ PERFECT ORDER! ✅ ✅ ✅
   Booking: BK002 | Sequence: 1 → 2 (consecutive)
   → Selecting seat 12A for booking
   ✓ Business logic executed successfully
```

**Key Indicators:**
- ✅ `WITH partition key`
- Same partition for all events (partition 2)
- ✅ `PERFECT ORDER`
- Consecutive sequences (1 → 2 → 3 → 4 → 5)
- ✓ `Business logic executed successfully`

---

## 🔧 Technical Details

### Topics Configuration

#### Problem Topic
```properties
Topic: airline-bookings-problem
Partitions: 1
Replication Factor: 1
Purpose: Demonstrate ordering issues
```

#### Solution Topic
```properties
Topic: airline-bookings-solved
Partitions: 3
Replication Factor: 1
Purpose: Demonstrate proper ordering
```

### Message Flow

#### Booking Event Structure
```json
{
  "bookingId": "BK001",
  "customerId": "CUST001",
  "flightNumber": "AA100",
  "eventType": "CREATED",
  "seatNumber": null,
  "amount": null,
  "timestamp": "2026-02-10T12:30:00",
  "sequenceNumber": 1
}
```

#### Event Sequence
1. **CREATED** (seq: 1) - Booking initiated
2. **SEAT_SELECTED** (seq: 2) - Seat chosen
3. **PAYMENT_INITIATED** (seq: 3) - Payment started
4. **PAYMENT_COMPLETED** (seq: 4) - Payment confirmed
5. **CONFIRMED** (seq: 5) - Booking confirmed

### Partition Key Strategy

```java
// Partition calculation
int partition = hash(bookingId) % numPartitions

// Examples:
hash("BK001") % 3 = 1  → Partition 1
hash("BK002") % 3 = 2  → Partition 2
hash("BK003") % 3 = 0  → Partition 0
```

All events with `bookingId = "BK001"` always go to Partition 1.

---

## 🔍 Monitoring

### Kafka UI

Access: http://localhost:8080

**Features:**
- View topics and partitions
- See messages in real-time
- Monitor consumer groups
- Check partition distribution

**What to look for:**
1. Navigate to Topics
2. Select `airline-bookings-problem` or `airline-bookings-solved`
3. Click "Messages" tab
4. Observe partition distribution
5. Check message keys

### Application Logs

The application uses colored, structured logging:

- 🔴 **Red/Warning**: Problem producer/consumer
- 🟢 **Green/Info**: Solution producer/consumer
- ❌ **Error**: Validation failures, out-of-order
- ✅ **Success**: Correct ordering, validations passed

---

## 🛠 Troubleshooting

### Kafka Not Starting

```bash
# Check if ports are already in use
lsof -i :9092
lsof -i :2181

# Kill processes using those ports
kill -9 <PID>

# Restart Kafka
docker-compose down
docker-compose up -d
```

### Application Can't Connect to Kafka

```bash
# Verify Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka

# Verify connectivity
telnet localhost 9092
```

### No Messages in Consumer

```bash
# Check consumer group status
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group booking-problem-group

# Reset consumer offset
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group booking-problem-group \
  --topic airline-bookings-problem \
  --reset-offsets --to-earliest --execute
```

### Build Failures

```bash
# Clean and rebuild
mvn clean install -U

# Skip tests
mvn clean install -DskipTests

# Use Java 17
export JAVA_HOME=/path/to/java-17
mvn clean install
```

---

## 📚 Key Learnings

### When to Use Partition Keys

✅ **Use partition keys when:**
- Events for same entity must be ordered
- State changes must be sequential
- Business logic depends on event order
- Example: Financial transactions, order processing, booking workflows

❌ **Don't need partition keys when:**
- Events are independent
- No ordering required
- Maximizing throughput is priority
- Example: Log aggregation, metrics collection

### Best Practices

1. **Always use partition keys for related events**
   ```java
   kafkaTemplate.send(topic, entityId, event);
   ```

2. **Choose good partition keys**
   - Use natural business identifiers (orderId, userId, bookingId)
   - Ensure even distribution across partitions
   - Avoid null or empty keys

3. **Monitor partition distribution**
   - Check for hot partitions
   - Ensure balanced load
   - Use metrics and Kafka UI

4. **Design for idempotency**
   - Handle duplicate messages
   - Use unique identifiers
   - Implement proper error handling

---

## 🎓 Further Exploration

### Experiment Ideas

1. **Increase partition count**
   - Modify `docker-compose.yml`
   - Set `KAFKA_NUM_PARTITIONS: 10`
   - Observe distribution

2. **Add more bookings**
   - Call `/demo-multiple` endpoint
   - Watch different bookings go to different partitions
   - All maintain order within their partition

3. **Test consumer groups**
   - Start multiple application instances
   - See partitions distributed across consumers
   - Observe parallel processing

4. **Simulate failures**
   - Stop consumer mid-processing
   - Restart and see offset management
   - Verify no message loss

---

## 📖 References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [Kafka Partitioning Strategy](https://kafka.apache.org/documentation/#design_partitioning)

---

## 🤝 Contributing

Feel free to:
- Report issues
- Suggest improvements
- Add more demo scenarios
- Improve documentation

---

## 📄 License

MIT License - Feel free to use this for learning and demonstrations!

---

## ✨ Summary

This demo clearly shows:

1. **THE PROBLEM**: Without partition keys → random distribution → out-of-order processing → business logic failures
2. **THE SOLUTION**: With partition keys → consistent routing → ordered processing → reliable business logic

**Key Takeaway**: In event-driven systems where order matters, always use partition keys based on your business entity identifier!

Happy Learning! 🚀✈️
