package db_api.db_api.repository;

import db_api.db_api.enums.FlightStatus;
import db_api.db_api.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    // ✅ Indexed queries
    List<Flight> findBySourceAirportCodeAndDestinationAirportCodeAndStatus(
            String sourceCode, String destinationCode, FlightStatus status);

    List<Flight> findBySourceAirportCodeAndDepartureTimeBetween(
            String sourceCode, LocalDateTime start, LocalDateTime end);

    @Query("SELECT f FROM Flight f WHERE f.aircraft.airline.id = :airlineId")
    List<Flight> findByAircraftAirlineId(@Param("airlineId") Long airlineId);

    List<Flight> findByStatus(FlightStatus status);

    @Query("SELECT f FROM Flight f WHERE f.sourceAirport.code = :sourceCode " +
            "AND f.destinationAirport.code = :destCode " +
            "AND DATE(f.departureTime) = DATE(:departureTime) " +
            "AND f.status = 'SCHEDULED'")
    List<Flight> findDirectFlights(@Param("sourceCode") String sourceCode,
                                   @Param("destCode") String destCode,
                                   @Param("departureTime") LocalDateTime departureTime);

    boolean existsByFlightNumber(String flightNumber);

    // ✅ New method for finding flights by departure time range with index
    @Query("SELECT f FROM Flight f WHERE f.departureTime BETWEEN :start AND :end")
    List<Flight> findByDepartureTimeBetween(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}