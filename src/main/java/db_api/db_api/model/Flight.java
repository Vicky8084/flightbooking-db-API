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

    // ✅ PRICE FIELDS - KEEP THESE (Set at Flight creation time)
    @Column(nullable = false)
    private Double basePriceEconomy;
    private Double basePriceBusiness;
    private Double basePriceFirstClass;

    // Current prices (can change based on demand/time)
    private Double currentPriceEconomy;
    private Double currentPriceBusiness;
    private Double currentPriceFirstClass;

    @Enumerated(EnumType.STRING)
    private FlightStatus status;

    // Seat availability tracking
    private Integer availableEconomySeats;
    private Integer availableBusinessSeats;
    private Integer availableFirstClassSeats;

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

    // Helper method to check if booking is still allowed
    public boolean canBook() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = departureTime.minusHours(bookingCutoffHours);
        return status == FlightStatus.SCHEDULED && now.isBefore(cutoffTime);
    }

    // Helper method to get current price based on class and booking time
    public double getCurrentPrice(String seatClass) {
        double basePrice;
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                basePrice = currentPriceBusiness != null ? currentPriceBusiness : basePriceBusiness;
                break;
            case "FIRST":
                basePrice = currentPriceFirstClass != null ? currentPriceFirstClass : basePriceFirstClass;
                break;
            default:
                basePrice = currentPriceEconomy != null ? currentPriceEconomy : basePriceEconomy;
        }

        // Dynamic pricing based on time until departure
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeparture = java.time.Duration.between(now, departureTime).toHours();

        if (hoursUntilDeparture <= 24 && hoursUntilDeparture > 0) {
            // Last 24 hours - 20% premium
            basePrice = basePrice * 1.20;
        } else if (hoursUntilDeparture <= 48 && hoursUntilDeparture > 24) {
            // 24-48 hours - 10% premium
            basePrice = basePrice * 1.10;
        } else if (hoursUntilDeparture <= 72 && hoursUntilDeparture > 48) {
            // 48-72 hours - 5% premium
            basePrice = basePrice * 1.05;
        }

        return Math.round(basePrice * 100.0) / 100.0;
    }

    @PrePersist
    protected void onCreate() {
        this.currentPriceEconomy = this.basePriceEconomy;
        this.currentPriceBusiness = this.basePriceBusiness;
        this.currentPriceFirstClass = this.basePriceFirstClass;
        this.originalDepartureTime = this.departureTime;
        this.originalArrivalTime = this.arrivalTime;
    }
}