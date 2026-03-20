package db_api.db_api.service;

import db_api.db_api.enums.ReviewStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Airline;
import db_api.db_api.model.Flight;
import db_api.db_api.model.Review;
import db_api.db_api.model.User;
import db_api.db_api.repository.AirlineRepository;
import db_api.db_api.repository.FlightRepository;
import db_api.db_api.repository.ReviewRepository;
import db_api.db_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirlineRepository airlineRepository;

    /**
     * Create a new review
     */
    public Review createReview(Long userId, Long flightId, Integer rating, String comment, String title) throws BookingException {
        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookingException("User not found with ID: " + userId));

        // Validate flight
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BookingException("Flight not found with ID: " + flightId));

        // Check if user has already reviewed this flight
        if (reviewRepository.existsByUserIdAndFlightId(userId, flightId)) {
            throw new BookingException("You have already reviewed this flight");
        }

        // Create review
        Review review = new Review();
        review.setUser(user);
        review.setFlight(flight);
        review.setAirline(flight.getAircraft().getAirline());
        review.setRating(rating);
        review.setComment(comment);
        review.setTitle(title);
        review.setStatus(ReviewStatus.PENDING);

        return reviewRepository.save(review);
    }

    /**
     * Create airline review
     */
    public Review createAirlineReview(Long userId, Long airlineId, Integer rating, String comment, String title) throws BookingException {
        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookingException("User not found with ID: " + userId));

        // Validate airline
        Airline airline = airlineRepository.findById(airlineId)
                .orElseThrow(() -> new BookingException("Airline not found with ID: " + airlineId));

        // Check if user has already reviewed this airline
        if (reviewRepository.existsByUserIdAndAirlineId(userId, airlineId)) {
            throw new BookingException("You have already reviewed this airline");
        }

        // Create review
        Review review = new Review();
        review.setUser(user);
        review.setFlight(null);
        review.setAirline(airline);
        review.setRating(rating);
        review.setComment(comment);
        review.setTitle(title);
        review.setStatus(ReviewStatus.PENDING);

        return reviewRepository.save(review);
    }

    /**
     * Get all reviews for a flight (approved only)
     */
    public List<Review> getFlightReviews(Long flightId) {
        return reviewRepository.findByFlightIdAndStatus(flightId, ReviewStatus.APPROVED);
    }

    /**
     * Get all reviews for an airline (approved only)
     */
    public List<Review> getAirlineReviews(Long airlineId) {
        return reviewRepository.findByAirlineIdAndStatus(airlineId, ReviewStatus.APPROVED);
    }

    /**
     * Get all reviews by user
     */
    public List<Review> getUserReviews(Long userId) throws BookingException {
        if (!userRepository.existsById(userId)) {
            throw new BookingException("User not found with ID: " + userId);
        }
        return reviewRepository.findByUserId(userId);
    }

    /**
     * Get pending reviews for admin
     */
    public List<Review> getPendingReviews() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING);
    }

    /**
     * Approve a review
     */
    @Transactional
    public Review approveReview(Long reviewId, Long adminId) throws BookingException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BookingException("Review not found with ID: " + reviewId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found"));

        if (admin.getRole() != db_api.db_api.enums.UserRole.SYSTEM_ADMIN) {
            throw new BookingException("Only SYSTEM_ADMIN can approve reviews");
        }

        review.setStatus(ReviewStatus.APPROVED);
        return reviewRepository.save(review);
    }

    /**
     * Reject a review
     */
    @Transactional
    public Review rejectReview(Long reviewId, Long adminId, String rejectionReason) throws BookingException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BookingException("Review not found with ID: " + reviewId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found"));

        if (admin.getRole() != db_api.db_api.enums.UserRole.SYSTEM_ADMIN) {
            throw new BookingException("Only SYSTEM_ADMIN can reject reviews");
        }

        review.setStatus(ReviewStatus.REJECTED);
        return reviewRepository.save(review);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) throws BookingException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BookingException("Review not found with ID: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BookingException("User not found"));
            if (user.getRole() != db_api.db_api.enums.UserRole.SYSTEM_ADMIN) {
                throw new BookingException("You can only delete your own reviews");
            }
        }

        reviewRepository.delete(review);
    }

    /**
     * Get average rating for a flight
     */
    public Map<String, Object> getFlightRatingStats(Long flightId) {
        Double avgRating = reviewRepository.getAverageRatingForFlight(flightId);
        List<Object[]> distribution = reviewRepository.getRatingDistributionForFlight(flightId);

        // ✅ FIXED: Convert int to Long using longValue()
        int totalReviewsInt = reviewRepository.findByFlightIdAndStatus(flightId, ReviewStatus.APPROVED).size();
        Long totalReviews = (long) totalReviewsInt;

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", avgRating != null ? avgRating : 0.0);
        stats.put("totalReviews", totalReviews);
        stats.put("ratingDistribution", distribution);

        return stats;
    }

    /**
     * Get average rating for an airline
     */
    public Map<String, Object> getAirlineRatingStats(Long airlineId) {
        Double avgRating = reviewRepository.getAverageRatingForAirline(airlineId);

        // ✅ FIXED: Convert int to Long using longValue()
        int totalReviewsInt = reviewRepository.findByAirlineIdAndStatus(airlineId, ReviewStatus.APPROVED).size();
        Long totalReviews = (long) totalReviewsInt;

        // Get count for each rating
        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            Long count = reviewRepository.countByAirlineAndRating(airlineId, i);
            ratingCounts.put(i, count != null ? count : 0L);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", avgRating != null ? avgRating : 0.0);
        stats.put("totalReviews", totalReviews);
        stats.put("ratingCounts", ratingCounts);

        return stats;
    }

    /**
     * Update a review (only if not approved yet)
     */
    @Transactional
    public Review updateReview(Long reviewId, Long userId, Integer rating, String comment, String title) throws BookingException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BookingException("Review not found with ID: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new BookingException("You can only update your own reviews");
        }

        if (review.getStatus() == ReviewStatus.APPROVED) {
            throw new BookingException("Approved reviews cannot be edited");
        }

        if (rating != null) {
            review.setRating(rating);
        }
        if (comment != null) {
            review.setComment(comment);
        }
        if (title != null) {
            review.setTitle(title);
        }

        return reviewRepository.save(review);
    }
}