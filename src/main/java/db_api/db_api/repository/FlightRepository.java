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

    // Find by source and destination
    List<Flight> findBySourceAirportCodeAndDestinationAirportCodeAndStatus(
            String sourceCode, String destinationCode, FlightStatus status);

    // Find by source and departure time range
    List<Flight> findBySourceAirportCodeAndDepartureTimeBetween(
            String sourceCode, LocalDateTime start, LocalDateTime end);

    // ✅ **YEH METHOD ADD KARO - Airline ID se flights find karne ke liye**
    @Query("SELECT f FROM Flight f WHERE f.aircraft.airline.id = :airlineId")
    List<Flight> findByAircraftAirlineId(@Param("airlineId") Long airlineId);

    // Find by status
    List<Flight> findByStatus(FlightStatus status);

    // Find direct flights
    @Query("SELECT f FROM Flight f WHERE f.sourceAirport.code = :sourceCode " +
            "AND f.destinationAirport.code = :destCode " +
            "AND DATE(f.departureTime) = DATE(:departureTime) " +
            "AND f.status = 'SCHEDULED'")
    List<Flight> findDirectFlights(@Param("sourceCode") String sourceCode,
                                   @Param("destCode") String destCode,
                                   @Param("departureTime") LocalDateTime departureTime);

    // FlightRepository.java mein ye method add karo
    boolean existsByFlightNumber(String flightNumber);
}
