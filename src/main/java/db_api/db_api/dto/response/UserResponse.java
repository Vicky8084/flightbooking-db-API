package db_api.db_api.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String password;  // ✅ ADDED PASSWORD FIELD
    private String fullName;
    private String phoneNumber;
    private String role;
    private Long airlineId;
    private String airlineName;
    private String status;
    private boolean isActive;
}