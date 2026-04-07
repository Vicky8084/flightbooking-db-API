package db_api.db_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerSeatDTO {

    @NotNull(message = "Seat ID is required")
    private Long seatId;

    private Integer passengerIndex;
}