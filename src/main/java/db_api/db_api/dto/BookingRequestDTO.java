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
public class BookingRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    private List<FlightBookingDTO> flights;

    private Long flightId;

    @NotEmpty(message = "At least one passenger is required")
    private List<PassengerDTO> passengers;

    private PaymentDTO payment;

    private String fareClassCode;
}