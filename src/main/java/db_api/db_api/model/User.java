package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import db_api.db_api.enums.AccountStatus;
import db_api.db_api.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole role;  // SYSTEM_ADMIN, AIRLINE_ADMIN, CUSTOMER

    // For AIRLINE_ADMIN only - which airline they belong to
    @ManyToOne
    @JoinColumn(name = "airline_id")
    private Airline airline;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;  // PENDING, ACTIVE, SUSPENDED, REJECTED

    private Boolean isActive = true;

    // Who approved this user
    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Booking> bookings;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Set default status based on role
        if (this.role == UserRole.CUSTOMER) {
            this.status = AccountStatus.ACTIVE;  // Customers are active directly
        } else if (this.role == UserRole.SYSTEM_ADMIN) {
            this.status = AccountStatus.ACTIVE;  // System admin is active
        } else {
            this.status = AccountStatus.PENDING;  // Airline admins need approval
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}