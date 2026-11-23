package cl.pedidos360.analytics;

import java.time.Instant;
import java.util.UUID;

import cl.pedidos360.streaming.PaymentAttemptEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PaymentAttemptRecord {
    @Id
    private UUID id;
    private String clienteId;
    private String estado;
    private Instant fecha;

    protected PaymentAttemptRecord() {
    }

    public PaymentAttemptRecord(PaymentAttemptEvent event) {
        id = event.eventId();
        clienteId = event.clienteId();
        estado = event.estado();
        fecha = event.fecha();
    }

    public UUID getId() { return id; }
    public String getClienteId() { return clienteId; }
    public String getEstado() { return estado; }
}
