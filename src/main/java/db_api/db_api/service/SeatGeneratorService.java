package db_api.db_api.service;

import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import db_api.db_api.model.Aircraft;
import db_api.db_api.model.Seat;
import db_api.db_api.repository.SeatRepository;
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

    private static final int SEATS_PER_ROW_ECONOMY = 6;
    private static final int SEATS_PER_ROW_BUSINESS = 4;
    private static final int SEATS_PER_ROW_FIRST = 2;

    private static final String[] ECONOMY_COLUMNS = {"A", "B", "C", "D", "E", "F"};
    private static final String[] BUSINESS_COLUMNS = {"A", "B", "C", "D"};
    private static final String[] FIRST_COLUMNS = {"A", "B"};

    /**
     * Generate seats for aircraft with CLASS-SPECIFIC LAYOUT
     * First Class: 1+1 layout (only window seats)
     * Business Class: 2+2 layout (window + aisle)
     * Economy Class: 3+3 layout (window + middle + aisle)
     */
    @Transactional
    public List<Seat> generateSeatsForAircraft(Aircraft aircraft) {
        // Check existing seats
        List<Seat> existingSeats = seatRepository.findByAircraftId(aircraft.getId());
        if (!existingSeats.isEmpty()) {
            log.warn("Aircraft ID {} already has {} seats. Returning existing.",
                    aircraft.getId(), existingSeats.size());
            return existingSeats;
        }

        List<Seat> allSeats = new ArrayList<>();
        int currentRow = 1;

        // ========== 1. FIRST CLASS (Rows 1-2) ==========
        int firstClassSeats = aircraft.getFirstClassSeats() != null ? aircraft.getFirstClassSeats() : 0;
        if (firstClassSeats > 0) {
            int firstClassRows = (int) Math.ceil((double) firstClassSeats / SEATS_PER_ROW_FIRST);
            log.info("First Class: {} seats → {} rows (rows {}-{}), Layout: 1+1",
                    firstClassSeats, firstClassRows, currentRow, currentRow + firstClassRows - 1);

            for (int rowOffset = 0; rowOffset < firstClassRows; rowOffset++) {
                int rowNum = currentRow + rowOffset;
                int seatsInThisRow = Math.min(SEATS_PER_ROW_FIRST, firstClassSeats - (rowOffset * SEATS_PER_ROW_FIRST));

                for (int col = 0; col < seatsInThisRow; col++) {
                    String columnLetter = FIRST_COLUMNS[col];
                    String seatNumber = rowNum + columnLetter;

                    // First class: ALL seats are WINDOW type
                    SeatType seatType = SeatType.WINDOW;
                    boolean hasExtraLegroom = true;
                    boolean isNearExit = false;
                    double extraPrice = 0.20; // 20% premium

                    Seat seat = createSeat(aircraft, seatNumber, SeatClass.FIRST,
                            seatType, hasExtraLegroom, isNearExit, extraPrice);
                    allSeats.add(seat);
                }
            }
            currentRow += firstClassRows;
            log.info("First Class generated. Next row: {}", currentRow);
        }

        // ========== 2. BUSINESS CLASS (2+2 layout) ==========
        int businessSeats = aircraft.getBusinessSeats() != null ? aircraft.getBusinessSeats() : 0;
        if (businessSeats > 0) {
            int businessRows = (int) Math.ceil((double) businessSeats / SEATS_PER_ROW_BUSINESS);
            log.info("Business Class: {} seats → {} rows (rows {}-{}), Layout: 2+2",
                    businessSeats, businessRows, currentRow, currentRow + businessRows - 1);

            for (int rowOffset = 0; rowOffset < businessRows; rowOffset++) {
                int rowNum = currentRow + rowOffset;
                int seatsInThisRow = Math.min(SEATS_PER_ROW_BUSINESS, businessSeats - (rowOffset * SEATS_PER_ROW_BUSINESS));

                for (int col = 0; col < seatsInThisRow; col++) {
                    String columnLetter = BUSINESS_COLUMNS[col];
                    String seatNumber = rowNum + columnLetter;

                    // Business class seat type based on column
                    SeatType seatType;
                    if (columnLetter.equals("A") || columnLetter.equals("D")) {
                        seatType = SeatType.WINDOW;
                    } else {
                        seatType = SeatType.AISLE;
                    }

                    boolean hasExtraLegroom = (rowOffset == 0); // First row of business has legroom
                    boolean isNearExit = false;
                    double extraPrice = calculateBusinessExtraPrice(rowNum, seatType);

                    Seat seat = createSeat(aircraft, seatNumber, SeatClass.BUSINESS,
                            seatType, hasExtraLegroom, isNearExit, extraPrice);
                    allSeats.add(seat);
                }
            }
            currentRow += businessRows;
            log.info("Business Class generated. Next row: {}", currentRow);
        }

        // ========== 3. ECONOMY CLASS (3+3 layout) ==========
        int economySeats = aircraft.getEconomySeats() != null ? aircraft.getEconomySeats() : 0;
        if (economySeats > 0) {
            int economyRows = (int) Math.ceil((double) economySeats / SEATS_PER_ROW_ECONOMY);
            log.info("Economy Class: {} seats → {} rows (rows {}-{}), Layout: 3+3",
                    economySeats, economyRows, currentRow, currentRow + economyRows - 1);

            for (int rowOffset = 0; rowOffset < economyRows; rowOffset++) {
                int rowNum = currentRow + rowOffset;
                int seatsInThisRow = Math.min(SEATS_PER_ROW_ECONOMY, economySeats - (rowOffset * SEATS_PER_ROW_ECONOMY));

                for (int col = 0; col < seatsInThisRow; col++) {
                    String columnLetter = ECONOMY_COLUMNS[col];
                    String seatNumber = rowNum + columnLetter;

                    // Economy class seat type based on column
                    SeatType seatType = getEconomySeatType(columnLetter);
                    boolean hasExtraLegroom = isEconomyLegroomRow(rowOffset);
                    boolean isNearExit = isEconomyExitRow(rowOffset);
                    double extraPrice = calculateEconomyExtraPrice(rowNum, seatType, rowOffset);

                    Seat seat = createSeat(aircraft, seatNumber, SeatClass.ECONOMY,
                            seatType, hasExtraLegroom, isNearExit, extraPrice);
                    allSeats.add(seat);
                }
            }
        }

        log.info("✅ Generated {} seats for aircraft: {} (ID: {})",
                allSeats.size(), aircraft.getRegistrationNumber(), aircraft.getId());

        return seatRepository.saveAll(allSeats);
    }

    /**
     * Get seat type for economy class based on column letter
     * A,F = WINDOW | B,E = MIDDLE | C,D = AISLE
     */
    private SeatType getEconomySeatType(String columnLetter) {
        switch (columnLetter) {
            case "A": return SeatType.WINDOW;
            case "B": return SeatType.MIDDLE;
            case "C": return SeatType.AISLE;
            case "D": return SeatType.AISLE;
            case "E": return SeatType.MIDDLE;
            case "F": return SeatType.WINDOW;
            default: return SeatType.MIDDLE;
        }
    }

    /**
     * Check if economy row has extra legroom
     * First 2 rows of economy (rowOffset 0,1) = legroom
     * Rows after exit (rowOffset 9,10) = legroom
     */
    private boolean isEconomyLegroomRow(int positionInEconomy) {
        if (positionInEconomy == 0 || positionInEconomy == 1) {
            return true;
        }
        if (positionInEconomy == 9 || positionInEconomy == 10) {
            return true;
        }
        return false;
    }

    /**
     * Check if economy row is emergency exit row
     * Exit rows at position 9 and 10 (16th and 17th economy rows)
     */
    private boolean isEconomyExitRow(int positionInEconomy) {
        if (positionInEconomy == 9 || positionInEconomy == 10) {
            return true;
        }
        return false;
    }

    /**
     * Calculate extra price for economy seat
     */
    private double calculateEconomyExtraPrice(int rowNum, SeatType seatType, int positionInEconomy) {
        double price = 0.0;

        // First 2 economy rows (extra legroom)
        if (positionInEconomy == 0 || positionInEconomy == 1) {
            price = 0.15; // 15% premium
        }
        // Exit rows
        else if (positionInEconomy == 9 || positionInEconomy == 10) {
            price = 0.20; // 20% premium for exit rows
        }

        // Seat type adjustment
        if (seatType == SeatType.WINDOW) {
            price += 0.05; // +5% for window
        } else if (seatType == SeatType.MIDDLE) {
            price -= 0.05; // -5% discount for middle
        }

        return Math.round(price * 100.0) / 100.0;
    }

    /**
     * Calculate extra price for business seat
     */
    private double calculateBusinessExtraPrice(int rowNum, SeatType seatType) {
        double price = 0.10; // Base 10% premium for business

        if (seatType == SeatType.WINDOW) {
            price += 0.05; // Extra for window
        }

        return Math.round(price * 100.0) / 100.0;
    }

    /**
     * Create individual seat
     */
    private Seat createSeat(Aircraft aircraft, String seatNumber, SeatClass seatClass,
                            SeatType seatType, boolean hasExtraLegroom,
                            boolean isNearExit, double extraPrice) {
        Seat seat = new Seat();
        seat.setAircraft(aircraft);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass(seatClass);
        seat.setSeatType(seatType);
        seat.setHasExtraLegroom(hasExtraLegroom);
        seat.setIsNearExit(isNearExit);
        seat.setExtraPrice(extraPrice);
        seat.setIsActive(true);
        return seat;
    }
}