package cl.pedidos360.productos;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import cl.pedidos360.streaming.KafkaTopics;
import cl.pedidos360.streaming.ProductViewEvent;

@Service
public class ProductViewPublisher {
    private static final Logger log = LoggerFactory.getLogger(ProductViewPublisher.class);

    private final KafkaTemplate<String, ProductViewEvent> kafka;
    private final boolean enabled;

    public ProductViewPublisher(
            KafkaTemplate<String, ProductViewEvent> kafka,
            @Value("${pedidos360.streaming.enabled:false}") boolean enabled) {
        this.kafka = kafka;
        this.enabled = enabled;
    }

    public void publicar(String usuarioId, Producto producto) {
        if (!enabled) {
            return;
        }
        ProductViewEvent event = new ProductViewEvent(
                UUID.randomUUID(), usuarioId, producto.getId(), producto.getCategoria(), Instant.now());
        kafka.send(KafkaTopics.PRODUCT_VIEWS, usuarioId, event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("No se publico vista de producto. eventId={}", event.eventId(), error);
                    }
                });
    }
}
