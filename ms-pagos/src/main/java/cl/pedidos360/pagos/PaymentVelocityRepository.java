package cl.pedidos360.pagos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface PaymentVelocityRepository extends JpaRepository<PaymentVelocity, String> {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentVelocity> findById(String id);
}
