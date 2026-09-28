package cl.pedidos360.auditoria;

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
public class AuditConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);

    private final AuditService service;

    public AuditConsumer(AuditService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitNames.AUDIT_QUEUE)
    public void procesar(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            service.registrar(event);
            channel.basicAck(deliveryTag, false);
            log.info("Evento auditado. eventId={}", event.eventId());
        } catch (Exception exception) {
            log.error("Auditoria enviada a DLQ. eventId={}", event.eventId(), exception);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
