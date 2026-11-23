package cl.pedidos360.pagos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentVelocityRepository extends JpaRepository<PaymentVelocity, String> {
    // PostgreSQL serializa tambien el primer intento concurrente de un usuario.
    @Modifying
    @Query(value = "insert into payment_velocity (fingerprint, attempts) values (:id, 0) "
            + "on conflict (fingerprint) do nothing", nativeQuery = true)
    void ensureExists(@Param("id") String id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentVelocity> findById(String id);
}
