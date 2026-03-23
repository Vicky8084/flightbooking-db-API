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
    public ResponseEntity<FareClass> getFareClassByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(fareClassService.getFareClassByCode(code));
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
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