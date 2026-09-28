package cl.pedidos360.auditoria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AuditEntry {
    @Id
    private UUID eventId;

    private String eventType;
    private Long clienteId;
    private Long productoId;
    private int cantidad;
    private BigDecimal total;
    private Instant receivedAt;

    protected AuditEntry() {
    }

    public AuditEntry(
            UUID eventId,
            String eventType,
            Long clienteId,
            Long productoId,
            int cantidad,
            BigDecimal total) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.clienteId = clienteId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.total = total;
        this.receivedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
