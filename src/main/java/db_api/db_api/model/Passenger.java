package db_api.db_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "passengers")
@Data
public class Passenger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private String fullName;

    private Integer age;

    private String gender;

    private String passportNumber;

    private String nationality;

    @JsonIgnore
    @OneToOne(mappedBy = "passenger")
    private PassengerSeat passengerSeat;

    // Getters and Setters
}
