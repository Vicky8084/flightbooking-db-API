package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import db_api.db_api.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pnrNumber;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    private Double totalAmount;

    @ManyToOne
    @JoinColumn(name = "fare_class_id")
    private FareClass fareClass;

    private String fareClassCode;

    private Double cancellationFee;
    private Double changeFee;
    private Double refundAmount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private Double totalBaggagePrice = 0.0;
    private Integer totalExtraBaggageKg = 0;

    // ✅ FIXED: Removed @JsonIgnore - Now appears in JSON response
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("bookingFlights")
    private List<BookingFlight> bookingFlights;

    // ✅ FIXED: Removed @JsonIgnore - Now appears in JSON response
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("passengers")
    private List<Passenger> passengers;

    // ✅ FIXED: Removed @JsonIgnore - Now appears in JSON response
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("extraBaggages")
    private List<ExtraBaggage> extraBaggages;

    // ✅ FIXED: Removed @JsonIgnore - Now appears in JSON response
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("mealPreferences")
    private List<MealPreference> mealPreferences;

    // ✅ FIXED: Removed @JsonIgnore - Now appears in JSON response
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("payment")
    private Payment payment;

    @PrePersist
    protected void onCreate() {
        bookingTime = LocalDateTime.now();
    }

    /**
     * Calculate if booking can be cancelled and refund amount
     */
    public double calculateRefundAmount(LocalDateTime cancellationTime) {
        if (fareClass == null || "NON_REFUNDABLE".equals(fareClassCode)) {
            return 0.0;
        }

        long daysBeforeDeparture = 0;
        if (bookingFlights != null && !bookingFlights.isEmpty()) {
            Flight flight = bookingFlights.get(0).getFlight();
            daysBeforeDeparture = java.time.Duration.between(
                    cancellationTime, flight.getDepartureTime()
            ).toDays();
        }

        if (fareClass.getRefundPercentageByDays() != null) {
            String[] rules = fareClass.getRefundPercentageByDays().split(",");
            for (String rule : rules) {
                String[] parts = rule.split(":");
                int thresholdDays = Integer.parseInt(parts[0]);
                double refundPercent = Double.parseDouble(parts[1]);
                if (daysBeforeDeparture >= thresholdDays) {
                    return totalAmount * refundPercent / 100;
                }
            }
        }

        double refund = totalAmount - (cancellationFee != null ? cancellationFee : 0);
        return Math.max(0, refund);
    }
}