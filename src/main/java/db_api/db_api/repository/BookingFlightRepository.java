package db_api.db_api.repository;

import db_api.db_api.model.BookingFlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingFlightRepository extends JpaRepository<BookingFlight, Long> {
}