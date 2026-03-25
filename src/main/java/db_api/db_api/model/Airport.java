package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "airports")
@Data
public class Airport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code; // e.g., "DEL", "BOM"

    @Column(nullable = false)
    private String name; // e.g., "Indira Gandhi International Airport"

    @Column(nullable = false)
    private String city;

    private String country;

    private String timezone;

    private Boolean isHub = false;
    private Integer hubPriority = 0;

    @JsonIgnore
    @OneToMany(mappedBy = "sourceAirport")
    private List<Flight> departingFlights;

    @JsonIgnore
    @OneToMany(mappedBy = "destinationAirport")
    private List<Flight> arrivingFlights;

    // Getters and Setters
}
