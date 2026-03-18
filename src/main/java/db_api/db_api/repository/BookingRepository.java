package db_api.db_api.repository;


import db_api.db_api.model.Booking;
import db_api.db_api.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find by PNR (unique)
    Optional<Booking> findByPnrNumber(String pnrNumber);

    // Find all bookings by user ID
    List<Booking> findByUserId(Long userId);

    // Find bookings by user ID and status
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    // Check if PNR exists
    boolean existsByPnrNumber(String pnrNumber);

    // Find bookings by date range
    @Query("SELECT b FROM Booking b WHERE b.bookingTime BETWEEN :start AND :end")
    List<Booking> findByDateRange(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    // Find bookings by flight ID
    @Query("SELECT b FROM Booking b JOIN b.bookingFlights bf WHERE bf.flight.id = :flightId")
    List<Booking> findByFlightId(@Param("flightId") Long flightId);
}
