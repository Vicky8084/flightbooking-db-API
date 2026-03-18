package db_api.db_api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
@Data
public class FlightBookingDTO {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    private Integer sequence; // For connecting flights

    @NotEmpty(message = "Seat selection required for each passenger")
    private List<PassengerSeatDTO> passengerSeats;

    // Getters and Setters
}
