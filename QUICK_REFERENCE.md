# Quick Reference Guide - Kafka Ordering Problem Demo

## 🚀 Quick Start (3 Steps)

1. **Start everything:**
   ```bash
   ./start.sh
   ```

2. **Run a demo:**
   ```bash
   # See the problem
   curl -X POST http://localhost:8081/api/bookings/demo-problem
   
   # See the solution
   curl -X POST http://localhost:8081/api/bookings/demo-solved
   ```

3. **Stop everything:**
   ```bash
   ./stop.sh
   ```

---

## 📊 What You'll See

### PROBLEM Demo (❌ Out of Order)

```
Producer sends events WITHOUT key:
  ┌─────────────┐
  │ CREATED     │ → Partition 0
  │ SEAT_SELECTED│ → Partition 1  ⚠️ Different partition!
  │ PAYMENT     │ → Partition 2  ⚠️ Different partition!
  │ CONFIRMED   │ → Partition 0
  └─────────────┘

Consumer receives:
  PAYMENT → ❌ Error! No booking exists!
  CREATED → OK
  CONFIRMED → ❌ Error! Payment not completed!
  SEAT_SELECTED → ❌ Error! Wrong order!
```

### SOLUTION Demo (✅ Ordered)

```
Producer sends events WITH key (bookingId):
  ┌─────────────┐
  │ CREATED     │ → Partition 1 (hash(BK001) = 1)
  │ SEAT_SELECTED│ → Partition 1 ✓ Same partition!
  │ PAYMENT     │ → Partition 1 ✓ Same partition!
  │ CONFIRMED   │ → Partition 1 ✓ Same partition!
  └─────────────┘

Consumer receives IN ORDER:
  CREATED       → ✓ Booking created
  SEAT_SELECTED → ✓ Seat assigned
  PAYMENT       → ✓ Payment processed
  CONFIRMED     → ✓ Booking confirmed!
```

---

## 🎯 Key Code Differences

### Problem Producer (Wrong ❌)

```java
// NO partition key - random distribution
kafkaTemplate.send(topic, event);
```

### Solution Producer (Correct ✅)

```java
// WITH partition key - ordered processing
kafkaTemplate.send(topic, bookingId, event);
```

---

## 📍 URLs

- **Application API**: http://localhost:8081
- **Kafka UI**: http://localhost:8080
- **Application Logs**: `tail -f app.log`

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 9092 in use | `docker-compose down && docker-compose up -d` |
| App won't start | Check Java version: `java -version` (need 17+) |
| No messages in consumer | Reset offset (see README) |
| Kafka not ready | Wait 30 seconds after `docker-compose up` |

---

## 📚 Core Concepts

### Partition Key
- Determines which partition receives the message
- Calculated: `hash(key) % number_of_partitions`
- Same key → same partition → guaranteed order

### Why Order Matters
- State changes must be sequential
- Payment before booking = FAILURE
- Seat selection before creation = FAILURE
- Confirmation before payment = FAILURE

### The Fix
Always use a consistent business identifier as partition key:
- Booking ID for bookings
- Order ID for orders
- User ID for user events
- Transaction ID for transactions

---

## 🎓 Learning Points

1. **Kafka guarantees order ONLY within a partition**
2. **Partition keys ensure same entity → same partition**
3. **Business logic requiring order NEEDS partition keys**
4. **Choose partition keys based on your business domain**

---

## 📞 Common Questions

**Q: Why not just use 1 partition?**
A: Scalability! 1 partition = no parallelism = bottleneck

**Q: What if I need global ordering?**
A: Use 1 partition OR implement sequence numbers with reordering buffer

**Q: How many partitions should I use?**
A: Start with: `num_consumers * 2` to `num_consumers * 3`

**Q: Can different bookings be parallel?**
A: Yes! BK001 and BK002 can be on different partitions and process in parallel

---

## 🎬 Demo Scenarios

| Endpoint | What It Shows |
|----------|---------------|
| `/demo-problem` | Out-of-order messages, validation failures |
| `/demo-solved` | Correct ordering, successful processing |
| `/demo-multiple` | Multiple bookings on different partitions |
| `/demo-comparison` | Side-by-side problem vs solution |

---

## 💡 Best Practices

✅ **DO:**
- Use partition keys for related events
- Choose keys with good distribution
- Monitor partition balance
- Design for idempotency

❌ **DON'T:**
- Forget partition keys on ordered data
- Use null/empty keys
- Create hot partitions
- Assume global ordering without keys

---

## 📖 Next Steps

1. Run all 4 demo scenarios
2. Check Kafka UI to see partition distribution
3. Read producer/consumer logs carefully
4. Experiment with different booking IDs
5. Try adding more partitions

Happy Learning! 🚀
