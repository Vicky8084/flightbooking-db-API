package db_api.db_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PassengerSeatDTO {

    @NotNull(message = "Seat ID is required")
    private Long seatId;

    private Integer passengerIndex; // Index in passengers list

    // Getters and Setters
}
