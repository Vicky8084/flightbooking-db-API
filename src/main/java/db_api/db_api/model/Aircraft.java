package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

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

    private Integer economySeats;

    private Integer businessSeats;

    private Integer firstClassSeats;

    // ✅ FIXED: Changed from User to Airline
    @ManyToOne
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;  // Now references Airline, not User

    @JsonIgnore
    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL)
    private List<Flight> flights;

    @JsonIgnore
    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL)
    private List<Seat> seats;
}