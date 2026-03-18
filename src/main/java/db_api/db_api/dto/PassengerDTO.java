package db_api.db_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PassengerDTO {

    @NotBlank(message = "Passenger name is required")
    private String fullName;

    @Min(value = 0, message = "Age must be positive")
    @Max(value = 120, message = "Age must be valid")
    private Integer age;

    private String gender;

    private String passportNumber;

    private String nationality;

    // Getters and Setters
}
