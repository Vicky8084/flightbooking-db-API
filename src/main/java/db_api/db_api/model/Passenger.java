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

    // ========== NEW FIELDS ==========
    private String passengerType;  // ADULT, CHILD, INFANT
    private String mealPreference;  // VEG, NON_VEG, JAL, KOSHER, HALAL
    private String specialRequests;  // Wheelchair, Medical assistance, etc.

    // Baggage tracking per passenger
    private Integer extraBaggageKg = 0;
    private Double extraBaggagePrice = 0.0;

    @JsonIgnore
    @OneToOne(mappedBy = "passenger")
    private PassengerSeat passengerSeat;

    @JsonIgnore
    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL)
    private java.util.List<ExtraBaggage> extraBaggages;

    @JsonIgnore
    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL)
    private java.util.List<MealPreference> mealPreferences;
}