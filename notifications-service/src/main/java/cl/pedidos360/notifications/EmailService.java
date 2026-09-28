package cl.pedidos360.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import cl.pedidos360.messaging.OrderEvent;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailService(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void enviar(OrderEvent event) {
        String destinatario = hasText(event.recipientEmail())
                ? event.recipientEmail()
                : properties.defaultTo();
        if (!hasText(destinatario)) {
            throw new IllegalArgumentException("No existe un destinatario configurado");
        }

        if (!properties.smtpEnabled()) {
            log.info("Correo simulado. eventId={} destinatario={} asunto={}",
                    event.eventId(), destinatario, event.subject());
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(properties.from());
        email.setTo(destinatario);
        email.setSubject(event.subject());
        email.setText(event.message());
        mailSender.send(email);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
