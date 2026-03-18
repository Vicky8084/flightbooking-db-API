package db_api.db_api.service;

import db_api.db_api.enums.SeatClass;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Aircraft;
import db_api.db_api.model.Seat;
import db_api.db_api.repository.AircraftRepository;
import db_api.db_api.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}