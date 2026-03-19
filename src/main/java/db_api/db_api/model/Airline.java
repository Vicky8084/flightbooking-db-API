package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import db_api.db_api.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "airlines")
@Data
public class Airline {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;           // Air India, IndiGo, etc.

    @Column(nullable = false, unique = true, length = 10)
    private String code;           // AI, 6E, G8 etc.

    private String registrationNumber;  // Company registration number

    private String contactEmail;

    private String contactPhone;

    private String address;

    private String website;

    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.PENDING;  // Default PENDING

    // Who approved this airline
    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    // Who rejected this airline
    @ManyToOne
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectionReason;

    @JsonIgnore
    @OneToMany(mappedBy = "airline")
    private List<User> admins;      // Multiple admins for one airline

    @JsonIgnore
    @OneToMany(mappedBy = "airline")
    private List<Aircraft> aircrafts;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}