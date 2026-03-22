package db_api.db_api.util;

import db_api.db_api.enums.SeatClass;
import db_api.db_api.exception.BookingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AircraftSeatValidator {

    /**
     * Validate seat distribution for aircraft
     */
    public void validateSeatDistribution(int totalSeats, int economySeats, int businessSeats, int firstClassSeats)
            throws BookingException {

        int sum = economySeats + businessSeats + firstClassSeats;
        if (sum != totalSeats) {
            throw new BookingException(
                    String.format("Seat sum mismatch: %d + %d + %d = %d, but total seats = %d",
                            economySeats, businessSeats, firstClassSeats, sum, totalSeats)
            );
        }

        // Validate based on total seats
        if (totalSeats < 50) {
            validateSmallAircraft(economySeats, businessSeats, firstClassSeats);
        } else if (totalSeats <= 200) {
            validateNarrowBodyAircraft(economySeats, businessSeats, firstClassSeats);
        } else if (totalSeats <= 350) {
            validateWideBodyAircraft(economySeats, businessSeats, firstClassSeats);
        } else {
            validateLargeAircraft(economySeats, businessSeats, firstClassSeats);
        }

        log.info("✅ Seat distribution validated: Eco={}, Bus={}, First={}, Total={}",
                economySeats, businessSeats, firstClassSeats, totalSeats);
    }

    private void validateSmallAircraft(int economy, int business, int first) throws BookingException {
        double ecoPercent = (double) economy / (economy + business + first) * 100;

        if (ecoPercent < 70) {
            throw new BookingException(
                    "For small aircraft (50-150 seats), economy seats must be at least 70% of total seats. " +
                            "Current: " + String.format("%.1f", ecoPercent) + "%"
            );
        }

        if (first > 0) {
            throw new BookingException("First class is not available for small aircraft (under 150 seats)");
        }
    }

    private void validateNarrowBodyAircraft(int economy, int business, int first) throws BookingException {
        double ecoPercent = (double) economy / (economy + business + first) * 100;

        if (ecoPercent < 75) {
            throw new BookingException(
                    "For narrow body aircraft (150-200 seats), economy seats must be at least 75% of total seats. " +
                            "Current: " + String.format("%.1f", ecoPercent) + "%"
            );
        }

        if (first > 10) {
            throw new BookingException("First class cannot exceed 10 seats on narrow body aircraft. Current: " + first);
        }
    }

    private void validateWideBodyAircraft(int economy, int business, int first) throws BookingException {
        double ecoPercent = (double) economy / (economy + business + first) * 100;
        double busPercent = (double) business / (economy + business + first) * 100;

        if (ecoPercent < 70 || ecoPercent > 85) {
            throw new BookingException(
                    "For wide body aircraft (200-350 seats), economy seats should be between 70% and 85%. " +
                            "Current: " + String.format("%.1f", ecoPercent) + "%"
            );
        }

        if (busPercent < 10 || busPercent > 25) {
            throw new BookingException(
                    "For wide body aircraft, business class should be between 10% and 25%. " +
                            "Current: " + String.format("%.1f", busPercent) + "%"
            );
        }
    }

    private void validateLargeAircraft(int economy, int business, int first) throws BookingException {
        double ecoPercent = (double) economy / (economy + business + first) * 100;
        double busPercent = (double) business / (economy + business + first) * 100;

        if (ecoPercent < 65 || ecoPercent > 80) {
            throw new BookingException(
                    "For large aircraft (350+ seats), economy seats should be between 65% and 80%. " +
                            "Current: " + String.format("%.1f", ecoPercent) + "%"
            );
        }

        if (busPercent < 15 || busPercent > 25) {
            throw new BookingException(
                    "For large aircraft, business class should be between 15% and 25%. " +
                            "Current: " + String.format("%.1f", busPercent) + "%"
            );
        }
    }

    /**
     * ✅ FIXED: Validate seat type distribution within class
     * First Class can have 100% window seats (1-1 configuration)
     * Business can have 50% window, 50% aisle (2-2 configuration)
     * Economy can have 33% each (3-3 configuration)
     */
    public void validateSeatTypeDistribution(int totalSeats, int windowSeats, int aisleSeats, int middleSeats,
                                             String className, SeatClass seatClass) throws BookingException {

        // Check sum
        int sum = windowSeats + aisleSeats + middleSeats;
        if (sum != totalSeats) {
            throw new BookingException(
                    String.format("%s seat type sum mismatch: %d + %d + %d = %d, but total = %d",
                            className, windowSeats, aisleSeats, middleSeats, sum, totalSeats)
            );
        }

        // ✅ Class-specific validation
        switch (seatClass) {
            case FIRST:
                // First Class: 1-1 configuration → 100% window seats
                if (windowSeats != totalSeats) {
                    throw new BookingException(
                            String.format("First Class must have 100%% window seats (1-1 configuration). " +
                                    "Current: %d window seats out of %d total", windowSeats, totalSeats)
                    );
                }
                if (aisleSeats != 0 || middleSeats != 0) {
                    throw new BookingException("First Class cannot have aisle or middle seats");
                }
                break;

            case BUSINESS:
                // Business Class: 2-2 configuration → 50% window, 50% aisle
                if (windowSeats != totalSeats / 2 || aisleSeats != totalSeats - windowSeats) {
                    // Allow small variations due to odd numbers
                    int expectedWindow = totalSeats / 2;
                    int expectedAisle = totalSeats - expectedWindow;
                    if (Math.abs(windowSeats - expectedWindow) > 1) {
                        throw new BookingException(
                                String.format("Business Class should have approximately 50%% window seats. " +
                                                "Current: %d window, %d aisle for %d total seats",
                                        windowSeats, aisleSeats, totalSeats)
                        );
                    }
                }
                if (middleSeats != 0) {
                    throw new BookingException("Business Class cannot have middle seats (2-2 configuration)");
                }
                break;

            case ECONOMY:
                // Economy Class: 3-3 configuration → ~33% each
                double windowPercent = (double) windowSeats / totalSeats * 100;
                double aislePercent = (double) aisleSeats / totalSeats * 100;
                double middlePercent = (double) middleSeats / totalSeats * 100;

                if (windowPercent < 30 || windowPercent > 36) {
                    throw new BookingException(
                            String.format("Economy window seats should be around 33%% of total. " +
                                    "Current: %.1f%% (%d/%d)", windowPercent, windowSeats, totalSeats)
                    );
                }
                if (aislePercent < 30 || aislePercent > 36) {
                    throw new BookingException(
                            String.format("Economy aisle seats should be around 33%% of total. " +
                                    "Current: %.1f%% (%d/%d)", aislePercent, aisleSeats, totalSeats)
                    );
                }
                break;
        }

        log.debug("✅ {} seat type distribution: Window={}, Aisle={}, Middle={} ({} config)",
                className, windowSeats, aisleSeats, middleSeats, seatClass);
    }
}