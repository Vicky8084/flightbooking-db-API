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
    // ✅ REMOVED @JsonProperty(access = WRITE_ONLY) so password can be read
    // ✅ Added @JsonProperty for both read and write
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @ManyToOne
    @JoinColumn(name = "airline_id")
    private Airline airline;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private Boolean isActive = true;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;


    private LocalDateTime approvedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Booking> bookings;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Review> reviews;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (this.role == UserRole.CUSTOMER) {
            this.status = AccountStatus.ACTIVE;
        } else if (this.role == UserRole.SYSTEM_ADMIN) {
            this.status = AccountStatus.ACTIVE;
        } else if (this.role == UserRole.AIRLINE_ADMIN) {
            // ✅ AIRLINE_ADMIN should be PENDING by default
            // But if status is already set from request, use that
            if (this.status == null) {
                this.status = AccountStatus.PENDING;
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}