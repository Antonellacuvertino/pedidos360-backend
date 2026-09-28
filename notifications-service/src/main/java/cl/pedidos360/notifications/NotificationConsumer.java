package cl.pedidos360.notifications;

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
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final EmailService emailService;

    public NotificationConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitNames.NOTIFICATIONS_QUEUE)
    public void procesar(OrderEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            emailService.enviar(event);
            channel.basicAck(deliveryTag, false);
            log.info("Notificacion procesada. eventId={}", event.eventId());
        } catch (Exception exception) {
            log.error("Notificacion enviada a DLQ. eventId={}", event.eventId(), exception);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
