package db_api.db_api.model;


import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"aircraft_id", "seat_number"})
})
@Data
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @Column(nullable = false)
    private String seatNumber; // e.g., "12A", "14B"

    @Enumerated(EnumType.STRING)
    private SeatClass seatClass; // ECONOMY, BUSINESS, FIRST

    @Enumerated(EnumType.STRING)
    private SeatType seatType; // WINDOW, AISLE, MIDDLE

    private Boolean hasExtraLegroom = false;

    private Boolean isNearExit = false;

    private Double extraPrice; // Additional price over base fare

    private Boolean isActive = true;

    // Getters and Setters
}
