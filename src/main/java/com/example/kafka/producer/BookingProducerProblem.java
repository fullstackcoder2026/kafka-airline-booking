package com.example.kafka.producer;

import com.example.kafka.model.BookingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * PROBLEM PRODUCER
 * 
 * This producer demonstrates the ordering problem in Kafka.
 * 
 * ISSUE: Messages are sent WITHOUT a partition key.
 * 
 * When no partition key is specified:
 * - Kafka uses round-robin or random distribution across partitions
 * - Events for the same booking ID can go to different partitions
 * - Different partitions are consumed independently
 * - Order is NOT guaranteed across partitions
 * 
 * RESULT: A booking's events (CREATED -> SEAT_SELECTED -> PAYMENT -> CONFIRMED)
 *         can be processed out of order, causing business logic failures.
 */
@Slf4j
@Service
public class BookingProducerProblem {

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;
    private final String topic;

    public BookingProducerProblem(
            KafkaTemplate<String, BookingEvent> kafkaTemplate,
            @Value("${kafka.topic.booking.problem}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * PROBLEM: Sending without partition key
     * The message will be distributed randomly across partitions
     */
    public void sendBookingEvent(BookingEvent event) {
        log.warn("⚠️  PROBLEM PRODUCER - Sending WITHOUT partition key: {}", event.getEventType());
        
        // NO PARTITION KEY - This is the problem!
        CompletableFuture<SendResult<String, BookingEvent>> future = 
            kafkaTemplate.send(topic, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("📤 Sent (NO KEY): {} to partition {} offset {}",
                        event.getEventType(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send event: {}", event.getEventType(), ex);
            }
        });
    }
}
