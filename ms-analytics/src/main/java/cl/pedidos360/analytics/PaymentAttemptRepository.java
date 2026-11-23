package cl.pedidos360.analytics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptRecord, UUID> {
    long countByEstado(String estado);
}
