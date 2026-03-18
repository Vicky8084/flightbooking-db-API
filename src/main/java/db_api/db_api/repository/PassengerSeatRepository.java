package db_api.db_api.repository;


import db_api.db_api.enums.SeatClass;
import db_api.db_api.model.PassengerSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PassengerSeatRepository extends JpaRepository<PassengerSeat, Long> {

    // Check if seat is booked for flight
    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN true ELSE false END " +
            "FROM PassengerSeat ps WHERE ps.seat.id = :seatId " +
            "AND ps.bookingFlight.flight.id = :flightId")
    boolean isSeatBookedForFlight(@Param("seatId") Long seatId,
                                  @Param("flightId") Long flightId);

    // Count booked seats for flight
    @Query("SELECT COUNT(ps) FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId")
    Long countBookedSeatsForFlight(@Param("flightId") Long flightId);

    // ✅ **YEH METHOD ADD KARO - Booked seat IDs fetch karne ke liye**
    @Query("SELECT ps.seat.id FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId")
    List<Long> findBookedSeatIdsByFlightId(@Param("flightId") Long flightId);

    // Count booked seats by class
    @Query("SELECT COUNT(ps) FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId " +
            "AND ps.seat.seatClass = :seatClass")
    Long countBookedSeatsByClass(@Param("flightId") Long flightId,
                                 @Param("seatClass") SeatClass seatClass);
}
