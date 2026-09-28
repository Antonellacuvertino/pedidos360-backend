package cl.pedidos360.productos;

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
public class StockEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(StockEventConsumer.class);

    private final StockService service;

    public StockEventConsumer(StockService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitNames.STOCK_QUEUE)
    public void procesar(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            service.descontar(event);
            channel.basicAck(deliveryTag, false);
            log.info("Stock actualizado. eventId={} productoId={}", event.eventId(), event.productoId());
        } catch (Exception exception) {
            log.error("Actualizacion de stock enviada a DLQ. eventId={}", event.eventId(), exception);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
