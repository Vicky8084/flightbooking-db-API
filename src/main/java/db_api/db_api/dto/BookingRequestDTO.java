package db_api.db_api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class BookingRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotEmpty(message = "At least one flight is required")
    private List<FlightBookingDTO> flights;

    @NotEmpty(message = "At least one passenger is required")
    private List<PassengerDTO> passengers;

    private PaymentDTO payment;
    private String fareClassCode;

    // Getters and Setters
}
