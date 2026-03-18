package db_api.db_api.repository;


import db_api.db_api.model.Flight;
import db_api.db_api.model.FlightSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FlightSegmentRepository extends JpaRepository<FlightSegment, Long> {

    // Find all segments for a parent flight (connecting flight)
    List<FlightSegment> findByParentFlightId(Long parentFlightId);

    // Find all segment flights for a parent flight
    @Query("SELECT fs.segmentFlight FROM FlightSegment fs WHERE fs.parentFlight.id = :parentFlightId ORDER BY fs.sequenceOrder")
    List<Flight> findSegmentFlightsByParentFlightId(@Param("parentFlightId") Long parentFlightId);

    // Find parent flight by a segment flight
    @Query("SELECT fs.parentFlight FROM FlightSegment fs WHERE fs.segmentFlight.id = :segmentFlightId")
    List<Flight> findParentFlightsBySegmentFlightId(@Param("segmentFlightId") Long segmentFlightId);

    // Check if a flight is a segment of any connecting flight
    @Query("SELECT CASE WHEN COUNT(fs) > 0 THEN true ELSE false END FROM FlightSegment fs WHERE fs.segmentFlight.id = :flightId")
    boolean isFlightSegment(@Param("flightId") Long flightId);

    // Get layover duration between two specific flights in a connection
    @Query("SELECT fs.layoverDuration FROM FlightSegment fs WHERE fs.parentFlight.id = :parentFlightId AND fs.sequenceOrder = :order")
    Integer getLayoverDuration(@Param("parentFlightId") Long parentFlightId, @Param("order") Integer order);

    // Delete all segments for a parent flight
    void deleteByParentFlightId(Long parentFlightId);
}
