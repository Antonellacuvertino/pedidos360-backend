package cl.pedidos360.productos;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class StockProcessedEvent {
    @Id
    private UUID eventId;

    private Instant processedAt;

    protected StockProcessedEvent() {
    }

    public StockProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
