package db_api.db_api.util;

import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import org.springframework.stereotype.Component;

@Component
public class SeatNumberGenerator {

    /**
     * Generate seat number using continuous numbering
     * Format: Number + Letter
     * Economy: 1A, 2B, 3C, 4D, 5E, 6F, 7A, 8B...
     * Business: 151A, 152B, 153C, 154D...
     * First: 201A, 202B, 203A, 204B...
     */

    private static final String[] ECONOMY_LETTERS = {"A", "B", "C", "D", "E", "F"};
    private static final String[] BUSINESS_LETTERS = {"A", "B", "C", "D"};
    private static final String[] FIRST_LETTERS = {"A", "B"};

    /**
     * Get seat letter based on position and class
     */
    public String getSeatLetter(int seatNumber, SeatClass seatClass) {
        int position = getPositionInCycle(seatNumber, seatClass);

        switch (seatClass) {
            case ECONOMY:
                return ECONOMY_LETTERS[position];
            case BUSINESS:
                return BUSINESS_LETTERS[position];
            case FIRST:
                return FIRST_LETTERS[position];
            default:
                return "A";
        }
    }

    /**
     * Get seat type based on seat number and class
     */
    public SeatType getSeatType(int seatNumber, SeatClass seatClass) {
        int position = getPositionInCycle(seatNumber, seatClass);

        switch (seatClass) {
            case ECONOMY:
                // 6-seat cycle: A(Window), B(Middle), C(Aisle), D(Aisle), E(Middle), F(Window)
                if (position == 0 || position == 5) return SeatType.WINDOW;
                if (position == 2 || position == 3) return SeatType.AISLE;
                return SeatType.MIDDLE;

            case BUSINESS:
                // 4-seat cycle: A(Window), B(Aisle), C(Aisle), D(Window)
                if (position == 0 || position == 3) return SeatType.WINDOW;
                return SeatType.AISLE;

            case FIRST:
                // 2-seat cycle: Both window seats
                return SeatType.WINDOW;

            default:
                return SeatType.MIDDLE;
        }
    }

    /**
     * Get position in cycle (0-based)
     * Economy: 6-seat cycle (0-5)
     * Business: 4-seat cycle (0-3)
     * First: 2-seat cycle (0-1)
     */
    private int getPositionInCycle(int seatNumber, SeatClass seatClass) {
        int cycleLength;
        switch (seatClass) {
            case ECONOMY:
                cycleLength = 6;
                break;
            case BUSINESS:
                cycleLength = 4;
                break;
            case FIRST:
                cycleLength = 2;
                break;
            default:
                cycleLength = 6;
        }
        return (seatNumber - 1) % cycleLength;
    }

    /**
     * Generate full seat number
     */
    public String generateSeatNumber(int seatNumber, SeatClass seatClass) {
        return seatNumber + getSeatLetter(seatNumber, seatClass);
    }
}