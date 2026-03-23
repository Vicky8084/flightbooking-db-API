package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import db_api.db_api.enums.FlightStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "flights")
@Data
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String flightNumber;

    @ManyToOne
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @ManyToOne
    @JoinColumn(name = "source_airport_id", nullable = false)
    private Airport sourceAirport;

    @ManyToOne
    @JoinColumn(name = "destination_airport_id", nullable = false)
    private Airport destinationAirport;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    private Integer duration;

    // ========== BASE PRICES ==========
    @Column(nullable = false)
    private Double basePriceEconomy;
    private Double basePriceBusiness;
    private Double basePriceFirstClass;

    // ========== DYNAMIC PRICING FIELDS ==========
    private Double currentPriceEconomy;
    private Double currentPriceBusiness;
    private Double currentPriceFirstClass;

    // Dynamic pricing factors
    private Double demandMultiplier = 1.0;  // Based on occupancy
    private Double timeMultiplier = 1.0;    // Based on days left
    private Double dayMultiplier = 1.0;     // Based on day of week
    private Double finalPriceMultiplier = 1.0;  // Combined multiplier

    // Dynamic pricing settings (can be airline specific)
    private Integer earlyBirdDays = 30;      // Days for early bird discount
    private Double earlyBirdDiscount = 0.85; // 15% off
    private Integer lastMinuteDays = 3;      // Days for last minute premium
    private Double lastMinutePremium = 1.5;  // 50% premium

    private Double highDemandThreshold = 0.8;  // 80% occupancy triggers high demand
    private Double highDemandMultiplier = 1.3;  // 30% premium
    private Double mediumDemandThreshold = 0.6;
    private Double mediumDemandMultiplier = 1.15;
    private Double lowDemandThreshold = 0.4;
    private Double lowDemandMultiplier = 1.05;

    // Weekend pricing
    private Double weekendMultiplier = 1.2;    // Saturday/Sunday: 20% premium
    private Double weekdayDiscount = 0.9;      // Tuesday/Wednesday: 10% discount

    @Enumerated(EnumType.STRING)
    private FlightStatus status;

    // Seat availability tracking
    private Integer availableEconomySeats;
    private Integer availableBusinessSeats;
    private Integer availableFirstClassSeats;

    // Track sold seats for dynamic pricing
    private Integer soldEconomySeats = 0;      // ✅ Default value 0
    private Integer soldBusinessSeats = 0;    // ✅ Default value 0
    private Integer soldFirstClassSeats = 0;

    // Booking cutoff time (hours before departure)
    private Integer bookingCutoffHours = 2;

    // Delay tracking
    private Integer delayCount = 0;
    private Integer totalDelayMinutes = 0;
    private LocalDateTime originalDepartureTime;
    private LocalDateTime originalArrivalTime;

    @OneToMany(mappedBy = "parentFlight", cascade = CascadeType.ALL)
    private List<FlightSegment> segments;

    @JsonIgnore
    @OneToMany(mappedBy = "flight")
    private List<BookingFlight> bookingFlights;

    /**
     * Check if booking is still allowed
     */
    public boolean canBook() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = departureTime.minusHours(bookingCutoffHours);
        return status == FlightStatus.SCHEDULED && now.isBefore(cutoffTime);
    }

    /**
     * Calculate occupancy percentage for a seat class
     */
    public double getOccupancyPercentage(String seatClass) {
        int total, sold;
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                total = aircraft.getBusinessSeats();
                sold = soldBusinessSeats;
                break;
            case "FIRST":
                total = aircraft.getFirstClassSeats();
                sold = soldFirstClassSeats;
                break;
            default:
                total = aircraft.getEconomySeats();
                sold = soldEconomySeats;
        }
        if (total == 0) return 0;
        return (double) sold / total;
    }

    /**
     * Calculate days until departure
     */
    public int getDaysUntilDeparture() {
        return (int) java.time.Duration.between(LocalDateTime.now(), departureTime).toDays();
    }

    /**
     * Get day of week multiplier (0 = Monday, 6 = Sunday)
     */
    public double getDayOfWeekMultiplier() {
        int dayOfWeek = departureTime.getDayOfWeek().getValue();
        // Saturday (6) or Sunday (7)
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return weekendMultiplier;
        }
        // Tuesday (2) or Wednesday (3)
        if (dayOfWeek == 2 || dayOfWeek == 3) {
            return weekdayDiscount;
        }
        return 1.0;
    }

    /**
     * Calculate time-based multiplier (early bird discount / last minute premium)
     */
    public double getTimeMultiplier() {
        int daysLeft = getDaysUntilDeparture();

        if (daysLeft > earlyBirdDays) {
            return earlyBirdDiscount;  // Early bird discount
        } else if (daysLeft <= lastMinuteDays && daysLeft > 0) {
            return lastMinutePremium;  // Last minute premium
        }
        return 1.0;
    }

    /**
     * Calculate demand-based multiplier based on occupancy
     */
    public double getDemandMultiplier() {
        double occupancy = getOccupancyPercentage("ECONOMY");

        if (occupancy >= highDemandThreshold) {
            return highDemandMultiplier;
        } else if (occupancy >= mediumDemandThreshold) {
            return mediumDemandMultiplier;
        } else if (occupancy >= lowDemandThreshold) {
            return lowDemandMultiplier;
        }
        return 1.0;
    }

    /**
     * Calculate final dynamic price for a seat class
     */
    public double calculateDynamicPrice(String seatClass) {
        double basePrice;
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                basePrice = basePriceBusiness;
                break;
            case "FIRST":
                basePrice = basePriceFirstClass;
                break;
            default:
                basePrice = basePriceEconomy;
        }

        // Update multipliers based on current conditions
        this.timeMultiplier = getTimeMultiplier();
        this.demandMultiplier = getDemandMultiplier();
        this.dayMultiplier = getDayOfWeekMultiplier();
        this.finalPriceMultiplier = timeMultiplier * demandMultiplier * dayMultiplier;

        double finalPrice = basePrice * finalPriceMultiplier;

        // Round to nearest integer
        return Math.round(finalPrice);
    }

    /**
     * Get current price with dynamic pricing applied
     */
    public double getCurrentPrice(String seatClass) {
        double dynamicPrice = calculateDynamicPrice(seatClass);

        // Update stored current price
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                this.currentPriceBusiness = dynamicPrice;
                break;
            case "FIRST":
                this.currentPriceFirstClass = dynamicPrice;
                break;
            default:
                this.currentPriceEconomy = dynamicPrice;
        }

        return dynamicPrice;
    }

    /**
     * Update all current prices based on dynamic pricing
     */
    public void updateAllCurrentPrices() {
        this.currentPriceEconomy = calculateDynamicPrice("ECONOMY");
        this.currentPriceBusiness = calculateDynamicPrice("BUSINESS");
        this.currentPriceFirstClass = calculateDynamicPrice("FIRST");
    }

    /**
     * Increment sold seats count when booking is made
     */
    public void incrementSoldSeats(String seatClass, int count) {
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                this.soldBusinessSeats += count;
                break;
            case "FIRST":
                this.soldFirstClassSeats += count;
                break;
            default:
                this.soldEconomySeats += count;
        }
        // Update available seats
        updateAvailableSeats();
        // Recalculate prices after seat sold
        updateAllCurrentPrices();
    }

    /**
     * Decrement sold seats when booking is cancelled
     */
    public void decrementSoldSeats(String seatClass, int count) {
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                this.soldBusinessSeats = Math.max(0, this.soldBusinessSeats - count);
                break;
            case "FIRST":
                this.soldFirstClassSeats = Math.max(0, this.soldFirstClassSeats - count);
                break;
            default:
                this.soldEconomySeats = Math.max(0, this.soldEconomySeats - count);
        }
        updateAvailableSeats();
        updateAllCurrentPrices();
    }

    /**
     * Update available seats based on sold seats
     */
    private void updateAvailableSeats() {
        this.availableEconomySeats = aircraft.getEconomySeats() - soldEconomySeats;
        this.availableBusinessSeats = aircraft.getBusinessSeats() - soldBusinessSeats;
        this.availableFirstClassSeats = aircraft.getFirstClassSeats() - soldFirstClassSeats;
    }

    @PrePersist
    protected void onCreate() {
        this.currentPriceEconomy = this.basePriceEconomy;
        this.currentPriceBusiness = this.basePriceBusiness;
        this.currentPriceFirstClass = this.basePriceFirstClass;
        this.originalDepartureTime = this.departureTime;
        this.originalArrivalTime = this.arrivalTime;

        // Initialize available seats from aircraft
        if (aircraft != null) {
            this.availableEconomySeats = aircraft.getEconomySeats();
            this.availableBusinessSeats = aircraft.getBusinessSeats();
            this.availableFirstClassSeats = aircraft.getFirstClassSeats();
        }

        // ✅ CRITICAL FIX: Initialize sold seats to 0
        if (this.soldEconomySeats == null) this.soldEconomySeats = 0;
        if (this.soldBusinessSeats == null) this.soldBusinessSeats = 0;
        if (this.soldFirstClassSeats == null) this.soldFirstClassSeats = 0;

        // Initialize dynamic pricing
        updateAllCurrentPrices();
    }
}