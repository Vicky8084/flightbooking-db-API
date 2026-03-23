package db_api.db_api.repository;

import db_api.db_api.enums.SeatClass;
import db_api.db_api.model.PassengerSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PassengerSeatRepository extends JpaRepository<PassengerSeat, Long> {

    // ✅ Check if seat is booked for flight with lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN true ELSE false END " +
            "FROM PassengerSeat ps WHERE ps.seat.id = :seatId " +
            "AND ps.bookingFlight.flight.id = :flightId")
    boolean isSeatBookedForFlight(@Param("seatId") Long seatId,
                                  @Param("flightId") Long flightId);

    // Count booked seats for flight
    @Query("SELECT COUNT(ps) FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId")
    Long countBookedSeatsForFlight(@Param("flightId") Long flightId);

    // ✅ Get booked seat IDs with lock for race condition prevention
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps.seat.id FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId")
    List<Long> findBookedSeatIdsByFlightIdWithLock(@Param("flightId") Long flightId);

    // Get booked seat IDs without lock (for display only)
    @Query("SELECT ps.seat.id FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId")
    List<Long> findBookedSeatIdsByFlightId(@Param("flightId") Long flightId);

    // Count booked seats by class
    @Query("SELECT COUNT(ps) FROM PassengerSeat ps " +
            "WHERE ps.bookingFlight.flight.id = :flightId " +
            "AND ps.seat.seatClass = :seatClass")
    Long countBookedSeatsByClass(@Param("flightId") Long flightId,
                                 @Param("seatClass") SeatClass seatClass);

    // ✅ Check if passenger already has a seat for this flight
    @Query("SELECT CASE WHEN COUNT(ps) > 0 THEN true ELSE false END " +
            "FROM PassengerSeat ps WHERE ps.passenger.id = :passengerId " +
            "AND ps.bookingFlight.flight.id = :flightId")
    boolean isPassengerBookedOnFlight(@Param("passengerId") Long passengerId,
                                      @Param("flightId") Long flightId);
}