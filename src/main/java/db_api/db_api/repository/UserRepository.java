package db_api.db_api.repository;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.enums.UserRole;
import db_api.db_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Email is unique and indexed
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByStatus(AccountStatus status);

    List<User> findByRoleAndStatus(UserRole role, AccountStatus status);

    List<User> findByAirlineId(Long airlineId);

    default List<User> findPendingAirlineAdmins() {
        return findByRoleAndStatus(UserRole.AIRLINE_ADMIN, AccountStatus.PENDING);
    }

    default List<User> findActiveAirlineAdmins() {
        return findByRoleAndStatus(UserRole.AIRLINE_ADMIN, AccountStatus.ACTIVE);
    }

    // ✅ Count users by airline for validation
    long countByAirlineId(Long airlineId);
}