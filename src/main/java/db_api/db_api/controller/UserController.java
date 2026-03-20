package db_api.db_api.controller;

import db_api.db_api.exception.BookingException;
import db_api.db_api.model.User;
import db_api.db_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/db/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Existing createUser method...

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("userId", createdUser.getId());
            response.put("email", createdUser.getEmail());
            response.put("role", createdUser.getRole());
            response.put("status", createdUser.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> userOpt = userService.getUserByEmail(email);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(userOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        try {
            List<User> users = userService.getUsersByRole(role);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<?> updatePassword(
            @PathVariable Long userId,
            @RequestParam String password) {

        try {
            userService.updatePassword(userId, password);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password updated successfully"
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User updated successfully",
                    "user", updatedUser
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/{adminId}/approve")
    public ResponseEntity<?> approveAirlineAdmin(
            @PathVariable Long adminId,
            @RequestParam Long systemAdminId) {

        try {
            User approvedAdmin = userService.approveAirlineAdmin(adminId, systemAdminId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Airline admin approved successfully",
                    "userId", approvedAdmin.getId(),
                    "status", approvedAdmin.getStatus()
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User deleted successfully"
            ));
        } catch (BookingException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}