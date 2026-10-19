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
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        return new OrderEvent(
                eventId,
                "ORDER_CREATED",
                clienteId,
                productoId,
                cantidad,
                total,
                estado,
                recipientEmail,
                "Pedidos360 - confirmacion " + eventId,
                ("Comprobante de pedido (no tributario)\n"
                        + "Referencia: %s\nFecha: %s\nCliente: %s\nProducto: %s\n"
                        + "Cantidad: %d\nTotal referencial: CLP %s\nEstado: %s\n\n"
                        + "Tu pedido fue recibido y se esta procesando. "
                        + "Este comprobante no acredita un pago ni reemplaza una boleta.\n")
                        .formatted(eventId, createdAt, clienteId, productoId, cantidad, total.toPlainString(), estado),
                createdAt);
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
