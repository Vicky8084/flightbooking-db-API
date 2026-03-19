package db_api.db_api.repository;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.enums.UserRole;
import db_api.db_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByStatus(AccountStatus status);

    List<User> findByRoleAndStatus(UserRole role, AccountStatus status);

    // Find by airline
    List<User> findByAirlineId(Long airlineId);

    // Find pending airline admins
    default List<User> findPendingAirlineAdmins() {
        return findByRoleAndStatus(UserRole.AIRLINE_ADMIN, AccountStatus.PENDING);
    }

    // Find active airline admins
    default List<User> findActiveAirlineAdmins() {
        return findByRoleAndStatus(UserRole.AIRLINE_ADMIN, AccountStatus.ACTIVE);
    }
}