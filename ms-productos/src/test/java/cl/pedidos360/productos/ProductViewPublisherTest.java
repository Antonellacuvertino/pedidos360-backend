package cl.pedidos360.productos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import cl.pedidos360.streaming.KafkaTopics;
import cl.pedidos360.streaming.ProductViewEvent;

import static org.mockito.Mockito.mock;

class ProductViewPublisherTest {
    @Test
    void sendsTypedViewUsingUserAsKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, ProductViewEvent> kafka = mock(KafkaTemplate.class);
        when(kafka.send(eq(KafkaTopics.PRODUCT_VIEWS), eq("user-1"), any(ProductViewEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        Producto product = new Producto("Router", "Redes", BigDecimal.TEN, 2);
        ReflectionTestUtils.setField(product, "id", 7L);

        new ProductViewPublisher(kafka, true).publicar("user-1", product);

        ArgumentCaptor<ProductViewEvent> event = ArgumentCaptor.forClass(ProductViewEvent.class);
        verify(kafka).send(eq(KafkaTopics.PRODUCT_VIEWS), eq("user-1"), event.capture());
        assertThat(event.getValue().productoId()).isEqualTo(7L);
        assertThat(event.getValue().categoria()).isEqualTo("Redes");
    }
}
