package com.example.kafka.producer;

import com.example.kafka.model.BookingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * SOLUTION PRODUCER
 * 
 * This producer demonstrates the SOLUTION to the ordering problem.
 * 
 * SOLUTION: Messages are sent WITH a partition key (bookingId).
 * 
 * When a partition key is specified:
 * - Kafka uses hash(key) % num_partitions to determine the partition
 * - All events with the same key go to the SAME partition
 * - Messages within a partition are strictly ordered
 * - Same-key events are guaranteed to be consumed in order
 * 
 * RESULT: All events for booking "BK001" go to the same partition,
 *         ensuring they are processed in the correct sequence.
 */
@Slf4j
@Service
public class BookingProducerSolved {

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;
    private final String topic;

    public BookingProducerSolved(
            KafkaTemplate<String, BookingEvent> kafkaTemplate,
            @Value("${kafka.topic.booking.solved}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * SOLUTION: Sending with partition key (bookingId)
     * All messages with the same bookingId will go to the same partition
     */
    public void sendBookingEvent(BookingEvent event) {
        log.info("✅ SOLUTION PRODUCER - Sending WITH partition key: {} [BookingID: {}]", 
                event.getEventType(), event.getBookingId());
        
        // PARTITION KEY = bookingId - This ensures ordering!
        String partitionKey = event.getBookingId();
        
        CompletableFuture<SendResult<String, BookingEvent>> future = 
            kafkaTemplate.send(topic, partitionKey, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("📤 Sent (KEY={}): {} to partition {} offset {}",
                        partitionKey,
                        event.getEventType(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send event: {}", event.getEventType(), ex);
            }
        });
    }
}
