package com.example.kafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEvent {
    
    private String bookingId;
    private String customerId;
    private String flightNumber;
    private String eventType; // CREATED, SEAT_SELECTED, PAYMENT_INITIATED, PAYMENT_COMPLETED, CONFIRMED, CANCELLED
    private String seatNumber;
    private Double amount;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private Integer sequenceNumber;
    
    @Override
    public String toString() {
        return String.format("[%s] Booking: %s | Customer: %s | Flight: %s | Type: %s | Seat: %s | Amount: %.2f | Seq: %d",
                timestamp, bookingId, customerId, flightNumber, eventType, 
                seatNumber != null ? seatNumber : "N/A", 
                amount != null ? amount : 0.0, 
                sequenceNumber);
    }
}
