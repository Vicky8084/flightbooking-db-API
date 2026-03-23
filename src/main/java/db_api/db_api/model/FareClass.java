package db_api.db_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "fare_classes")
@Data
public class FareClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;  // LITE, STANDARD, FLEX, BUSINESS, FIRST

    @Column(nullable = false)
    private String name;  // Economy Lite, Economy Standard, Economy Flex

    private String description;

    // Price multiplier relative to base price
    @Column(nullable = false)
    private Double priceMultiplier = 1.0;

    // ========== BAGGAGE ALLOWANCE ==========
    @Column(nullable = false)
    private Integer cabinBaggageKg = 7;  // Hand luggage

    @Column(nullable = false)
    private Integer checkInBaggageKg = 15;  // Checked luggage

    private Double extraBaggageRatePerKg = 500.0;  // ₹ per kg

    // ========== MEAL PREFERENCES ==========
    private Boolean mealIncluded = false;
    private String mealType;  // VEG, NON_VEG, JAL, KOSHER, etc.

    // ========== CANCELLATION RULES ==========
    private Double cancellationFee = 0.0;  // Fixed fee or percentage

    private String cancellationPolicy;  // JSON string with rules

    // ========== CHANGE RULES ==========
    private Double changeFee = 0.0;
    private Boolean allowDateChange = true;
    private Boolean allowRouteChange = false;

    // ========== SEAT SELECTION ==========
    private Boolean seatSelectionFree = false;
    private Boolean prioritySeatSelection = false;  // Extra legroom, exit row

    // ========== ADDITIONAL BENEFITS ==========
    private Boolean priorityCheckin = false;
    private Boolean priorityBoarding = false;
    private Boolean loungeAccess = false;
    private Boolean chauffeurService = false;

    // ========== REFUND PERCENTAGES (Days before departure) ==========
    // Format: "30:100,15:75,7:50,3:25,0:0"
    private String refundPercentageByDays;

    private Boolean isActive = true;

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