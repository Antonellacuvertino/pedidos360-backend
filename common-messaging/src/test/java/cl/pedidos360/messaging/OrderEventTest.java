package cl.pedidos360.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderEventTest {

    @Test
    void correoDePedidoIncluyeReferenciaYDetalle() {
        OrderEvent event = OrderEvent.orderCreated(
                7L, 12L, 2, new BigDecimal("15990"), "RECIBIDO", "cliente@ejemplo.cl");

        assertThat(event.recipientEmail()).isEqualTo("cliente@ejemplo.cl");
        assertThat(event.subject()).contains(event.eventId().toString());
        assertThat(event.message()).contains(
                event.eventId().toString(), "Cliente: 7", "Producto: 12",
                "Cantidad: 2", "CLP 15990", "no acredita un pago");
    }
}
