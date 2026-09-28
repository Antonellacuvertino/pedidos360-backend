package cl.pedidos360.notifications;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import cl.pedidos360.messaging.OrderEvent;
import cl.pedidos360.messaging.RabbitNames;

@Service
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public NotificationAccepted publicar(EmailRequest request) {
        OrderEvent event = OrderEvent.notification(
                request.destinatario(),
                request.asunto(),
                request.mensaje());
        rabbitTemplate.convertAndSend(
                RabbitNames.NOTIFICATIONS_EXCHANGE,
                RabbitNames.NOTIFICATION_EMAIL_KEY,
                event);
        return new NotificationAccepted(event.eventId(), "ACEPTADA");
    }
}
