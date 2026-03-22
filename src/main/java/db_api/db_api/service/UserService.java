package db_api.db_api.service;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.enums.UserRole;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.User;
import db_api.db_api.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new user
     * ✅ Password is already encrypted when received from Auth API
     * ✅ Store as-is (already hashed)
     */
    public User createUser(User user) throws BookingException {
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BookingException("Email already registered: " + user.getEmail());
        }

        // ✅ Store password as-is (already encrypted by Auth API)
        // ✅ No additional encoding here - Auth API handles encryption
        return userRepository.save(user);
    }

    public User getUserById(Long id) throws BookingException {
        return userRepository.findById(id)
                .orElseThrow(() -> new BookingException("User not found with ID: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(String role) throws BookingException {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            return userRepository.findByRole(userRole);
        } catch (IllegalArgumentException e) {
            throw new BookingException("Invalid role: " + role);
        }
    }

    public List<User> getUsersByStatus(AccountStatus status) {
        return userRepository.findByStatus(status);
    }

    public List<User> getPendingAirlineAdmins() {
        return userRepository.findPendingAirlineAdmins();
    }

    /**
     * Update user password
     * ✅ Password is already encrypted when received from Auth API
     */
    @Transactional
    public User updatePassword(Long userId, String newPassword) throws BookingException {
        User user = getUserById(userId);
        // ✅ Store password as-is (already encrypted by Auth API)
        user.setPassword(newPassword);
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) throws BookingException {
        User user = getUserById(id);

        user.setFullName(userDetails.getFullName());
        user.setPhoneNumber(userDetails.getPhoneNumber());

        // ✅ Only update password if provided (already encrypted)
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        }

        return userRepository.save(user);
    }

    @Transactional
    public User approveAirlineAdmin(Long adminId, Long systemAdminId) throws BookingException {
        User admin = getUserById(adminId);
        User systemAdmin = getUserById(systemAdminId);

        if (systemAdmin.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new BookingException("Only SYSTEM_ADMIN can approve users");
        }

        if (admin.getRole() != UserRole.AIRLINE_ADMIN) {
            throw new BookingException("User is not an AIRLINE_ADMIN");
        }

        admin.setStatus(AccountStatus.ACTIVE);
        admin.setApprovedBy(systemAdmin);
        admin.setApprovedAt(LocalDateTime.now());

        return userRepository.save(admin);
    }

    public void deleteUser(Long id) throws BookingException {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}