package com.example.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
public class KafkaAirlineBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaAirlineBookingApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("\n");
        log.info("╔══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                              ║");
        log.info("║          ✈️  KAFKA ORDERING PROBLEM DEMONSTRATION - AIRLINE BOOKING  ✈️        ║");
        log.info("║                                                                              ║");
        log.info("╚══════════════════════════════════════════════════════════════════════════════╝");
        log.info("\n");
        log.info("🚀 Application is ready!");
        log.info("📍 Server running at: http://localhost:8081");
        log.info("📊 Kafka UI available at: http://localhost:8080");
        log.info("\n");
        log.info("═════════════════════════════════════════════════════════════════════════════");
        log.info("                           API ENDPOINTS");
        log.info("═════════════════════════════════════════════════════════════════════════════");
        log.info("\n");
        log.info("🔴 DEMONSTRATE THE PROBLEM (no partition key):");
        log.info("   POST http://localhost:8081/api/bookings/demo-problem");
        log.info("   curl -X POST http://localhost:8081/api/bookings/demo-problem");
        log.info("\n");
        log.info("🟢 DEMONSTRATE THE SOLUTION (with partition key):");
        log.info("   POST http://localhost:8081/api/bookings/demo-solved");
        log.info("   curl -X POST http://localhost:8081/api/bookings/demo-solved");
        log.info("\n");
        log.info("📊 DEMONSTRATE MULTIPLE BOOKINGS:");
        log.info("   POST http://localhost:8081/api/bookings/demo-multiple");
        log.info("   curl -X POST http://localhost:8081/api/bookings/demo-multiple");
        log.info("\n");
        log.info("⚖️  SIDE-BY-SIDE COMPARISON:");
        log.info("   POST http://localhost:8081/api/bookings/demo-comparison");
        log.info("   curl -X POST http://localhost:8081/api/bookings/demo-comparison");
        log.info("\n");
        log.info("═════════════════════════════════════════════════════════════════════════════");
        log.info("\n");
        log.info("📖 WHAT TO EXPECT:");
        log.info("\n");
        log.info("   🔴 PROBLEM:");
        log.info("      • Events sent WITHOUT partition key");
        log.info("      • Messages distributed randomly across partitions");
        log.info("      • Events processed OUT OF ORDER");
        log.info("      • Business logic validation FAILS");
        log.info("      • Example: PAYMENT before BOOKING_CREATED");
        log.info("\n");
        log.info("   🟢 SOLUTION:");
        log.info("      • Events sent WITH partition key (bookingId)");
        log.info("      • Same bookingId → Same partition");
        log.info("      • Events processed IN ORDER");
        log.info("      • Business logic validation SUCCEEDS");
        log.info("      • Proper sequence: CREATED → SEAT → PAYMENT → CONFIRMED");
        log.info("\n");
        log.info("═════════════════════════════════════════════════════════════════════════════");
        log.info("\n");
    }
}
