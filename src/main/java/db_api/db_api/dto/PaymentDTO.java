package db_api.db_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentDTO {

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    private Double amount;

    // Getters and Setters
}
