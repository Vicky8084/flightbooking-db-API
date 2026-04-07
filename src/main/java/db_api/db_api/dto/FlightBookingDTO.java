package db_api.db_api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightBookingDTO {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    private Integer sequence;

    @NotEmpty(message = "Seat selection required for each passenger")
    private List<PassengerSeatDTO> passengerSeats;
}