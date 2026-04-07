package db_api.db_api.controller;

import db_api.db_api.dto.BookingRequestDTO;
import db_api.db_api.enums.BookingStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Booking;
import db_api.db_api.model.Flight;
import db_api.db_api.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/db/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequestDTO request) {
        try {
            Booking booking = bookingService.createBooking(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking created successfully");
            response.put("pnr", booking.getPnrNumber());
            response.put("bookingId", booking.getId());
            response.put("status", booking.getStatus());
            response.put("totalAmount", booking.getTotalAmount());

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ✅ Existing endpoint - returns Map with success, count, bookings
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBookings(@PathVariable Long userId) {
        try {
            List<Booking> bookings = bookingService.findByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", bookings.size());
            response.put("bookings", bookings);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());

            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ✅ NEW ENDPOINT - returns direct list of bookings (no wrapper)
    @GetMapping("/user/{userId}/list")
    public ResponseEntity<?> getUserBookingsList(@PathVariable Long userId) {
        try {
            List<Booking> bookings = bookingService.findByUserId(userId);
            return ResponseEntity.ok(bookings);
        } catch (BookingException e) {
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ✅ NEW ENDPOINT - Get upcoming bookings (future flights)
    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<?> getUserUpcomingBookings(@PathVariable Long userId) {
        try {
            List<Booking> allBookings = bookingService.findByUserId(userId);
            LocalDateTime now = LocalDateTime.now();

            List<Booking> upcomingBookings = allBookings.stream()
                    .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                    .filter(booking -> {
                        if (booking.getBookingFlights() == null || booking.getBookingFlights().isEmpty()) {
                            return false;
                        }
                        LocalDateTime departureTime = booking.getBookingFlights().get(0).getFlight().getDepartureTime();
                        return departureTime.isAfter(now);
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", upcomingBookings.size());
            response.put("bookings", upcomingBookings);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ✅ NEW ENDPOINT - Get past/completed bookings
    @GetMapping("/user/{userId}/past")
    public ResponseEntity<?> getUserPastBookings(@PathVariable Long userId) {
        try {
            List<Booking> allBookings = bookingService.findByUserId(userId);
            LocalDateTime now = LocalDateTime.now();

            List<Booking> pastBookings = allBookings.stream()
                    .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED)
                    .filter(booking -> {
                        if (booking.getBookingFlights() == null || booking.getBookingFlights().isEmpty()) {
                            return false;
                        }
                        LocalDateTime departureTime = booking.getBookingFlights().get(0).getFlight().getDepartureTime();
                        return departureTime.isBefore(now);
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", pastBookings.size());
            response.put("bookings", pastBookings);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ✅ NEW ENDPOINT - Get cancelled bookings
    @GetMapping("/user/{userId}/cancelled")
    public ResponseEntity<?> getUserCancelledBookings(@PathVariable Long userId) {
        try {
            List<Booking> cancelledBookings = bookingService.findByUserIdAndStatus(userId, BookingStatus.CANCELLED);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", cancelledBookings.size());
            response.put("bookings", cancelledBookings);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingService.cancelBooking(bookingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking cancelled successfully");
            response.put("bookingId", booking.getId());
            response.put("status", booking.getStatus());

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/pnr/{pnr}")
    public ResponseEntity<?> getBookingByPNR(@PathVariable String pnr) {
        try {
            Booking booking = bookingService.findByPNR(pnr);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("booking", booking);
            response.put("message", "Booking found successfully");

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable Long bookingId) {
        try {
            Booking booking = bookingService.findById(bookingId);
            return ResponseEntity.ok(booking);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}