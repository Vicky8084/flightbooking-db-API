package db_api.db_api.controller;

import db_api.db_api.dto.FlightSearchDTO;
import db_api.db_api.dto.SeatMapDTO;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Flight;
import db_api.db_api.model.Seat;
import db_api.db_api.service.FlightSearchService;
import db_api.db_api.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/flights")
public class FlightController {

    @Autowired
    private FlightSearchService flightSearchService;

    @Autowired
    private FlightService flightService;

    // ✅ CREATE FLIGHT - POST (YEH MISSING THA)
    @PostMapping
    public ResponseEntity<?> createFlight(@Valid @RequestBody Flight flight) {
        try {
            Flight createdFlight = flightService.createFlight(flight);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Flight created successfully",
                    "flightId", createdFlight.getId(),
                    "flightNumber", createdFlight.getFlightNumber()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }

    // ✅ GET ALL FLIGHTS
    @GetMapping
    public ResponseEntity<?> getAllFlights() {
        try {
            List<Flight> flights = flightService.getAllFlights();
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ SEARCH FLIGHTS
    @PostMapping("/search")
    public ResponseEntity<?> searchFlights(@Valid @RequestBody FlightSearchDTO searchDTO) {
        try {
            List<Object> results = flightSearchService.searchFlights(searchDTO);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ GET AVAILABLE SEATS
    @GetMapping("/{flightId}/seats/available")
    public ResponseEntity<?> getAvailableSeats(@PathVariable Long flightId) {
        try {
            List<Seat> availableSeats = flightService.getAvailableSeats(flightId);
            return ResponseEntity.ok(availableSeats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ GET SEAT MAP
    @GetMapping("/{flightId}/seatmap")
    public ResponseEntity<?> getSeatMap(@PathVariable Long flightId) {
        try {
            SeatMapDTO seatMap = flightService.getSeatMap(flightId);
            return ResponseEntity.ok(seatMap);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ GET FLIGHT BY ID
    @GetMapping("/{flightId}")
    public ResponseEntity<?> getFlightDetails(@PathVariable Long flightId) {
        try {
            Flight flight = flightService.getFlightDetails(flightId);
            return ResponseEntity.ok(flight);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ GET FLIGHTS BY AIRLINE
    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<?> getFlightsByAirline(@PathVariable Long airlineId) {
        try {
            List<Flight> flights = flightService.getFlightsByAirline(airlineId);
            return ResponseEntity.ok(flights);
        } catch (BookingException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ GET FLIGHTS BY STATUS
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getFlightsByStatus(@PathVariable FlightStatus status) {
        try {
            List<Flight> flights = flightService.getFlightsByStatus(status);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ DELAY FLIGHT
    @PutMapping("/{flightId}/delay")
    public ResponseEntity<?> delayFlight(@PathVariable Long flightId,
                                         @RequestParam int minutes) {
        try {
            Flight flight = flightService.delayFlight(flightId, minutes);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Flight delayed by " + minutes + " minutes",
                    "flight", flight
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ UPDATE FLIGHT STATUS
    @PutMapping("/{flightId}/status")
    public ResponseEntity<?> updateFlightStatus(@PathVariable Long flightId,
                                                @RequestBody Map<String, FlightStatus> request) {
        try {
            FlightStatus newStatus = request.get("status");
            Flight flight = flightService.updateFlightStatus(flightId, newStatus);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Flight status updated to " + newStatus,
                    "flight", flight
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}