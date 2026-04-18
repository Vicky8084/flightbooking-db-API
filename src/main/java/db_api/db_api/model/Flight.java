package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("aircraft")
    private Aircraft aircraft;

    @ManyToOne
    @JoinColumn(name = "source_airport_id", nullable = false)
    @JsonProperty("sourceAirport")
    private Airport sourceAirport;

    @ManyToOne
    @JoinColumn(name = "destination_airport_id", nullable = false)
    @JsonProperty("destinationAirport")
    private Airport destinationAirport;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    private Integer duration;

    @Column(nullable = false)
    private Double basePriceEconomy;
    private Double basePriceBusiness;
    private Double basePriceFirstClass;

    private Double currentPriceEconomy;
    private Double currentPriceBusiness;
    private Double currentPriceFirstClass;

    private Double demandMultiplier = 1.0;
    private Double timeMultiplier = 1.0;
    private Double dayMultiplier = 1.0;
    private Double finalPriceMultiplier = 1.0;

    private Integer earlyBirdDays = 30;
    private Double earlyBirdDiscount = 0.85;
    private Integer lastMinuteDays = 3;
    private Double lastMinutePremium = 1.5;

    private Double highDemandThreshold = 0.8;
    private Double highDemandMultiplier = 1.3;
    private Double mediumDemandThreshold = 0.6;
    private Double mediumDemandMultiplier = 1.15;
    private Double lowDemandThreshold = 0.4;
    private Double lowDemandMultiplier = 1.05;

    private Double weekendMultiplier = 1.2;
    private Double weekdayDiscount = 0.9;

    @Enumerated(EnumType.STRING)
    private FlightStatus status;

    private Integer availableEconomySeats;
    private Integer availableBusinessSeats;
    private Integer availableFirstClassSeats;

    private Integer soldEconomySeats = 0;
    private Integer soldBusinessSeats = 0;
    private Integer soldFirstClassSeats = 0;

    private Integer bookingCutoffHours = 2;

    private Integer delayCount = 0;
    private Integer totalDelayMinutes = 0;
    private LocalDateTime originalDepartureTime;
    private LocalDateTime originalArrivalTime;

    @OneToMany(mappedBy = "parentFlight", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<FlightSegment> segments;

    @OneToMany(mappedBy = "flight")
    @JsonIgnore
    private List<BookingFlight> bookingFlights;

    // Helper methods (keep as is)
    public boolean canBook() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = departureTime.minusHours(bookingCutoffHours);
        return status == FlightStatus.SCHEDULED && now.isBefore(cutoffTime);
    }

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

    public int getDaysUntilDeparture() {
        return (int) java.time.Duration.between(LocalDateTime.now(), departureTime).toDays();
    }

    public double getDayOfWeekMultiplier() {
        int dayOfWeek = departureTime.getDayOfWeek().getValue();
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return weekendMultiplier;
        }
        if (dayOfWeek == 2 || dayOfWeek == 3) {
            return weekdayDiscount;
        }
        return 1.0;
    }

    public double getTimeMultiplier() {
        int daysLeft = getDaysUntilDeparture();
        if (daysLeft > earlyBirdDays) {
            return earlyBirdDiscount;
        } else if (daysLeft <= lastMinuteDays && daysLeft > 0) {
            return lastMinutePremium;
        }
        return 1.0;
    }

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
        this.timeMultiplier = getTimeMultiplier();
        this.demandMultiplier = getDemandMultiplier();
        this.dayMultiplier = getDayOfWeekMultiplier();
        this.finalPriceMultiplier = timeMultiplier * demandMultiplier * dayMultiplier;
        double finalPrice = basePrice * finalPriceMultiplier;
        return Math.round(finalPrice);
    }

    public double getCurrentPrice(String seatClass) {
        double dynamicPrice = calculateDynamicPrice(seatClass);
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

    public void updateAllCurrentPrices() {
        this.currentPriceEconomy = this.basePriceEconomy;
        this.currentPriceBusiness = this.basePriceBusiness;
        this.currentPriceFirstClass = this.basePriceFirstClass;
    }

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
        updateAvailableSeats();
        updateAllCurrentPrices();
    }

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

        if (aircraft != null) {
            this.availableEconomySeats = aircraft.getEconomySeats();
            this.availableBusinessSeats = aircraft.getBusinessSeats();
            this.availableFirstClassSeats = aircraft.getFirstClassSeats();
        }

        if (this.soldEconomySeats == null) this.soldEconomySeats = 0;
        if (this.soldBusinessSeats == null) this.soldBusinessSeats = 0;
        if (this.soldFirstClassSeats == null) this.soldFirstClassSeats = 0;

        updateAllCurrentPrices();
    }
}