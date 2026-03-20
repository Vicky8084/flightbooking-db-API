package db_api.db_api.repository;

import db_api.db_api.enums.ReviewStatus;
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

    // Find reviews by status
    List<Review> findByStatus(ReviewStatus status);

    // Find approved reviews by flight
    List<Review> findByFlightIdAndStatus(Long flightId, ReviewStatus status);

    // Find approved reviews by airline
    List<Review> findByAirlineIdAndStatus(Long airlineId, ReviewStatus status);

    // Get average rating for a flight
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.flight.id = :flightId AND r.status = 'APPROVED'")
    Double getAverageRatingForFlight(@Param("flightId") Long flightId);

    // Get average rating for an airline
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.airline.id = :airlineId AND r.status = 'APPROVED'")
    Double getAverageRatingForAirline(@Param("airlineId") Long airlineId);

    // Get rating distribution for a flight
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.flight.id = :flightId AND r.status = 'APPROVED' GROUP BY r.rating")
    List<Object[]> getRatingDistributionForFlight(@Param("flightId") Long flightId);

    // Search reviews by comment content (approved only)
    @Query("SELECT r FROM Review r WHERE LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%')) AND r.status = 'APPROVED'")
    List<Review> searchByKeyword(@Param("keyword") String keyword);

    // Find pending reviews for admin
    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    // Check if user has already reviewed a flight
    boolean existsByUserIdAndFlightId(Long userId, Long flightId);

    // Check if user has already reviewed an airline
    boolean existsByUserIdAndAirlineId(Long userId, Long airlineId);

    // Count reviews by rating
    @Query("SELECT COUNT(r) FROM Review r WHERE r.airline.id = :airlineId AND r.rating = :rating AND r.status = 'APPROVED'")
    Long countByAirlineAndRating(@Param("airlineId") Long airlineId, @Param("rating") Integer rating);
}