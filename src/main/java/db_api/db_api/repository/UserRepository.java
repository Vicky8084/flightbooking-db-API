package db_api.db_api.repository;


import db_api.db_api.model.User;
import db_api.db_api.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find by email
    Optional<User> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find by role
    List<User> findByRole(UserRole role);

    // Check if user exists by ID (for validation)
    boolean existsById(Long id);
}
