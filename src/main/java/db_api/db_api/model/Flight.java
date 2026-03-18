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
    private String flightNumber; // e.g., "AI-101"

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

    private Integer duration; // in minutes

    @Column(nullable = false)
    private Double basePriceEconomy;

    private Double basePriceBusiness;

    private Double basePriceFirstClass;

    @Enumerated(EnumType.STRING)
    private FlightStatus status; // SCHEDULED, DELAYED, CANCELLED, COMPLETED

    private Integer availableEconomySeats;

    private Integer availableBusinessSeats;

    private Integer availableFirstClassSeats;

    @OneToMany(mappedBy = "parentFlight", cascade = CascadeType.ALL)  // ✅ SAHI
    private List<FlightSegment> segments;

    @JsonIgnore
    @OneToMany(mappedBy = "flight")
    private List<BookingFlight> bookingFlights;

    // Getters and Setters
}
