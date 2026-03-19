package db_api.db_api.repository;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.model.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long> {

    Optional<Airline> findByCode(String code);

    Optional<Airline> findByName(String name);

    List<Airline> findByStatus(AccountStatus status);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    // Find pending airlines
    default List<Airline> findPendingAirlines() {
        return findByStatus(AccountStatus.PENDING);
    }

    // Find active airlines
    default List<Airline> findActiveAirlines() {
        return findByStatus(AccountStatus.ACTIVE);
    }

    // Find rejected airlines
    default List<Airline> findRejectedAirlines() {
        return findByStatus(AccountStatus.REJECTED);
    }
}