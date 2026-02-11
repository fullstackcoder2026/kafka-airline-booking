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
 * SOLUTION CONSUMER
 * 
 * This consumer demonstrates proper ordering when partition keys are used.
 * 
 * SOLUTION OBSERVED:
 * - Events arrive in the correct sequence
 * - CREATED → SEAT_SELECTED → PAYMENT_INITIATED → PAYMENT_COMPLETED → CONFIRMED
 * - Business logic executes correctly
 * - No validation failures
 * 
 * This works because:
 * - Producer uses bookingId as partition key
 * - All events for same booking go to same partition
 * - Kafka guarantees ordering within a partition
 * - Consumer processes events in correct order
 */
@Slf4j
@Service
public class BookingConsumerSolved {

    // Track sequence numbers to verify ordering
    private final Map<String, Integer> lastSequenceMap = new ConcurrentHashMap<>();
    private final Map<String, String> bookingStateMap = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${kafka.topic.booking.solved}",
            groupId = "booking-solved-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookingEvent(
            ConsumerRecord<String, BookingEvent> record,
            Acknowledgment acknowledgment) {
        
        BookingEvent event = record.value();
        String bookingId = event.getBookingId();
        Integer currentSeq = event.getSequenceNumber();
        
        log.info("SOLUTION CONSUMER - Received from partition {} (Key={}): {}",
                record.partition(), record.key(), event);

        // Verify ordering
        Integer lastSeq = lastSequenceMap.get(bookingId);
        
        if (lastSeq == null) {
            log.info("✅ First event for booking {}: seq {}", bookingId, currentSeq);
        } else if (currentSeq == lastSeq + 1) {
            log.info("✅ ✅ ✅ PERFECT ORDER! ✅ ✅ ✅");
            log.info("   Booking: {} | Sequence: {} → {} (consecutive)", 
                    bookingId, lastSeq, currentSeq);
        } else if (currentSeq > lastSeq) {
            log.warn("⚠️  Sequence jump - Booking: {} | {} → {}", 
                    bookingId, lastSeq, currentSeq);
        } else {
            // This should NEVER happen with proper partition keys
            log.error("❌ UNEXPECTED! Out of order with partition key! {} -> {}", 
                    lastSeq, currentSeq);
        }
        
        // Process with business logic validation
        boolean success = processBookingEventWithValidation(event);
        
        if (success) {
            lastSequenceMap.put(bookingId, currentSeq);
            log.info("   ✓ Business logic executed successfully");
        } else {
            log.error("   ✗ Business logic validation failed!");
        }
        
        // Manual acknowledgment
        acknowledgment.acknowledge();
    }

    private boolean processBookingEventWithValidation(BookingEvent event) {
        String bookingId = event.getBookingId();
        String currentState = bookingStateMap.get(bookingId);
        
        switch (event.getEventType()) {
            case "CREATED":
                if (currentState != null) {
                    log.error("   Validation Error: Booking already exists!");
                    return false;
                }
                log.info("   → Creating booking for flight {}", event.getFlightNumber());
                bookingStateMap.put(bookingId, "CREATED");
                return true;
                
            case "SEAT_SELECTED":
                if (!"CREATED".equals(currentState)) {
                    log.error("   Validation Error: Cannot select seat before creating booking!");
                    return false;
                }
                log.info("   → Selecting seat {} for booking", event.getSeatNumber());
                bookingStateMap.put(bookingId, "SEAT_SELECTED");
                return true;
                
            case "PAYMENT_INITIATED":
                if (!"SEAT_SELECTED".equals(currentState)) {
                    log.error("   Validation Error: Cannot initiate payment before seat selection!");
                    return false;
                }
                log.info("   → Initiating payment of ${}", event.getAmount());
                bookingStateMap.put(bookingId, "PAYMENT_INITIATED");
                return true;
                
            case "PAYMENT_COMPLETED":
                if (!"PAYMENT_INITIATED".equals(currentState)) {
                    log.error("   Validation Error: Cannot complete payment before initiating!");
                    return false;
                }
                log.info("   → Payment completed successfully");
                bookingStateMap.put(bookingId, "PAYMENT_COMPLETED");
                return true;
                
            case "CONFIRMED":
                if (!"PAYMENT_COMPLETED".equals(currentState)) {
                    log.error("   Validation Error: Cannot confirm before payment!");
                    return false;
                }
                log.info("   → Booking CONFIRMED! ✈️");
                bookingStateMap.put(bookingId, "CONFIRMED");
                return true;
                
            case "CANCELLED":
                log.info("   → Booking cancelled");
                bookingStateMap.put(bookingId, "CANCELLED");
                return true;
                
            default:
                log.warn("   → Unknown event type: {}", event.getEventType());
                return false;
        }
    }
}
