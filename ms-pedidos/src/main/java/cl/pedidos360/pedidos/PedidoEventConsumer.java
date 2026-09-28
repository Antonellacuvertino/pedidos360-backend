package cl.pedidos360.pedidos;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.pedidos360.messaging.OrderEvent;
import cl.pedidos360.messaging.RabbitNames;

@Component
public class PedidoEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PedidoEventConsumer.class);

    private final PedidoEventService service;

    public PedidoEventConsumer(PedidoEventService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitNames.ORDERS_QUEUE)
    public void procesar(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            service.guardarSiEsNuevo(event);
            channel.basicAck(deliveryTag, false);
            log.info("Pedido procesado. eventId={}", event.eventId());
        } catch (Exception exception) {
            log.error("Pedido enviado a DLQ. eventId={}", event.eventId(), exception);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
