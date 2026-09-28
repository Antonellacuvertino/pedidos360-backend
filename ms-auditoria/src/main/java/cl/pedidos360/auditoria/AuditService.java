package cl.pedidos360.auditoria;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.pedidos360.messaging.OrderEvent;

@Service
public class AuditService {
    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(OrderEvent event) {
        if (repository.existsById(event.eventId())) {
            return;
        }
        repository.save(new AuditEntry(
                event.eventId(),
                event.eventType(),
                event.clienteId(),
                event.productoId(),
                event.cantidad(),
                event.total()));
    }
}
