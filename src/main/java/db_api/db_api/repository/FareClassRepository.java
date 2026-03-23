package db_api.db_api.repository;

import db_api.db_api.model.FareClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareClassRepository extends JpaRepository<FareClass, Long> {

    Optional<FareClass> findByCode(String code);

    // ✅ FIXED: Correct method name
    List<FareClass> findByIsActiveTrue();

    List<FareClass> findByIsActiveTrueOrderByPriceMultiplierAsc();

    Optional<FareClass> findByCodeAndIsActiveTrue(String code);
}