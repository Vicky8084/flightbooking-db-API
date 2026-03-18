package db_api.db_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
@Data
public class FlightSearchDTO {

    @NotBlank(message = "Source airport code is required")
    private String sourceCode;

    @NotBlank(message = "Destination airport code is required")
    private String destinationCode;

    @NotNull(message = "Travel date is required")
    private LocalDate travelDate;

    private Boolean includeConnectingFlights = true;

    private String seatClass;

    private Double maxPrice;

    // Getters and Setters
}
