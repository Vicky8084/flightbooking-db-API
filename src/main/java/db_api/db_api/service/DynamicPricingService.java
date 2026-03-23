package db_api.db_api.service;

import db_api.db_api.model.Flight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DynamicPricingService {

    /**
     * Calculate dynamic price for a flight
     */
    public double calculatePrice(Flight flight, String seatClass, int daysBeforeBooking) {
        double basePrice = getBasePrice(flight, seatClass);

        double timeMultiplier = calculateTimeMultiplier(daysBeforeBooking);
        double demandMultiplier = calculateDemandMultiplier(flight, seatClass);
        double dayMultiplier = calculateDayMultiplier(flight);

        double finalPrice = basePrice * timeMultiplier * demandMultiplier * dayMultiplier;

        log.debug("Dynamic pricing for flight {} - {}: Base={}, Time={}, Demand={}, Day={}, Final={}",
                flight.getFlightNumber(), seatClass, basePrice, timeMultiplier, demandMultiplier, dayMultiplier, finalPrice);

        return Math.round(finalPrice);
    }

    private double getBasePrice(Flight flight, String seatClass) {
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                return flight.getBasePriceBusiness();
            case "FIRST":
                return flight.getBasePriceFirstClass();
            default:
                return flight.getBasePriceEconomy();
        }
    }

    private double calculateTimeMultiplier(int daysBefore) {
        if (daysBefore > 30) {
            return 0.85;  // 15% off - Early bird
        } else if (daysBefore > 15) {
            return 1.0;   // Normal price
        } else if (daysBefore > 7) {
            return 1.1;   // 10% premium
        } else if (daysBefore > 3) {
            return 1.2;   // 20% premium
        } else if (daysBefore > 0) {
            return 1.5;   // 50% premium - Last minute
        }
        return 1.0;
    }

    private double calculateDemandMultiplier(Flight flight, String seatClass) {
        double occupancy = flight.getOccupancyPercentage(seatClass);

        if (occupancy >= 0.8) {
            return 1.3;   // 30% premium - High demand
        } else if (occupancy >= 0.6) {
            return 1.15;  // 15% premium - Medium demand
        } else if (occupancy >= 0.4) {
            return 1.05;  // 5% premium - Low demand
        }
        return 1.0;
    }

    private double calculateDayMultiplier(Flight flight) {
        int dayOfWeek = flight.getDepartureTime().getDayOfWeek().getValue();

        // Saturday (6) or Sunday (7)
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return 1.2;   // 20% premium - Weekend
        }
        // Tuesday (2) or Wednesday (3)
        if (dayOfWeek == 2 || dayOfWeek == 3) {
            return 0.9;   // 10% discount - Midweek
        }
        return 1.0;
    }
}