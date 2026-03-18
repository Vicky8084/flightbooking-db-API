package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "booking_flights")
@Data
public class BookingFlight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    private Integer flightSequence; // 1 for first flight in connection

    @JsonIgnore
    @OneToMany(mappedBy = "bookingFlight", cascade = CascadeType.ALL)
    private List<PassengerSeat> passengerSeats;

    // Getters and Setters
}
