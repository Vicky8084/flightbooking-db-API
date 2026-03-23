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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/flights")
public class FlightController {

    @Autowired
    private FlightSearchService flightSearchService;

    @Autowired
    private FlightService flightService;

    @PostMapping
    public ResponseEntity<?> createFlight(@Valid @RequestBody Flight flight) {
        try {
            Flight createdFlight = flightService.createFlight(flight);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Flight created successfully",
                    "flightId", createdFlight.getId(),
                    "flightNumber", createdFlight.getFlightNumber(),
                    "departureTime", createdFlight.getDepartureTime(),
                    "arrivalTime", createdFlight.getArrivalTime(),
                    "status", createdFlight.getStatus(),
                    "economyPrice", createdFlight.getBasePriceEconomy(),
                    "businessPrice", createdFlight.getBasePriceBusiness(),
                    "firstClassPrice", createdFlight.getBasePriceFirstClass()
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

    @GetMapping("/{flightId}/seats/available")
    public ResponseEntity<?> getAvailableSeats(@PathVariable Long flightId) {
        try {
            List<Seat> availableSeats = flightService.getAvailableSeats(flightId);
            return ResponseEntity.ok(availableSeats);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

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

    @GetMapping("/{flightId}")
    public ResponseEntity<?> getFlightDetails(@PathVariable Long flightId) {
        try {
            Flight flight = flightService.getFlightDetails(flightId);
            return ResponseEntity.ok(flight);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

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

    @PutMapping("/{flightId}/delay")
    public ResponseEntity<?> delayFlight(@PathVariable Long flightId,
                                         @RequestParam int minutes) {
        try {
            Flight flight = flightService.delayFlight(flightId, minutes);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Flight delayed by " + minutes + " minutes",
                    "flightId", flight.getId(),
                    "flightNumber", flight.getFlightNumber(),
                    "newDepartureTime", flight.getDepartureTime(),
                    "newArrivalTime", flight.getArrivalTime(),
                    "delayCount", flight.getDelayCount(),
                    "totalDelayMinutes", flight.getTotalDelayMinutes(),
                    "status", flight.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{flightId}/reschedule")
    public ResponseEntity<?> rescheduleFlight(
            @PathVariable Long flightId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDepartureTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newArrivalTime) {
        try {
            Flight flight = flightService.rescheduleFlight(flightId, newDepartureTime, newArrivalTime);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Flight rescheduled successfully",
                    "flightId", flight.getId(),
                    "flightNumber", flight.getFlightNumber(),
                    "newDepartureTime", flight.getDepartureTime(),
                    "newArrivalTime", flight.getArrivalTime(),
                    "originalDepartureTime", flight.getOriginalDepartureTime(),
                    "status", flight.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{flightId}/cancel")
    public ResponseEntity<?> cancelFlight(@PathVariable Long flightId) {
        try {
            Flight flight = flightService.cancelFlight(flightId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Flight cancelled successfully",
                    "flightId", flight.getId(),
                    "flightNumber", flight.getFlightNumber(),
                    "status", flight.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

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


    // Add these methods to existing FlightController.java

    /**
     * Get price breakdown for a flight
     */
    @GetMapping("/{flightId}/price-breakdown")
    public ResponseEntity<?> getPriceBreakdown(
            @PathVariable Long flightId,
            @RequestParam String seatClass,
            @RequestParam String fareClassCode) {
        try {
            Map<String, Object> breakdown = flightService.getPriceBreakdown(flightId, seatClass, fareClassCode);
            return ResponseEntity.ok(breakdown);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Update dynamic pricing for a flight
     */
    @PutMapping("/{flightId}/update-pricing")
    public ResponseEntity<?> updateDynamicPricing(@PathVariable Long flightId) {
        try {
            flightService.updateDynamicPricing(flightId);
            Flight flight = flightService.getFlightDetails(flightId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dynamic pricing updated",
                    "economyPrice", flight.getCurrentPriceEconomy(),
                    "businessPrice", flight.getCurrentPriceBusiness(),
                    "firstClassPrice", flight.getCurrentPriceFirstClass()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Update all flights dynamic pricing (scheduled job)
     */
    @PostMapping("/update-all-pricing")
    public ResponseEntity<?> updateAllDynamicPricing() {
        flightService.updateAllDynamicPricing();
        return ResponseEntity.ok(Map.of("success", true, "message", "All flights pricing updated"));
    }
}