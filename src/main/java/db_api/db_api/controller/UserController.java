package db_api.db_api.controller;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.enums.UserRole;
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

@RestController
@RequestMapping("/api/db/users")
public class UserController {

    @Autowired
    private UserService userService;

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
            User user = userService.getUserByEmail(email)
                    .orElseThrow(() -> new BookingException("User not found with email: " + email));
            return ResponseEntity.ok(user);
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable UserRole role) {
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

    // ✅ NEW: Get users by status
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getUsersByStatus(@PathVariable AccountStatus status) {
        try {
            List<User> users = userService.getUsersByStatus(status);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ NEW: Get pending airline admins
    @GetMapping("/pending-airline-admins")
    public ResponseEntity<?> getPendingAirlineAdmins() {
        try {
            List<User> pendingAdmins = userService.getPendingAirlineAdmins();
            return ResponseEntity.ok(pendingAdmins);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ✅ NEW: Approve airline admin
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