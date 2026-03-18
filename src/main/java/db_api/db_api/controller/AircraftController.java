package db_api.db_api.controller;

import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Aircraft;
import db_api.db_api.service.AircraftService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/aircrafts")
public class AircraftController {

    @Autowired
    private AircraftService aircraftService;

    @PostMapping
    public ResponseEntity<?> createAircraft(@Valid @RequestBody Aircraft aircraft) {
        try {
            Aircraft createdAircraft = aircraftService.createAircraft(aircraft);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Aircraft created successfully");
            response.put("aircraftId", createdAircraft.getId());
            response.put("registrationNumber", createdAircraft.getRegistrationNumber());
            response.put("model", createdAircraft.getModel());

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

    @GetMapping
    public ResponseEntity<?> getAllAircrafts() {
        try {
            List<Aircraft> aircrafts = aircraftService.getAllAircrafts();
            return ResponseEntity.ok(aircrafts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAircraftById(@PathVariable Long id) {
        try {
            Aircraft aircraft = aircraftService.getAircraftById(id);
            return ResponseEntity.ok(aircraft);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<?> getAircraftsByAirline(@PathVariable Long airlineId) {
        try {
            List<Aircraft> aircrafts = aircraftService.getAircraftsByAirline(airlineId);
            return ResponseEntity.ok(aircrafts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAircraft(@PathVariable Long id, @Valid @RequestBody Aircraft aircraftDetails) {
        try {
            Aircraft updatedAircraft = aircraftService.updateAircraft(id, aircraftDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Aircraft updated successfully");
            response.put("aircraft", updatedAircraft);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAircraft(@PathVariable Long id) {
        try {
            aircraftService.deleteAircraft(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Aircraft deleted successfully");

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}