package cl.pedidos360.streaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentAttemptEvent(
        UUID eventId,
        String clienteId,
        String tarjetaHuella,
        BigDecimal monto,
        String estado,
        Instant fecha) {

    public PaymentAttemptEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(tarjetaHuella, "tarjetaHuella");
        Objects.requireNonNull(monto, "monto");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fecha, "fecha");
        if (clienteId.isBlank() || tarjetaHuella.isBlank() || monto.signum() <= 0) {
            throw new IllegalArgumentException("Intento de pago incompleto");
        }
    }
}
