package db_api.db_api.controller;

import db_api.db_api.dto.SeatWithRowInfo;
import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Seat;
import db_api.db_api.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/db/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @PostMapping
    public ResponseEntity<?> createSeat(@Valid @RequestBody Seat seat) {
        try {
            Seat createdSeat = seatService.createSeat(seat);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Seat created successfully");
            response.put("seatId", createdSeat.getId());
            response.put("seatNumber", createdSeat.getSeatNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    @PostMapping("/bulk")
    public ResponseEntity<?> createSeatsBulk(@RequestBody Map<String, Object> request) {
        try {
            Long aircraftId = Long.parseLong(request.get("aircraftId").toString());

            // Convert List of Maps to List of Seat objects
            List<Map<String, Object>> seatMaps = (List<Map<String, Object>>) request.get("seats");
            List<Seat> seats = seatMaps.stream().map(map -> {
                Seat seat = new Seat();
                seat.setSeatNumber((String) map.get("seatNumber"));
                seat.setSeatClass(Enum.valueOf(SeatClass.class, (String) map.get("seatClass")));
                seat.setSeatType(Enum.valueOf(SeatType.class, (String) map.get("seatType")));
                seat.setHasExtraLegroom((Boolean) map.get("hasExtraLegroom"));
                seat.setIsNearExit((Boolean) map.get("isNearExit"));
                seat.setExtraPrice(map.get("extraPrice") != null ? Double.parseDouble(map.get("extraPrice").toString()) : 0.0);
                seat.setIsActive((Boolean) map.get("isActive"));
                return seat;
            }).collect(Collectors.toList());

            List<Seat> createdSeats = seatService.createSeatsBulk(aircraftId, seats);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Seats created successfully");
            response.put("count", createdSeats.size());
            response.put("seats", createdSeats);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    @GetMapping("/aircraft/{aircraftId}")
    public ResponseEntity<?> getSeatsByAircraft(@PathVariable Long aircraftId) {
        List<Seat> seats = seatService.getSeatsByAircraft(aircraftId);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/aircraft/{aircraftId}/class/{seatClass}")
    public ResponseEntity<?> getSeatsByAircraftAndClass(
            @PathVariable Long aircraftId,
            @PathVariable String seatClass) {
        try {
            List<Seat> seats = seatService.getSeatsByAircraftAndClass(aircraftId, seatClass);
            return ResponseEntity.ok(seats);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSeatById(@PathVariable Long id) {
        try {
            Seat seat = seatService.getSeatById(id);
            return ResponseEntity.ok(seat);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSeat(@PathVariable Long id, @Valid @RequestBody Seat seatDetails) {
        try {
            Seat updatedSeat = seatService.updateSeat(id, seatDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Seat updated successfully");
            response.put("seat", updatedSeat);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSeat(@PathVariable Long id) {
        try {
            seatService.deleteSeat(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Seat deleted successfully");

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * NEW: Get seats grouped by row with full information
     * URL: GET /api/db/seats/aircraft/{aircraftId}/grouped
     */
    @GetMapping("/aircraft/{aircraftId}/grouped")
    public ResponseEntity<?> getSeatsGroupedByRow(@PathVariable Long aircraftId) {
        try {
            Map<Integer, List<SeatWithRowInfo>> groupedSeats = seatService.getSeatsGroupedByRow(aircraftId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("aircraftId", aircraftId);
            response.put("seatsByRow", groupedSeats);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * NEW: Get seat map with categories (for frontend display)
     * URL: GET /api/db/seats/aircraft/{aircraftId}/map
     */
    @GetMapping("/aircraft/{aircraftId}/map")
    public ResponseEntity<?> getSeatMapWithCategories(@PathVariable Long aircraftId) {
        try {
            Map<String, Object> seatMap = seatService.getSeatMapWithCategories(aircraftId);
            seatMap.put("success", true);
            return ResponseEntity.ok(seatMap);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

}