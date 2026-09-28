package cl.pedidos360.notifications;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;

import cl.pedidos360.messaging.OrderEvent;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {
    @Mock
    private EmailService emailService;
    @Mock
    private Channel channel;

    @Test
    void confirmaConAckCuandoElCorreoSeProcesa() throws Exception {
        OrderEvent event = event();
        Message message = message(10L);

        new NotificationConsumer(emailService).procesar(event, message, channel);

        verify(channel).basicAck(10L, false);
    }

    @Test
    void rechazaSinRequeueCuandoElCorreoFalla() throws Exception {
        OrderEvent event = event();
        Message message = message(11L);
        doThrow(new IllegalStateException("SMTP no disponible")).when(emailService).enviar(event);

        new NotificationConsumer(emailService).procesar(event, message, channel);

        verify(channel).basicNack(11L, false, false);
    }

    private OrderEvent event() {
        return new OrderEvent(
                UUID.randomUUID(), "ORDER_CREATED", 1L, 1L, 1, BigDecimal.TEN,
                "RECIBIDO", "cliente@ejemplo.cl", "Pedido", "Mensaje", Instant.now());
    }

    private Message message(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }
}
