package db_api.db_api.repository;


import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import db_api.db_api.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    // ✅ **YEH METHOD ADD KARO - Aircraft ID se seats find karne ke liye**
    List<Seat> findByAircraftId(Long aircraftId);

    // Filter by class
    List<Seat> findByAircraftIdAndSeatClass(Long aircraftId, SeatClass seatClass);

    // Filter by type
    List<Seat> findByAircraftIdAndSeatType(Long aircraftId, SeatType seatType);

    // Filter by extra legroom
    List<Seat> findByAircraftIdAndHasExtraLegroomTrue(Long aircraftId);

    // Find available seats for flight (alternative approach)
    @Query("SELECT s FROM Seat s WHERE s.aircraft.id = :aircraftId " +
            "AND s.id NOT IN (SELECT ps.seat.id FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId)")
    List<Seat> findAvailableSeatsForFlight(@Param("aircraftId") Long aircraftId,
                                           @Param("flightId") Long flightId);
}
