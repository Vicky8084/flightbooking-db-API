package db_api.db_api.repository;

import db_api.db_api.model.ExtraBaggage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExtraBaggageRepository extends JpaRepository<ExtraBaggage, Long> {

    List<ExtraBaggage> findByBookingId(Long bookingId);

    List<ExtraBaggage> findByPassengerId(Long passengerId);
}