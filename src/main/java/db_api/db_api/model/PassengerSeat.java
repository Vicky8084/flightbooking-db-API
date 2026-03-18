package db_api.db_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "passenger_seats")
@Data
public class PassengerSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_flight_id", nullable = false)
    private BookingFlight bookingFlight;

    @OneToOne
    @JoinColumn(name = "passenger_id", nullable = false, unique = true)
    private Passenger passenger;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    private Double seatPrice; // Price paid for this specific seat

    // Getters and Setters
}
