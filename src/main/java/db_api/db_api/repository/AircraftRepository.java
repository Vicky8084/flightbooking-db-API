package db_api.db_api.repository;

import db_api.db_api.model.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {

    // Find by registration number
    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);

    // Find all aircrafts belonging to an airline
    List<Aircraft> findByAirlineId(Long airlineId);

    // Find by model
    List<Aircraft> findByModel(String model);

    // Find by manufacturer
    List<Aircraft> findByManufacturer(String manufacturer);

    // Find aircrafts with minimum seat capacity
    @Query("SELECT a FROM Aircraft a WHERE a.totalSeats >= :minSeats")
    List<Aircraft> findByMinSeats(@Param("minSeats") Integer minSeats);

    // Get total seats by airline
    @Query("SELECT SUM(a.totalSeats) FROM Aircraft a WHERE a.airline.id = :airlineId")
    Integer getTotalSeatsByAirline(@Param("airlineId") Long airlineId);

    // Get count of aircrafts by airline
    @Query("SELECT COUNT(a) FROM Aircraft a WHERE a.airline.id = :airlineId")
    Long countByAirlineId(@Param("airlineId") Long airlineId);

    // Find aircrafts with available economy seats > threshold
    @Query("SELECT a FROM Aircraft a WHERE a.economySeats > :threshold")
    List<Aircraft> findByEconomySeatsGreaterThan(@Param("threshold") Integer threshold);

    // Check if registration number exists
    boolean existsByRegistrationNumber(String registrationNumber);

    // Find aircrafts not used in any flight
    @Query("SELECT a FROM Aircraft a WHERE a NOT IN (SELECT DISTINCT f.aircraft FROM Flight f)")
    List<Aircraft> findUnusedAircrafts();
}