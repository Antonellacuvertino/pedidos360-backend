package cl.pedidos360.notifications;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pedidos360.notifications")
public record NotificationProperties(boolean smtpEnabled, String defaultTo, String from) {
}
