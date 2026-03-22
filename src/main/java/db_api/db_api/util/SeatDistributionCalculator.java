package db_api.db_api.util;

import db_api.db_api.enums.SeatClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SeatDistributionCalculator {

    /**
     * Calculate seat distribution for a given class
     */
    public SeatDistribution calculateDistribution(int totalSeats, SeatClass seatClass) {
        int windowSeats, aisleSeats, middleSeats;

        switch (seatClass) {
            case ECONOMY:
                // 3-3 configuration: 33.3% each
                windowSeats = (int) Math.round(totalSeats * 0.333);
                aisleSeats = (int) Math.round(totalSeats * 0.333);
                middleSeats = totalSeats - windowSeats - aisleSeats;

                // Ensure even numbers (since seats come in pairs)
                if (windowSeats % 2 != 0) windowSeats++;
                if (aisleSeats % 2 != 0) aisleSeats++;
                middleSeats = totalSeats - windowSeats - aisleSeats;
                break;

            case BUSINESS:
                // 2-2 configuration: 50% window, 50% aisle
                windowSeats = (int) Math.round(totalSeats * 0.5);
                aisleSeats = totalSeats - windowSeats;
                middleSeats = 0;

                // Ensure even numbers
                if (windowSeats % 2 != 0) {
                    windowSeats++;
                    aisleSeats--;
                }
                break;

            case FIRST:
                // 1-1 configuration: 100% window
                windowSeats = totalSeats;
                aisleSeats = 0;
                middleSeats = 0;
                break;

            default:
                windowSeats = totalSeats;
                aisleSeats = 0;
                middleSeats = 0;
        }

        return new SeatDistribution(windowSeats, aisleSeats, middleSeats);
    }

    /**
     * Calculate complete aircraft seat distribution
     */
    public AircraftSeatDistribution calculateAircraftDistribution(
            int economySeats, int businessSeats, int firstClassSeats) {

        SeatDistribution economy = calculateDistribution(economySeats, SeatClass.ECONOMY);
        SeatDistribution business = calculateDistribution(businessSeats, SeatClass.BUSINESS);
        SeatDistribution first = calculateDistribution(firstClassSeats, SeatClass.FIRST);

        return new AircraftSeatDistribution(economy, business, first);
    }

    /**
     * Auto-calculate distribution if admin doesn't specify
     */
    public Map<String, Integer> getAutoDistribution(int totalSeats, SeatClass seatClass) {
        SeatDistribution dist = calculateDistribution(totalSeats, seatClass);

        Map<String, Integer> result = new HashMap<>();
        result.put("window", dist.windowSeats);
        result.put("aisle", dist.aisleSeats);
        result.put("middle", dist.middleSeats);

        log.info("Auto-calculated {} distribution: {} window, {} aisle, {} middle for {} seats",
                seatClass, dist.windowSeats, dist.aisleSeats, dist.middleSeats, totalSeats);

        return result;
    }

    /**
     * Inner class for seat distribution
     */
    public static class SeatDistribution {
        public final int windowSeats;
        public final int aisleSeats;
        public final int middleSeats;

        public SeatDistribution(int windowSeats, int aisleSeats, int middleSeats) {
            this.windowSeats = windowSeats;
            this.aisleSeats = aisleSeats;
            this.middleSeats = middleSeats;
        }
    }

    /**
     * Inner class for complete aircraft distribution
     */
    public static class AircraftSeatDistribution {
        public final SeatDistribution economy;
        public final SeatDistribution business;
        public final SeatDistribution first;

        public AircraftSeatDistribution(SeatDistribution economy, SeatDistribution business, SeatDistribution first) {
            this.economy = economy;
            this.business = business;
            this.first = first;
        }

        public int getTotalSeats() {
            return economy.windowSeats + economy.aisleSeats + economy.middleSeats +
                    business.windowSeats + business.aisleSeats + business.middleSeats +
                    first.windowSeats + first.aisleSeats + first.middleSeats;
        }
    }
}