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

    /**
     * Get row number from seat number
     * Example: "1A" -> 1, "22D" -> 22, "151C" -> 151
     */
    public int getRowNumber() {
        String seatNum = this.seatNumber;
        // Extract numeric part from seat number
        String numericPart = seatNum.replaceAll("[^0-9]", "");
        return Integer.parseInt(numericPart);
    }

    /**
     * Get column letter from seat number
     * Example: "1A" -> "A", "22D" -> "D"
     */
    public String getColumnLetter() {
        String seatNum = this.seatNumber;
        // Extract letter part from seat number
        String letterPart = seatNum.replaceAll("[0-9]", "");
        return letterPart;
    }

    /**
     * Check if this is a window seat (already has seatType field, but this is helper)
     */
    public boolean isWindowSeat() {
        return this.seatType == SeatType.WINDOW;
    }

    /**
     * Check if this is an aisle seat
     */
    public boolean isAisleSeat() {
        return this.seatType == SeatType.AISLE;
    }

    /**
     * Check if this is a middle seat
     */
    public boolean isMiddleSeat() {
        return this.seatType == SeatType.MIDDLE;
    }

    /**
     * Get seat category based on row and seat type
     * Returns: "COMFORT", "LEGROOM", "STANDARD", "EXIT", "BUSINESS", "FIRST"
     */
    public String getSeatCategory() {
        int row = getRowNumber();

        // First Class seats (rows 241-260)
        if (this.seatClass == SeatClass.FIRST) {
            return "FIRST_CLASS";
        }

        // Business Class seats (rows 151-240)
        if (this.seatClass == SeatClass.BUSINESS) {
            return "BUSINESS_CLASS";
        }

        // Economy class categorization
        if (row == 1) {
            return "COMFORT_SEATS";
        } else if (row >= 2 && row <= 3) {
            return "LEGROOM_SEATS";
        } else if (row >= 5 && row <= 15) {
            return "STANDARD_SEATS";
        } else if (row >= 16 && row <= 17) {
            return "LEGROOM_SEATS";
        } else if (row >= 18 && row <= 33) {
            return "STANDARD_SEATS";
        } else if (row == 34) {
            return "EMERGENCY_EXIT";
        } else if (row >= 35 && row <= 150) {
            return "STANDARD_SEATS";
        }

        return "STANDARD_SEATS";
    }

    /**
     * Get display label for seat (e.g., "A", "B", "C")
     */
    public String getSeatLabel() {
        return getColumnLetter();
    }

    /**
     * Get CSS class for seat type (for frontend)
     */
    public String getSeatTypeClass() {
        switch (this.seatType) {
            case WINDOW:
                return "seat-window";
            case AISLE:
                return "seat-aisle";
            case MIDDLE:
                return "seat-middle";
            default:
                return "seat-standard";
        }
    }

    /**
     * Get premium/discount percentage (extraPrice as percentage)
     */
    public int getPremiumPercent() {
        if (this.extraPrice == null) return 0;
        // extraPrice can be like 0.15 (15% premium) or -0.05 (5% discount)
        return (int) (this.extraPrice * 100);
    }
}