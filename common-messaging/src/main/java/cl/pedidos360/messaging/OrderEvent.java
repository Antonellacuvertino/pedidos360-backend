package cl.pedidos360.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        String eventType,
        Long clienteId,
        Long productoId,
        int cantidad,
        BigDecimal total,
        String estado,
        String recipientEmail,
        String subject,
        String message,
        Instant createdAt) {

    public static OrderEvent orderCreated(
            Long clienteId,
            Long productoId,
            int cantidad,
            BigDecimal total,
            String estado,
            String recipientEmail) {
        return new OrderEvent(
                UUID.randomUUID(),
                "ORDER_CREATED",
                clienteId,
                productoId,
                cantidad,
                total,
                estado,
                recipientEmail,
                "Pedido recibido en Pedidos360",
                "Tu pedido fue recibido y esta siendo procesado.",
                Instant.now());
    }

    public static OrderEvent notification(String recipientEmail, String subject, String message) {
        return new OrderEvent(
                UUID.randomUUID(),
                "NOTIFICATION_TEST",
                null,
                null,
                0,
                BigDecimal.ZERO,
                null,
                recipientEmail,
                subject,
                message,
                Instant.now());
    }
}
