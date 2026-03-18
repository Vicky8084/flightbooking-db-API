package db_api.db_api.service;

import db_api.db_api.enums.UserRole;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.User;
import db_api.db_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createUser(User user) throws BookingException {
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BookingException("Email already registered: " + user.getEmail());
        }

        return userRepository.save(user);
    }

    public User getUserById(Long id) throws BookingException {
        return userRepository.findById(id)
                .orElseThrow(() -> new BookingException("User not found with ID: " + id));
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public User updateUser(Long id, User userDetails) throws BookingException {
        User user = getUserById(id);

        user.setFullName(userDetails.getFullName());
        user.setPhoneNumber(userDetails.getPhoneNumber());
        user.setPassword(userDetails.getPassword());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) throws BookingException {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}
