package db_api.db_api.repository;


import db_api.db_api.model.Payment;
import db_api.db_api.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by booking ID
    Optional<Payment> findByBookingId(Long bookingId);

    // Find by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Find by status
    List<Payment> findByStatus(PaymentStatus status);
}
