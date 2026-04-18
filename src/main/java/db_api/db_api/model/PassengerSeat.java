package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonIgnore
    private BookingFlight bookingFlight;

    @OneToOne
    @JoinColumn(name = "passenger_id", nullable = false, unique = true)
    @JsonProperty("passenger")
    private Passenger passenger;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    @JsonProperty("seat")
    private Seat seat;

    @JsonProperty("seatPrice")
    private Double seatPrice;
}