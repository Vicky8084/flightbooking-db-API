package db_api.db_api.repository;

import db_api.db_api.model.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

    // ✅ Find airport by code
    Optional<Airport> findByCode(String code);

    // Check if exists
    boolean existsByCode(String code);

    // Search by city
    @Query("SELECT a FROM Airport a WHERE LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<Airport> findByCityContaining(@Param("city") String city);

    // Find by country
    List<Airport> findByCountry(String country);

    // ✅ Find by name
    Optional<Airport> findByName(String name);

    // ✅ Find all airports in a country
    List<Airport> findAllByCountryOrderByCityAsc(String country);
}