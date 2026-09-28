package cl.pedidos360.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.pedidos360.messaging.OrderEvent;
import cl.pedidos360.messaging.RabbitNames;

@ExtendWith(MockitoExtension.class)
class PedidoPublisherTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publicaEventoYRespondeAceptado() {
        PedidoPublisher publisher = new PedidoPublisher(rabbitTemplate);
        NuevoPedidoRequest request = new NuevoPedidoRequest(
                1L, 2L, 3, BigDecimal.valueOf(45000), EstadoPedido.RECIBIDO, "cliente@ejemplo.cl");

        PedidoAceptado response = publisher.publicar(request);

        assertThat(response.eventoId()).isNotNull();
        assertThat(response.estado()).isEqualTo("ACEPTADO");
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitNames.ORDERS_EXCHANGE),
                eq(RabbitNames.ORDER_CREATED_KEY),
                any(OrderEvent.class));
    }
}
