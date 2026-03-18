package db_api.db_api.repository;

import db_api.db_api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Find reviews by flight
    List<Review> findByFlightId(Long flightId);

    // Find reviews by airline
    List<Review> findByAirlineId(Long airlineId);

    // Find reviews by user
    List<Review> findByUserId(Long userId);

    // Find approved reviews
    List<Review> findByIsApprovedTrue(Boolean isApproved);

    // Find pending reviews (for admin)
    List<Review> findByIsApprovedFalse();

    // Get average rating for a flight
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.flight.id = :flightId AND r.isApproved = true")
    Double getAverageRatingForFlight(@Param("flightId") Long flightId);

    // Get average rating for an airline
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.airline.id = :airlineId AND r.isApproved = true")
    Double getAverageRatingForAirline(@Param("airlineId") Long airlineId);

    // Get rating distribution for a flight
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.flight.id = :flightId AND r.isApproved = true GROUP BY r.rating")
    List<Object[]> getRatingDistributionForFlight(@Param("flightId") Long flightId);

    // Find reviews by rating
    List<Review> findByRating(Integer rating);

    // Search reviews by comment content
    @Query("SELECT r FROM Review r WHERE LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Review> searchByKeyword(@Param("keyword") String keyword);

    // Check if user has already reviewed a flight
    boolean existsByUserIdAndFlightId(Long userId, Long flightId);
}
