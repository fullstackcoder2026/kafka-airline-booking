package com.example.kafka.consumer;

import com.example.kafka.model.BookingEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PROBLEM CONSUMER
 * 
 * This consumer demonstrates what happens when ordering is NOT maintained.
 * 
 * PROBLEM OBSERVED:
 * - Events arrive out of sequence
 * - PAYMENT_COMPLETED might arrive before CREATED
 * - CONFIRMED might arrive before SEAT_SELECTED
 * - Business logic validation fails
 * 
 * This happens because:
 * - Producer doesn't use partition keys
 * - Events go to random partitions
 * - Different partitions are consumed at different rates
 */
@Slf4j
@Service
public class BookingConsumerProblem {

    // Track last sequence number per booking to detect out-of-order messages
    private final Map<String, Integer> lastSequenceMap = new ConcurrentHashMap<>();
    private final Map<String, String> lastEventMap = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${kafka.topic.booking.problem}",
            groupId = "booking-problem-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookingEvent(
            ConsumerRecord<String, BookingEvent> record,
            Acknowledgment acknowledgment) {
        
        BookingEvent event = record.value();
        String bookingId = event.getBookingId();
        Integer currentSeq = event.getSequenceNumber();
        
        log.info("PROBLEM CONSUMER - Received from partition {}: {}",
                record.partition(), event);

        // Check for ordering issues
        Integer lastSeq = lastSequenceMap.get(bookingId);
        String lastEvent = lastEventMap.get(bookingId);
        
        if (lastSeq != null && currentSeq <= lastSeq) {
            log.error("❌ ❌ ❌ OUT OF ORDER DETECTED! ❌ ❌ ❌");
            log.error("   Booking: {} | Expected seq > {}, but got seq {}", 
                    bookingId, lastSeq, currentSeq);
            log.error("   Last event: {} | Current event: {}", 
                    lastEvent, event.getEventType());
            log.error("   This breaks business logic! Payment before booking creation?");
        } else if (lastSeq != null && currentSeq != lastSeq + 1) {
            log.warn("⚠️  SEQUENCE GAP - Booking: {} | Last: {} | Current: {} | Gap: {}", 
                    bookingId, lastSeq, currentSeq, currentSeq - lastSeq - 1);
        } else {
            log.info("✓ Sequence OK for booking {}: {} -> {}", 
                    bookingId, lastSeq, currentSeq);
        }
        
        // Process the event (business logic would go here)
        processBookingEvent(event);
        
        // Update tracking
        lastSequenceMap.put(bookingId, currentSeq);
        lastEventMap.put(bookingId, event.getEventType());
        
        // Manual acknowledgment
        acknowledgment.acknowledge();
    }

    private void processBookingEvent(BookingEvent event) {
        switch (event.getEventType()) {
            case "CREATED":
                log.info("   → Processing: Booking created for flight {}", event.getFlightNumber());
                break;
            case "SEAT_SELECTED":
                log.info("   → Processing: Seat {} selected", event.getSeatNumber());
                break;
            case "PAYMENT_INITIATED":
                log.info("   → Processing: Payment initiated for ${}", event.getAmount());
                break;
            case "PAYMENT_COMPLETED":
                log.info("   → Processing: Payment completed");
                break;
            case "CONFIRMED":
                log.info("   → Processing: Booking confirmed!");
                break;
            case "CANCELLED":
                log.info("   → Processing: Booking cancelled");
                break;
            default:
                log.warn("   → Unknown event type: {}", event.getEventType());
        }
    }
}
