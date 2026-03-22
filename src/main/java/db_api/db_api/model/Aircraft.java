package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import db_api.db_api.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "aircrafts")
@Data
public class Aircraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String manufacturer;

    private Integer totalSeats;

    // ========== SEAT DISTRIBUTION ==========

    // Economy class seat counts
    private Integer economySeats;
    private Integer economyWindowSeats;
    private Integer economyAisleSeats;
    private Integer economyMiddleSeats;

    // Business class seat counts
    private Integer businessSeats;
    private Integer businessWindowSeats;
    private Integer businessAisleSeats;
    private Integer businessMiddleSeats;

    // First class seat counts
    private Integer firstClassSeats;
    private Integer firstClassWindowSeats;
    private Integer firstClassAisleSeats;
    private Integer firstClassMiddleSeats;

    // ========== PREMIUM CONFIGURATIONS (For Seat Extra Price) ==========
    private Integer windowSeatPremiumPercent = 15;    // 15% extra for window seats
    private Integer aisleSeatPremiumPercent = 10;     // 10% extra for aisle seats
    private Integer middleSeatDiscountPercent = 5;    // 5% discount for middle seats
    private Double extraLegroomPremium = 1000.0;      // ₹1000 extra for legroom
    private Double exitRowPremium = 750.0;            // ₹750 extra for exit row


    @ManyToOne
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @JsonIgnore
    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL)
    private List<Flight> flights;

    @JsonIgnore
    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL)
    private List<Seat> seats;

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