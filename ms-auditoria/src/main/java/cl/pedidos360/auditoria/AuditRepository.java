package cl.pedidos360.auditoria;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, UUID> {
}
