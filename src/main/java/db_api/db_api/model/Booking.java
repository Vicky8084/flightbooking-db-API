package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import db_api.db_api.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pnrNumber; // e.g., "ABC123"

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status; // PENDING, CONFIRMED, CANCELLED, COMPLETED

    @JsonIgnore
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingFlight> bookingFlights;

    @JsonIgnore
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<Passenger> passengers;

    @JsonIgnore
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;

    @PrePersist
    protected void onCreate() {
        bookingTime = LocalDateTime.now();
    }

    // Getters and Setters
}
