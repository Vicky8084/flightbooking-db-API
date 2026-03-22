package db_api.db_api.model;

import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"aircraft_id", "seat_number"})
})
@Data
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @Column(nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    private Boolean hasExtraLegroom = false;
    private Boolean isNearExit = false;
    private Double extraPrice = 0.0;
    private Boolean isActive = true;

    /**
     * Calculate final seat price for a flight
     * @param flightBasePrice Base price from flight for this seat class
     * @return Final price for this seat on the flight
     */
    public double calculateFinalPrice(double flightBasePrice) {
        double finalPrice = flightBasePrice;

        // Add seat type premium/discount (extraPrice is already calculated as factor or fixed amount)
        // Since extraPrice might be a factor (0.15) or fixed amount (1000), we need to handle both
        if (extraPrice != null) {
            // If extraPrice is less than 100, treat as percentage factor
            if (extraPrice < 100 && extraPrice > -100) {
                finalPrice += flightBasePrice * extraPrice;
            } else {
                finalPrice += extraPrice;
            }
        }

        return Math.round(finalPrice * 100.0) / 100.0;
    }
}