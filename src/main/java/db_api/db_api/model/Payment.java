package db_api.db_api.model;


import db_api.db_api.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, unique = true)
    private String transactionId;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, SUCCESS, FAILED, REFUNDED

    private String paymentMethod; // CREDIT_CARD, DEBIT_CARD, UPI, etc.

    private LocalDateTime paymentTime;

    private String gatewayResponse;

    @PrePersist
    protected void onCreate() {
        paymentTime = LocalDateTime.now();
    }

    // Getters and Setters
}
