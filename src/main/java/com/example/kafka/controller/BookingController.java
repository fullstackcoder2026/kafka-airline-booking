package com.example.kafka.controller;

import com.example.kafka.model.BookingEvent;
import com.example.kafka.producer.BookingProducerProblem;
import com.example.kafka.producer.BookingProducerSolved;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingProducerProblem problemProducer;
    private final BookingProducerSolved solvedProducer;

    /**
     * Demonstrates the PROBLEM - events sent without partition key
     */
    @PostMapping("/demo-problem")
    public ResponseEntity<String> demoProblem() {
        log.info("\n========================================");
        log.info("🔴 DEMONSTRATING THE PROBLEM");
        log.info("========================================\n");
        
        List<BookingEvent> events = createBookingEventsForDemo("BK001", "CUST001", "AA100");
        
        log.info("Sending {} events WITHOUT partition key (will go to random partitions)...\n", events.size());
        
        for (BookingEvent event : events) {
            problemProducer.sendBookingEvent(event);
            sleep(100); // Small delay to simulate real-world timing
        }
        
        return ResponseEntity.ok("Problem demo started! Check logs for out-of-order messages.");
    }

    /**
     * Demonstrates the SOLUTION - events sent with partition key
     */
    @PostMapping("/demo-solved")
    public ResponseEntity<String> demoSolved() {
        log.info("\n========================================");
        log.info("🟢 DEMONSTRATING THE SOLUTION");
        log.info("========================================\n");
        
        List<BookingEvent> events = createBookingEventsForDemo("BK002", "CUST002", "UA200");
        
        log.info("Sending {} events WITH partition key (all to same partition)...\n", events.size());
        
        for (BookingEvent event : events) {
            solvedProducer.sendBookingEvent(event);
            sleep(100);
        }
        
        return ResponseEntity.ok("Solution demo started! Check logs for ordered messages.");
    }

    /**
     * Send multiple bookings to demonstrate partitioning
     */
    @PostMapping("/demo-multiple")
    public ResponseEntity<String> demoMultipleBookings() {
        log.info("\n========================================");
        log.info("🟢 DEMONSTRATING MULTIPLE BOOKINGS");
        log.info("========================================\n");
        
        // Create 3 different bookings
        String[][] bookings = {
            {"BK101", "CUST101", "DL300"},
            {"BK102", "CUST102", "UA400"},
            {"BK103", "CUST103", "AA500"}
        };
        
        log.info("Sending events for {} bookings with partition keys...\n", bookings.length);
        
        for (String[] booking : bookings) {
            List<BookingEvent> events = createBookingEventsForDemo(booking[0], booking[1], booking[2]);
            for (BookingEvent event : events) {
                solvedProducer.sendBookingEvent(event);
                sleep(50);
            }
        }
        
        return ResponseEntity.ok("Multiple bookings demo started! Each booking goes to consistent partition.");
    }

    /**
     * Compare problem vs solution side by side
     */
    @PostMapping("/demo-comparison")
    public ResponseEntity<String> demoComparison() {
        log.info("\n========================================");
        log.info("⚖️  SIDE-BY-SIDE COMPARISON");
        log.info("========================================\n");
        
        // Problem case
        log.info("--- PROBLEM: Sending booking BK999 WITHOUT key ---");
        List<BookingEvent> problemEvents = createBookingEventsForDemo("BK999", "CUST999", "BA999");
        for (BookingEvent event : problemEvents) {
            problemProducer.sendBookingEvent(event);
            sleep(100);
        }
        
        sleep(2000); // Wait 2 seconds
        
        // Solution case
        log.info("\n--- SOLUTION: Sending booking BK888 WITH key ---");
        List<BookingEvent> solvedEvents = createBookingEventsForDemo("BK888", "CUST888", "EK888");
        for (BookingEvent event : solvedEvents) {
            solvedProducer.sendBookingEvent(event);
            sleep(100);
        }
        
        return ResponseEntity.ok("Comparison demo complete! Compare the logs.");
    }

    /**
     * Create a sequence of booking events
     */
    private List<BookingEvent> createBookingEventsForDemo(String bookingId, String customerId, String flightNumber) {
        List<BookingEvent> events = new ArrayList<>();
        
        events.add(BookingEvent.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .flightNumber(flightNumber)
                .eventType("CREATED")
                .timestamp(LocalDateTime.now())
                .sequenceNumber(1)
                .build());
        
        events.add(BookingEvent.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .flightNumber(flightNumber)
                .eventType("SEAT_SELECTED")
                .seatNumber("12A")
                .timestamp(LocalDateTime.now())
                .sequenceNumber(2)
                .build());
        
        events.add(BookingEvent.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .flightNumber(flightNumber)
                .eventType("PAYMENT_INITIATED")
                .amount(299.99)
                .timestamp(LocalDateTime.now())
                .sequenceNumber(3)
                .build());
        
        events.add(BookingEvent.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .flightNumber(flightNumber)
                .eventType("PAYMENT_COMPLETED")
                .amount(299.99)
                .timestamp(LocalDateTime.now())
                .sequenceNumber(4)
                .build());
        
        events.add(BookingEvent.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .flightNumber(flightNumber)
                .eventType("CONFIRMED")
                .timestamp(LocalDateTime.now())
                .sequenceNumber(5)
                .build());
        
        return events;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
