package db_api.db_api.repository;

import db_api.db_api.model.MealPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MealPreferenceRepository extends JpaRepository<MealPreference, Long> {

    List<MealPreference> findByBookingId(Long bookingId);

    List<MealPreference> findByFlightId(Long flightId);
}