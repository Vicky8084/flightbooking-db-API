package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonIgnore
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    @JsonProperty("flight")
    private Flight flight;

    private Integer flightSequence;

    @OneToMany(mappedBy = "bookingFlight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("passengerSeats")
    private List<PassengerSeat> passengerSeats;
}