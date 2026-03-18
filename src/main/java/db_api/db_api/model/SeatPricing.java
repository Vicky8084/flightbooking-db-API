package db_api.db_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "seat_pricing")
@Data
public class SeatPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    private Double price; // Final price for this seat on this flight

    private Boolean isAvailable = true;

    private LocalDate validFrom;

    private LocalDate validTo;

    // Getters and Setters
}
