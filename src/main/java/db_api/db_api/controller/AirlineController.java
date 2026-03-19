package db_api.db_api.controller;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Airline;
import db_api.db_api.service.AirlineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/airlines")
public class AirlineController {

    @Autowired
    private AirlineService airlineService;

    @PostMapping
    public ResponseEntity<?> createAirline(@Valid @RequestBody Airline airline) {
        try {
            Airline createdAirline = airlineService.createAirline(airline);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Airline created successfully");
            response.put("airlineId", createdAirline.getId());
            response.put("code", createdAirline.getCode());
            response.put("name", createdAirline.getName());
            response.put("status", createdAirline.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAirlines() {
        try {
            List<Airline> airlines = airlineService.getAllAirlines();
            return ResponseEntity.ok(airlines);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAirlines() {
        try {
            List<Airline> pendingAirlines = airlineService.getPendingAirlines();
            return ResponseEntity.ok(pendingAirlines);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveAirlines() {
        try {
            List<Airline> activeAirlines = airlineService.getActiveAirlines();
            return ResponseEntity.ok(activeAirlines);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAirlineById(@PathVariable Long id) {
        try {
            Airline airline = airlineService.getAirlineById(id);
            return ResponseEntity.ok(airline);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<?> getAirlineByCode(@PathVariable String code) {
        try {
            Airline airline = airlineService.getAirlineByCode(code);
            return ResponseEntity.ok(airline);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{airlineId}/approve")
    public ResponseEntity<?> approveAirline(
            @PathVariable Long airlineId,
            @RequestParam Long adminId) {
        try {
            Airline approvedAirline = airlineService.approveAirline(airlineId, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Airline approved successfully");
            response.put("airlineId", approvedAirline.getId());
            response.put("name", approvedAirline.getName());
            response.put("status", approvedAirline.getStatus());
            response.put("approvedBy", approvedAirline.getApprovedBy().getId());
            response.put("approvedAt", approvedAirline.getApprovedAt());

            return ResponseEntity.ok(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{airlineId}/reject")
    public ResponseEntity<?> rejectAirline(
            @PathVariable Long airlineId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String reason) {
        try {
            Airline rejectedAirline = airlineService.rejectAirline(airlineId, adminId, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Airline rejected successfully",
                    "airlineId", rejectedAirline.getId(),
                    "status", rejectedAirline.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{airlineId}/suspend")
    public ResponseEntity<?> suspendAirline(
            @PathVariable Long airlineId,
            @RequestParam Long adminId) {
        try {
            Airline suspendedAirline = airlineService.suspendAirline(airlineId, adminId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Airline suspended successfully",
                    "airlineId", suspendedAirline.getId(),
                    "status", suspendedAirline.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAirline(@PathVariable Long id, @Valid @RequestBody Airline airlineDetails) {
        try {
            Airline updatedAirline = airlineService.updateAirline(id, airlineDetails);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Airline updated successfully",
                    "airline", updatedAirline
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAirline(@PathVariable Long id) {
        try {
            airlineService.deleteAirline(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Airline deleted successfully"
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}