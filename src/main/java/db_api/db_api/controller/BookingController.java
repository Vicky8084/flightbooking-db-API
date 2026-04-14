package db_api.db_api.controller;

import db_api.db_api.dto.BookingRequestDTO;
import db_api.db_api.enums.BookingStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.*;
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

            // ✅ Convert Booking objects to Map for better compatibility
            List<Map<String, Object>> bookingMaps = new ArrayList<>();
            for (Booking booking : bookings) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", booking.getId());
                map.put("pnrNumber", booking.getPnrNumber());
                map.put("bookingTime", booking.getBookingTime());
                map.put("totalAmount", booking.getTotalAmount());
                map.put("status", booking.getStatus() != null ? booking.getStatus().name() : null);
                map.put("fareClassCode", booking.getFareClassCode());

                // Add booking flights info
                if (booking.getBookingFlights() != null && !booking.getBookingFlights().isEmpty()) {
                    List<Map<String, Object>> bookingFlightsList = new ArrayList<>();
                    for (BookingFlight bf : booking.getBookingFlights()) {
                        Map<String, Object> bfMap = new HashMap<>();
                        Flight flight = bf.getFlight();
                        if (flight != null) {
                            Map<String, Object> flightMap = new HashMap<>();
                            flightMap.put("id", flight.getId());
                            flightMap.put("flightNumber", flight.getFlightNumber());
                            flightMap.put("departureTime", flight.getDepartureTime());
                            flightMap.put("arrivalTime", flight.getArrivalTime());
                            flightMap.put("duration", flight.getDuration());
                            flightMap.put("status", flight.getStatus() != null ? flight.getStatus().name() : null);

                            // Source airport
                            if (flight.getSourceAirport() != null) {
                                Map<String, Object> sourceMap = new HashMap<>();
                                sourceMap.put("code", flight.getSourceAirport().getCode());
                                sourceMap.put("city", flight.getSourceAirport().getCity());
                                sourceMap.put("name", flight.getSourceAirport().getName());
                                flightMap.put("sourceAirport", sourceMap);
                            }

                            // Destination airport
                            if (flight.getDestinationAirport() != null) {
                                Map<String, Object> destMap = new HashMap<>();
                                destMap.put("code", flight.getDestinationAirport().getCode());
                                destMap.put("city", flight.getDestinationAirport().getCity());
                                destMap.put("name", flight.getDestinationAirport().getName());
                                flightMap.put("destinationAirport", destMap);
                            }

                            // Aircraft & Airline
                            if (flight.getAircraft() != null) {
                                Map<String, Object> aircraftMap = new HashMap<>();
                                aircraftMap.put("id", flight.getAircraft().getId());
                                aircraftMap.put("model", flight.getAircraft().getModel());
                                if (flight.getAircraft().getAirline() != null) {
                                    Map<String, Object> airlineMap = new HashMap<>();
                                    airlineMap.put("id", flight.getAircraft().getAirline().getId());
                                    airlineMap.put("name", flight.getAircraft().getAirline().getName());
                                    airlineMap.put("code", flight.getAircraft().getAirline().getCode());
                                    aircraftMap.put("airline", airlineMap);
                                }
                                flightMap.put("aircraft", aircraftMap);
                            }

                            bfMap.put("flight", flightMap);
                        }

                        // Add passenger seats
                        if (bf.getPassengerSeats() != null && !bf.getPassengerSeats().isEmpty()) {
                            List<Map<String, Object>> passengerSeatsList = new ArrayList<>();
                            for (PassengerSeat ps : bf.getPassengerSeats()) {
                                Map<String, Object> psMap = new HashMap<>();
                                if (ps.getPassenger() != null) {
                                    Map<String, Object> passengerMap = new HashMap<>();
                                    passengerMap.put("id", ps.getPassenger().getId());
                                    passengerMap.put("fullName", ps.getPassenger().getFullName());
                                    psMap.put("passenger", passengerMap);
                                }
                                if (ps.getSeat() != null) {
                                    Map<String, Object> seatMap = new HashMap<>();
                                    seatMap.put("id", ps.getSeat().getId());
                                    seatMap.put("seatNumber", ps.getSeat().getSeatNumber());
                                    psMap.put("seat", seatMap);
                                }
                                psMap.put("seatPrice", ps.getSeatPrice());
                                passengerSeatsList.add(psMap);
                            }
                            bfMap.put("passengerSeats", passengerSeatsList);
                        }
                        bookingFlightsList.add(bfMap);
                    }
                    map.put("bookingFlights", bookingFlightsList);
                }

                // Add passengers
                if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
                    List<Map<String, Object>> passengersList = new ArrayList<>();
                    for (Passenger p : booking.getPassengers()) {
                        Map<String, Object> pMap = new HashMap<>();
                        pMap.put("id", p.getId());
                        pMap.put("fullName", p.getFullName());
                        pMap.put("age", p.getAge());
                        pMap.put("email", p.getEmail());
                        pMap.put("phoneNumber", p.getPhoneNumber());
                        passengersList.add(pMap);
                    }
                    map.put("passengers", passengersList);
                }

                bookingMaps.add(map);
            }

            System.out.println("✅ getUserBookingsList for userId: " + userId + " returned " + bookingMaps.size() + " bookings");
            return ResponseEntity.ok(bookingMaps);
        } catch (BookingException e) {
            System.out.println("❌ getUserBookingsList error: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            System.out.println("❌ getUserBookingsList exception: " + e.getMessage());
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