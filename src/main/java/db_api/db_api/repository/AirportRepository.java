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

    // ✅ **YEH METHOD ADD KARO - Code se airport find karne ke liye**
    Optional<Airport> findByCode(String code);

    // Check if exists
    boolean existsByCode(String code);

    // Search by city
    @Query("SELECT a FROM Airport a WHERE LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))")
    List<Airport> findByCityContaining(@Param("city") String city);

    // Find by country
    List<Airport> findByCountry(String country);
}