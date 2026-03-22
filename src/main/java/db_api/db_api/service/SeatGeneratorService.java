package db_api.db_api.service;

import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import db_api.db_api.model.Aircraft;
import db_api.db_api.model.Seat;
import db_api.db_api.repository.SeatRepository;
import db_api.db_api.util.SeatNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatGeneratorService {

    private final SeatRepository seatRepository;
    private final SeatNumberGenerator seatNumberGenerator;

    /**
     * Generate seats for a new aircraft
     * Each aircraft gets its own seat numbering starting from 1
     */
    @Transactional
    public List<Seat> generateSeatsForAircraft(Aircraft aircraft) {
        // Check if seats already exist for this aircraft
        List<Seat> existingSeats = seatRepository.findByAircraftId(aircraft.getId());
        if (!existingSeats.isEmpty()) {
            log.warn("Aircraft ID {} already has {} seats. Returning existing.",
                    aircraft.getId(), existingSeats.size());
            return existingSeats;
        }

        List<Seat> allSeats = new ArrayList<>();
        int currentSeatNumber = 1;

        // Generate Economy seats
        if (aircraft.getEconomySeats() != null && aircraft.getEconomySeats() > 0) {
            for (int i = 1; i <= aircraft.getEconomySeats(); i++) {
                Seat seat = createSeat(aircraft, SeatClass.ECONOMY, currentSeatNumber);
                allSeats.add(seat);
                currentSeatNumber++;
            }
        }

        // Generate Business seats
        if (aircraft.getBusinessSeats() != null && aircraft.getBusinessSeats() > 0) {
            for (int i = 1; i <= aircraft.getBusinessSeats(); i++) {
                Seat seat = createSeat(aircraft, SeatClass.BUSINESS, currentSeatNumber);
                allSeats.add(seat);
                currentSeatNumber++;
            }
        }

        // Generate First Class seats
        if (aircraft.getFirstClassSeats() != null && aircraft.getFirstClassSeats() > 0) {
            for (int i = 1; i <= aircraft.getFirstClassSeats(); i++) {
                Seat seat = createSeat(aircraft, SeatClass.FIRST, currentSeatNumber);
                allSeats.add(seat);
                currentSeatNumber++;
            }
        }

        log.info("✅ Generated {} seats for aircraft: {} (ID: {})",
                allSeats.size(), aircraft.getRegistrationNumber(), aircraft.getId());

        // Add premium seats (extra legroom, exit row)
        addPremiumSeats(allSeats, aircraft);

        return seatRepository.saveAll(allSeats);
    }

    /**
     * Create a single seat with extra price based on seat type
     */
    private Seat createSeat(Aircraft aircraft, SeatClass seatClass, int seatNumber) {
        Seat seat = new Seat();
        seat.setAircraft(aircraft);
        seat.setSeatClass(seatClass);
        seat.setSeatNumber(seatNumberGenerator.generateSeatNumber(seatNumber, seatClass));
        seat.setSeatType(seatNumberGenerator.getSeatType(seatNumber, seatClass));
        seat.setIsActive(true);
        seat.setHasExtraLegroom(false);
        seat.setIsNearExit(false);

        // Calculate extra price based on seat type (stored as factor or fixed amount)
        double extraPrice = calculateSeatExtraPrice(seat.getSeatType(), aircraft);
        seat.setExtraPrice(extraPrice);

        return seat;
    }

    /**
     * Calculate extra price based on seat type
     * Returns percentage factor for window/aisle/middle, returns fixed amount for premium features
     */
    private double calculateSeatExtraPrice(SeatType seatType, Aircraft aircraft) {
        switch (seatType) {
            case WINDOW:
                // Return as factor (0.15 = 15% premium)
                return aircraft.getWindowSeatPremiumPercent() / 100.0;
            case AISLE:
                // Return as factor (0.10 = 10% premium)
                return aircraft.getAisleSeatPremiumPercent() / 100.0;
            case MIDDLE:
                // Return as factor (-0.05 = 5% discount)
                return -aircraft.getMiddleSeatDiscountPercent() / 100.0;
            default:
                return 0;
        }
    }

    /**
     * Add premium features to seats (extra legroom, exit row)
     */
    private void addPremiumSeats(List<Seat> seats, Aircraft aircraft) {
        if (seats.isEmpty()) return;

        // Add extra legroom to first 10% of seats (min 2)
        int extraLegroomCount = Math.max(2, seats.size() / 10);
        for (int i = 0; i < extraLegroomCount && i < seats.size(); i++) {
            seats.get(i).setHasExtraLegroom(true);
            // Add fixed premium amount to extra price
            double currentExtra = seats.get(i).getExtraPrice();
            seats.get(i).setExtraPrice(currentExtra + aircraft.getExtraLegroomPremium());
        }

        // Add exit row seats to first 4 seats (or 5% of seats)
        int exitRowCount = Math.min(4, seats.size() / 8);
        for (int i = 0; i < exitRowCount && i < seats.size(); i++) {
            seats.get(i).setIsNearExit(true);
            double currentExtra = seats.get(i).getExtraPrice();
            seats.get(i).setExtraPrice(currentExtra + aircraft.getExitRowPremium());
        }
    }
}