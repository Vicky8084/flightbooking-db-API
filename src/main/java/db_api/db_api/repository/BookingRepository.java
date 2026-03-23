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

    // ✅ PNR is unique and indexed
    Optional<Booking> findByPnrNumber(String pnrNumber);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    boolean existsByPnrNumber(String pnrNumber);

    @Query("SELECT b FROM Booking b WHERE b.bookingTime BETWEEN :start AND :end")
    List<Booking> findByDateRange(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Booking b JOIN b.bookingFlights bf WHERE bf.flight.id = :flightId")
    List<Booking> findByFlightId(@Param("flightId") Long flightId);

    // ✅ Count bookings by status for dashboard
    long countByStatus(BookingStatus status);

    // ✅ Find bookings by user with pagination
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.bookingTime DESC")
    List<Booking> findRecentByUserId(@Param("userId") Long userId);
}