# Test Scenarios & Expected Results

## Test Scenario 1: Demonstrate the PROBLEM

### Objective
Show how messages without partition keys can arrive out of order, breaking business logic.

### Steps
```bash
# 1. Start the application
./start.sh

# 2. Run the problem demo
curl -X POST http://localhost:8081/api/bookings/demo-problem

# 3. Watch the logs
tail -f app.log
```

### Expected Results

#### In Producer Logs:
```
⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: CREATED
📤 Sent (NO KEY): CREATED to partition 0 offset 0

⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: SEAT_SELECTED
📤 Sent (NO KEY): SEAT_SELECTED to partition 0 offset 1

⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: PAYMENT_INITIATED
📤 Sent (NO KEY): PAYMENT_INITIATED to partition 0 offset 2

⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: PAYMENT_COMPLETED
📤 Sent (NO KEY): PAYMENT_COMPLETED to partition 0 offset 3

⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: CONFIRMED
📤 Sent (NO KEY): CONFIRMED to partition 0 offset 4
```

Note: With only 1 partition, messages might still arrive in order, but this demonstrates the risk.

#### In Consumer Logs:
```
📥 PROBLEM CONSUMER - Received from partition 0: [timestamp] Booking: BK001 | Type: CREATED | Seq: 1
✓ Sequence OK for booking BK001: null -> 1
   → Processing: Booking created for flight AA100

📥 PROBLEM CONSUMER - Received from partition 0: [timestamp] Booking: BK001 | Type: SEAT_SELECTED | Seq: 2
✓ Sequence OK for booking BK001: 1 -> 2
   → Processing: Seat 12A selected
```

**Key Observations:**
- ⚠️ Messages sent WITHOUT partition key
- Messages go to random partitions (even if same partition in this case)
- Risk of out-of-order if under load or with multiple partitions
- No guarantee of ordering

---

## Test Scenario 2: Demonstrate the SOLUTION

### Objective
Show how partition keys ensure messages arrive in correct order.

### Steps
```bash
# 1. Run the solution demo
curl -X POST http://localhost:8081/api/bookings/demo-solved

# 2. Watch the logs
tail -f app.log
```

### Expected Results

#### In Producer Logs:
```
✅ SOLUTION PRODUCER - Sending WITH partition key: CREATED [BookingID: BK002]
📤 Sent (KEY=BK002): CREATED to partition 2 offset 0

✅ SOLUTION PRODUCER - Sending WITH partition key: SEAT_SELECTED [BookingID: BK002]
📤 Sent (KEY=BK002): SEAT_SELECTED to partition 2 offset 1

✅ SOLUTION PRODUCER - Sending WITH partition key: PAYMENT_INITIATED [BookingID: BK002]
📤 Sent (KEY=BK002): PAYMENT_INITIATED to partition 2 offset 2

✅ SOLUTION PRODUCER - Sending WITH partition key: PAYMENT_COMPLETED [BookingID: BK002]
📤 Sent (KEY=BK002): PAYMENT_COMPLETED to partition 2 offset 3

✅ SOLUTION PRODUCER - Sending WITH partition key: CONFIRMED [BookingID: BK002]
📤 Sent (KEY=BK002): CONFIRMED to partition 2 offset 4
```

#### In Consumer Logs:
```
📥 SOLUTION CONSUMER - Received from partition 2 (Key=BK002): [timestamp] Booking: BK002 | Type: CREATED | Seq: 1
✅ First event for booking BK002: seq 1
   → Creating booking for flight UA200
   ✓ Business logic executed successfully

📥 SOLUTION CONSUMER - Received from partition 2 (Key=BK002): [timestamp] Booking: BK002 | Type: SEAT_SELECTED | Seq: 2
✅ ✅ ✅ PERFECT ORDER! ✅ ✅ ✅
   Booking: BK002 | Sequence: 1 → 2 (consecutive)
   → Selecting seat 12A for booking
   ✓ Business logic executed successfully

📥 SOLUTION CONSUMER - Received from partition 2 (Key=BK002): [timestamp] Booking: BK002 | Type: PAYMENT_INITIATED | Seq: 3
✅ ✅ ✅ PERFECT ORDER! ✅ ✅ ✅
   Booking: BK002 | Sequence: 2 → 3 (consecutive)
   → Initiating payment of $299.99
   ✓ Business logic executed successfully

📥 SOLUTION CONSUMER - Received from partition 2 (Key=BK002): [timestamp] Booking: BK002 | Type: PAYMENT_COMPLETED | Seq: 4
✅ ✅ ✅ PERFECT ORDER! ✅ ✅ ✅
   Booking: BK002 | Sequence: 3 → 4 (consecutive)
   → Payment completed successfully
   ✓ Business logic executed successfully

📥 SOLUTION CONSUMER - Received from partition 2 (Key=BK002): [timestamp] Booking: BK002 | Type: CONFIRMED | Seq: 5
✅ ✅ ✅ PERFECT ORDER! ✅ ✅ ✅
   Booking: BK002 | Sequence: 4 → 5 (consecutive)
   → Booking CONFIRMED! ✈️
   ✓ Business logic executed successfully
```

**Key Observations:**
- ✅ All messages sent WITH partition key (BK002)
- All messages go to SAME partition (partition 2)
- ✅ PERFECT ORDER maintained
- ✅ All business logic validations pass
- Consecutive sequence numbers (1→2→3→4→5)

---

## Test Scenario 3: Multiple Bookings in Parallel

### Objective
Show that different bookings can be processed in parallel while maintaining order within each booking.

### Steps
```bash
# 1. Run multiple bookings demo
curl -X POST http://localhost:8081/api/bookings/demo-multiple

# 2. Watch the logs
tail -f app.log
```

### Expected Results

#### In Producer Logs:
```
✅ SOLUTION PRODUCER - Sending WITH partition key: CREATED [BookingID: BK101]
📤 Sent (KEY=BK101): CREATED to partition 0 offset 0

✅ SOLUTION PRODUCER - Sending WITH partition key: CREATED [BookingID: BK102]
📤 Sent (KEY=BK102): CREATED to partition 1 offset 0

✅ SOLUTION PRODUCER - Sending WITH partition key: CREATED [BookingID: BK103]
📤 Sent (KEY=BK103): CREATED to partition 2 offset 0
```

#### Partition Distribution:
```
Partition 0: BK101 events (all in order)
Partition 1: BK102 events (all in order)
Partition 2: BK103 events (all in order)
```

**Key Observations:**
- Different bookings → Different partitions
- Each booking maintains its own order
- Parallel processing of different bookings
- Scalability achieved without losing order

---

## Test Scenario 4: Side-by-Side Comparison

### Objective
Direct comparison of problem vs solution.

### Steps
```bash
# 1. Run comparison demo
curl -X POST http://localhost:8081/api/bookings/demo-comparison

# 2. Watch the logs carefully
tail -f app.log
```

### Expected Results

You'll see both approaches back-to-back, making it easy to compare:

```
--- PROBLEM: Sending booking BK999 WITHOUT key ---
⚠️  Messages to random partitions
❌ Potential for out-of-order processing

[2 second pause]

--- SOLUTION: Sending booking BK888 WITH key ---
✅ Messages to consistent partition
✅ Guaranteed order
✅ All validations pass
```

---

## Test Scenario 5: Kafka UI Verification

### Objective
Visualize partition distribution using Kafka UI.

### Steps
```bash
# 1. Open Kafka UI
open http://localhost:8080

# 2. Run solved demo
curl -X POST http://localhost:8081/api/bookings/demo-solved

# 3. In Kafka UI:
#    - Click "Topics"
#    - Click "airline-bookings-solved"
#    - Click "Messages" tab
```

### Expected Results

In Kafka UI you should see:
- Messages tab showing all messages
- Partition column showing which partition each message went to
- Key column showing the bookingId
- All messages with same key in same partition

**Verification:**
- Filter by partition 0: Should see only one booking's events
- Filter by partition 1: Should see only another booking's events
- Filter by partition 2: Should see only third booking's events

---

## Test Scenario 6: Consumer Group Monitoring

### Objective
Monitor consumer lag and processing.

### Steps
```bash
# 1. Check consumer group status
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group booking-solved-group

# 2. Run demo
curl -X POST http://localhost:8081/api/bookings/demo-solved

# 3. Check status again
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group booking-solved-group
```

### Expected Results

```
GROUP              TOPIC                   PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
booking-solved-group airline-bookings-solved 0         5               5               0
booking-solved-group airline-bookings-solved 1         0               0               0
booking-solved-group airline-bookings-solved 2         5               5               0
```

**Key Observations:**
- LAG = 0 means all messages consumed
- CURRENT-OFFSET = LOG-END-OFFSET means caught up
- Different partitions show different activity

---

## Test Scenario 7: Simulating Heavy Load

### Objective
Show ordering maintained under load.

### Steps
```bash
# 1. Send multiple requests rapidly
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/bookings/demo-solved &
done

# 2. Watch logs
tail -f app.log | grep "PERFECT ORDER"
```

### Expected Results

Even with concurrent requests:
- Each booking still maintains order
- No sequence gaps within a booking
- All validations pass
- Messages may interleave but each booking's sequence is intact

---

## Test Scenario 8: Failure Recovery

### Objective
Show message replay on consumer restart.

### Steps
```bash
# 1. Send messages
curl -X POST http://localhost:8081/api/bookings/demo-solved

# 2. Stop the application
kill <APP_PID>

# 3. Restart the application
mvn spring-boot:run

# 4. Observe
tail -f app.log
```

### Expected Results

After restart:
- Consumer rejoins group
- Does NOT re-process messages (offset committed)
- Only new messages processed
- Order still maintained

---

## Troubleshooting Scenarios

### Scenario: Messages not appearing in consumer

**Problem:** Producer sends but consumer doesn't receive

**Diagnosis:**
```bash
# Check topics exist
docker exec -it kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list

# Check messages in topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic airline-bookings-solved \
  --from-beginning
```

**Solution:**
- Verify topic exists
- Check consumer group is running
- Verify no errors in application logs

### Scenario: Out-of-order even with keys

**Problem:** Messages still out of order despite using keys

**Diagnosis:**
- Check if partition key is actually being set
- Verify not using wrong topic (problem topic)
- Look for producer code using correct method

**Solution:**
```java
// Wrong
kafkaTemplate.send(topic, event);

// Right
kafkaTemplate.send(topic, bookingId, event);
```

### Scenario: Consumer lag increasing

**Problem:** Consumer can't keep up

**Diagnosis:**
```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group booking-solved-group
```

**Solution:**
- Increase consumer instances (up to partition count)
- Optimize consumer processing logic
- Increase partition count

---

## Performance Testing

### Throughput Test

```bash
# Send 1000 bookings as fast as possible
for i in {1..1000}; do
  curl -X POST http://localhost:8081/api/bookings/demo-solved &
done

# Monitor in Kafka UI
# Check consumer lag
# Verify all processed correctly
```

### Latency Test

```bash
# Measure end-to-end latency
# Producer timestamp → Consumer processed timestamp
# Check application logs for timing
```

---

## Success Criteria

For each test scenario, success means:

✅ **Problem Demo:**
- Shows potential for out-of-order (or explains the risk)
- Consumer logs warnings about ordering

✅ **Solution Demo:**
- All messages to same partition per booking
- Perfect consecutive sequence
- All business validations pass
- No errors or warnings

✅ **Multiple Bookings:**
- Different partitions for different bookings
- Each booking maintains order
- Parallel processing observed

✅ **Comparison:**
- Clear difference visible in logs
- Problem case shows risks
- Solution case shows fixes

---

## Next Steps After Testing

1. **Experiment with partition counts**
   - Change in docker-compose.yml
   - Observe distribution

2. **Add your own event types**
   - Extend BookingEvent
   - Add new states
   - Test ordering

3. **Implement additional consumers**
   - Create new consumer groups
   - Test parallel consumption
   - Monitor rebalancing

4. **Add monitoring**
   - Integrate with Prometheus
   - Create Grafana dashboards
   - Alert on lag

Happy Testing! 🚀
