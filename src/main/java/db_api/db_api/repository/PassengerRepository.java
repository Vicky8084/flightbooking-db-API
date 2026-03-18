package db_api.db_api.repository;


import db_api.db_api.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    // Find by booking ID
    List<Passenger> findByBookingId(Long bookingId);

    // Find by passport number
    List<Passenger> findByPassportNumber(String passportNumber);
}
