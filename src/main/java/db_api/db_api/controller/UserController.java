package db_api.db_api.controller;

import db_api.db_api.dto.response.UserResponse;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/db/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Convert User entity to safe UserResponse DTO
     * ✅ Excludes password field completely
     */
    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
        response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        response.setActive(user.getIsActive() != null ? user.getIsActive() : true);

        if (user.getAirline() != null) {
            response.setAirlineId(user.getAirline().getId());
            response.setAirlineName(user.getAirline().getName());
        }

        return response;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            // ✅ Password is already encrypted from Auth API
            User createdUser = userService.createUser(user);

            // ✅ Return safe response without password
            UserResponse safeResponse = toUserResponse(createdUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("userId", createdUser.getId());
            response.put("email", createdUser.getEmail());
            response.put("role", createdUser.getRole());
            response.put("status", createdUser.getStatus());
            // ✅ No password in response

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
            // ✅ Convert to safe DTOs without passwords
            List<UserResponse> safeUsers = users.stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(safeUsers);
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
            // ✅ Return safe DTO without password
            return ResponseEntity.ok(toUserResponse(user));
        } catch (BookingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> userOpt = userService.getUserByEmail(email);
            if (userOpt.isPresent()) {
                // ✅ Return safe DTO without password
                return ResponseEntity.ok(toUserResponse(userOpt.get()));
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

    /**
     * ✅ Special endpoint for Auth API that includes password
     * ⚠️ This should ONLY be called by Auth API (internal service)
     */
    @GetMapping("/email/{email}/with-password")
    public ResponseEntity<?> getUserByEmailWithPassword(@PathVariable String email) {
        try {
            Optional<User> userOpt = userService.getUserByEmail(email);
            if (userOpt.isPresent()) {
                // ⚠️ Return FULL user with password - ONLY for Auth API
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
            // ✅ Convert to safe DTOs without passwords
            List<UserResponse> safeUsers = users.stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(safeUsers);
        } catch (BookingException e) {
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
            // ✅ Password is already encrypted from Auth API
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
            // ✅ Return safe DTO without password
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User updated successfully",
                    "user", toUserResponse(updatedUser)
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
            // ✅ Return safe DTO without password
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