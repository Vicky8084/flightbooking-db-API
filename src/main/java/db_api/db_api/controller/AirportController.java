package db_api.db_api.controller;

import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Airport;
import db_api.db_api.service.AirportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/airports")
public class AirportController {

    @Autowired
    private AirportService airportService;

    @PostMapping
    public ResponseEntity<?> createAirport(@Valid @RequestBody Airport airport) {
        try {
            Airport createdAirport = airportService.createAirport(airport);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Airport created successfully");
            response.put("airportId", createdAirport.getId());
            response.put("code", createdAirport.getCode());
            response.put("name", createdAirport.getName());

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
    public ResponseEntity<?> getAllAirports() {
        try {
            List<Airport> airports = airportService.getAllAirports();
            return ResponseEntity.ok(airports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAirportById(@PathVariable Long id) {
        try {
            Airport airport = airportService.getAirportById(id);
            return ResponseEntity.ok(airport);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<?> getAirportByCode(@PathVariable String code) {
        try {
            Airport airport = airportService.getAirportByCode(code);
            return ResponseEntity.ok(airport);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<?> getAirportsByCity(@PathVariable String city) {
        try {
            List<Airport> airports = airportService.getAirportsByCity(city);
            return ResponseEntity.ok(airports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<?> getAirportsByCountry(@PathVariable String country) {
        try {
            List<Airport> airports = airportService.getAirportsByCountry(country);
            return ResponseEntity.ok(airports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAirport(@PathVariable Long id, @Valid @RequestBody Airport airportDetails) {
        try {
            Airport updatedAirport = airportService.updateAirport(id, airportDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Airport updated successfully");
            response.put("airport", updatedAirport);

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAirport(@PathVariable Long id) {
        try {
            airportService.deleteAirport(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Airport deleted successfully");

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}