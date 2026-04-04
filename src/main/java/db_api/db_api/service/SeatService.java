package db_api.db_api.service;

import db_api.db_api.dto.SeatWithRowInfo;
import db_api.db_api.enums.SeatClass;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Aircraft;
import db_api.db_api.model.Seat;
import db_api.db_api.repository.AircraftRepository;
import db_api.db_api.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SeatService {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    public Seat createSeat(Seat seat) throws BookingException {
        // Verify aircraft exists
        Aircraft aircraft = aircraftRepository.findById(seat.getAircraft().getId())
                .orElseThrow(() -> new BookingException("Aircraft not found with ID: " + seat.getAircraft().getId()));

        seat.setAircraft(aircraft);

        return seatRepository.save(seat);
    }

    public List<Seat> createSeatsBulk(Long aircraftId, List<Seat> seats) throws BookingException {
        Aircraft aircraft = aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new BookingException("Aircraft not found with ID: " + aircraftId));

        for (Seat seat : seats) {
            seat.setAircraft(aircraft);
        }

        return seatRepository.saveAll(seats);
    }

    public Seat getSeatById(Long id) throws BookingException {
        return seatRepository.findById(id)
                .orElseThrow(() -> new BookingException("Seat not found with ID: " + id));
    }

    public List<Seat> getSeatsByAircraft(Long aircraftId) {
        return seatRepository.findByAircraftId(aircraftId);
    }

    public List<Seat> getSeatsByAircraftAndClass(Long aircraftId, String seatClass) throws BookingException {
        try {
            SeatClass enumClass = SeatClass.valueOf(seatClass.toUpperCase());
            return seatRepository.findByAircraftIdAndSeatClass(aircraftId, enumClass);
        } catch (IllegalArgumentException e) {
            throw new BookingException("Invalid seat class: " + seatClass);
        }
    }

    public Seat updateSeat(Long id, Seat seatDetails) throws BookingException {
        Seat seat = getSeatById(id);

        seat.setSeatType(seatDetails.getSeatType());
        seat.setHasExtraLegroom(seatDetails.getHasExtraLegroom());
        seat.setIsNearExit(seatDetails.getIsNearExit());
        seat.setExtraPrice(seatDetails.getExtraPrice());
        seat.setIsActive(seatDetails.getIsActive());

        return seatRepository.save(seat);
    }

    public void deleteSeat(Long id) throws BookingException {
        Seat seat = getSeatById(id);
        seatRepository.delete(seat);
    }

    /**
     * Get seats grouped by row with full information
     * Returns Map where key = row number, value = list of seats in that row
     */
    public Map<Integer, List<SeatWithRowInfo>> getSeatsGroupedByRow(Long aircraftId) throws BookingException {
        List<Seat> seats = getSeatsByAircraft(aircraftId);
        Map<Integer, List<SeatWithRowInfo>> rowsMap = new TreeMap<>();

        for (Seat seat : seats) {
            SeatWithRowInfo seatInfo = new SeatWithRowInfo(seat);
            int rowNum = seatInfo.getRowNumber();

            rowsMap.putIfAbsent(rowNum, new ArrayList<>());
            rowsMap.get(rowNum).add(seatInfo);
        }

        // Sort seats within each row by column letter (A, B, C, D...)
        for (Map.Entry<Integer, List<SeatWithRowInfo>> entry : rowsMap.entrySet()) {
            entry.getValue().sort((s1, s2) ->
                    s1.getColumnLetter().compareTo(s2.getColumnLetter())
            );
        }

        return rowsMap;
    }

    public Map<String, Object> getSeatMapWithCategories(Long aircraftId) throws BookingException {
        Map<Integer, List<SeatWithRowInfo>> rowsMap = getSeatsGroupedByRow(aircraftId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aircraftId", aircraftId);
        result.put("totalSeats", getSeatsByAircraft(aircraftId).size());
        result.put("rows", rowsMap);

        // ✅ DYNAMIC: Build categories based on actual seat data
        Map<String, List<Integer>> categories = new LinkedHashMap<>();

        // Get all row numbers
        List<Integer> allRows = new ArrayList<>(rowsMap.keySet());
        Collections.sort(allRows);

        if (!allRows.isEmpty()) {
            // Find first row (COMFORT_SEATS)
            categories.put("COMFORT_SEATS", Arrays.asList(allRows.get(0)));

            // Find rows with extra legroom (hasExtraLegroom = true)
            List<Integer> legroomRows = new ArrayList<>();
            List<Integer> exitRows = new ArrayList<>();
            List<Integer> standardRows = new ArrayList<>();

            for (Map.Entry<Integer, List<SeatWithRowInfo>> entry : rowsMap.entrySet()) {
                int rowNum = entry.getKey();
                List<SeatWithRowInfo> seatsInRow = entry.getValue();

                // Check if any seat in this row has extra legroom
                boolean hasLegroom = seatsInRow.stream().anyMatch(SeatWithRowInfo::getHasExtraLegroom);
                // Check if any seat is near exit
                boolean isExit = seatsInRow.stream().anyMatch(SeatWithRowInfo::getIsNearExit);

                if (isExit) {
                    exitRows.add(rowNum);
                } else if (hasLegroom) {
                    legroomRows.add(rowNum);
                } else if (rowNum != allRows.get(0)) { // Skip first row
                    standardRows.add(rowNum);
                }
            }

            if (!legroomRows.isEmpty()) {
                categories.put("LEGROOM_SEATS", legroomRows);
            }
            if (!exitRows.isEmpty()) {
                categories.put("EMERGENCY_EXIT", exitRows);
            }
            if (!standardRows.isEmpty()) {
                categories.put("STANDARD_SEATS", standardRows);
            }
        }

        // ✅ Business and First Class detection based on seatClass
        List<Integer> businessRows = new ArrayList<>();
        List<Integer> firstClassRows = new ArrayList<>();

        for (Map.Entry<Integer, List<SeatWithRowInfo>> entry : rowsMap.entrySet()) {
            int rowNum = entry.getKey();
            List<SeatWithRowInfo> seatsInRow = entry.getValue();

            if (seatsInRow.isEmpty()) continue;

            String seatClass = seatsInRow.get(0).getSeatClass();

            if ("BUSINESS".equals(seatClass)) {
                businessRows.add(rowNum);
            } else if ("FIRST".equals(seatClass)) {
                firstClassRows.add(rowNum);
            }
        }

        if (!businessRows.isEmpty()) {
            categories.put("BUSINESS_CLASS", businessRows);
        }
        if (!firstClassRows.isEmpty()) {
            categories.put("FIRST_CLASS", firstClassRows);
        }

        result.put("categories", categories);

        return result;
    }
}