package db_api.db_api.controller;

import db_api.db_api.exception.BookingException;
import db_api.db_api.model.FareClass;
import db_api.db_api.service.FareClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db/fare-classes")
@RequiredArgsConstructor
public class FareClassController {

    private final FareClassService fareClassService;

    @GetMapping
    public ResponseEntity<List<FareClass>> getAllFareClasses() {
        return ResponseEntity.ok(fareClassService.getAllActiveFareClasses());
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getFareClassByCode(@PathVariable String code) {
        try {
            System.out.println("🔍 Fetching fare class with code: " + code);
            FareClass fareClass = fareClassService.getFareClassByCode(code);
            System.out.println("✅ Found fare class: " + fareClass.getCode() + " - " + fareClass.getName());
            return ResponseEntity.ok(fareClass);
        } catch (BookingException e) {
            System.out.println("❌ Fare class not found: " + code);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            System.out.println("❌ Error fetching fare class: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping
    public ResponseEntity<?> createFareClass(@Valid @RequestBody FareClass fareClass) {
        try {
            FareClass created = fareClassService.createFareClass(fareClass);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Fare class created successfully");
            response.put("fareClass", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFareClass(@PathVariable Long id, @Valid @RequestBody FareClass fareClass) {
        try {
            FareClass updated = fareClassService.updateFareClass(id, fareClass);
            return ResponseEntity.ok(Map.of("success", true, "fareClass", updated));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/calculate-baggage")
    public ResponseEntity<?> calculateBaggageCost(
            @RequestParam String fareClassCode,
            @RequestParam int extraKg) {
        try {
            FareClass fareClass = fareClassService.getFareClassByCode(fareClassCode);
            double cost = fareClassService.calculateExtraBaggageCost(fareClass, extraKg);
            return ResponseEntity.ok(Map.of(
                    "fareClass", fareClassCode,
                    "extraKg", extraKg,
                    "ratePerKg", fareClass.getExtraBaggageRatePerKg(),
                    "totalCost", cost
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}