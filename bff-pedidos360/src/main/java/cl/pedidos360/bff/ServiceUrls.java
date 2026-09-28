package cl.pedidos360.bff;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pedidos360.services")
public record ServiceUrls(
        String productosUrl,
        String clientesUrl,
        String pedidosUrl,
        String notificationsUrl,
        String rabbitAdminUrl,
        String auditoriaUrl) {
}
