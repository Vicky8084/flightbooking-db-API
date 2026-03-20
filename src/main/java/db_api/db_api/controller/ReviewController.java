package db_api.db_api.controller;

import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Review;
import db_api.db_api.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * Create review for a flight
     */
    @PostMapping("/flight")
    public ResponseEntity<?> createFlightReview(
            @RequestParam Long userId,
            @RequestParam Long flightId,
            @RequestParam @Min(1) @Max(5) Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String title) {

        try {
            Review review = reviewService.createReview(userId, flightId, rating, comment, title);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review submitted successfully. Pending admin approval.");
            response.put("reviewId", review.getId());
            response.put("status", review.getStatus());

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Create review for an airline
     */
    @PostMapping("/airline")
    public ResponseEntity<?> createAirlineReview(
            @RequestParam Long userId,
            @RequestParam Long airlineId,
            @RequestParam @Min(1) @Max(5) Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String title) {

        try {
            Review review = reviewService.createAirlineReview(userId, airlineId, rating, comment, title);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review submitted successfully. Pending admin approval.");
            response.put("reviewId", review.getId());
            response.put("status", review.getStatus());

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get reviews for a flight (approved only)
     */
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<?> getFlightReviews(@PathVariable Long flightId) {
        try {
            List<Review> reviews = reviewService.getFlightReviews(flightId);
            Map<String, Object> stats = reviewService.getFlightRatingStats(flightId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews);
            response.put("stats", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get reviews for an airline (approved only)
     */
    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<?> getAirlineReviews(@PathVariable Long airlineId) {
        try {
            List<Review> reviews = reviewService.getAirlineReviews(airlineId);
            Map<String, Object> stats = reviewService.getAirlineRatingStats(airlineId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews);
            response.put("stats", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get reviews by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserReviews(@PathVariable Long userId) {
        try {
            List<Review> reviews = reviewService.getUserReviews(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reviews", reviews
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get pending reviews (admin only)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReviews() {
        try {
            List<Review> reviews = reviewService.getPendingReviews();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reviews", reviews
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Approve a review (admin only)
     */
    @PutMapping("/{reviewId}/approve")
    public ResponseEntity<?> approveReview(
            @PathVariable Long reviewId,
            @RequestParam Long adminId) {

        try {
            Review review = reviewService.approveReview(reviewId, adminId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review approved successfully",
                    "reviewId", review.getId(),
                    "status", review.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Reject a review (admin only)
     */
    @PutMapping("/{reviewId}/reject")
    public ResponseEntity<?> rejectReview(
            @PathVariable Long reviewId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String reason) {

        try {
            Review review = reviewService.rejectReview(reviewId, adminId, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review rejected successfully",
                    "reviewId", review.getId(),
                    "status", review.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete a review
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            @RequestParam Long userId) {

        try {
            reviewService.deleteReview(reviewId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review deleted successfully"
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Update a review
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @RequestParam Long userId,
            @RequestParam(required = false) @Min(1) @Max(5) Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String title) {

        try {
            Review review = reviewService.updateReview(reviewId, userId, rating, comment, title);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review updated successfully",
                    "review", review
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get flight rating stats
     */
    @GetMapping("/flight/{flightId}/stats")
    public ResponseEntity<?> getFlightRatingStats(@PathVariable Long flightId) {
        try {
            Map<String, Object> stats = reviewService.getFlightRatingStats(flightId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get airline rating stats
     */
    @GetMapping("/airline/{airlineId}/stats")
    public ResponseEntity<?> getAirlineRatingStats(@PathVariable Long airlineId) {
        try {
            Map<String, Object> stats = reviewService.getAirlineRatingStats(airlineId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}