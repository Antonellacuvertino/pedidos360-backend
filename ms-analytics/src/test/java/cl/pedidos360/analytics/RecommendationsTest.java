package cl.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import cl.pedidos360.streaming.ProductViewEvent;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class RecommendationsTest {
    @Test
    void suggestsUnseenCategoryProductsAndFallsBackToLastSelection() {
        ProductViewRepository views = mock(ProductViewRepository.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("oid")).thenReturn("user");
        when(views.findFirstByUsuarioIdOrderByFechaDesc("user")).thenReturn(
            new ProductViewRecord(new ProductViewEvent(UUID.randomUUID(), "user", 1L, "Redes", Instant.now(), "CARRITO")));
        when(views.viewedBy("user")).thenReturn(List.of(1L));
        when(views.popularInCategory(eq("Redes"), any())).thenReturn(List.of(1L, 2L));
        AnalyticsController controller = new AnalyticsController(views, mock(PaymentAttemptRepository.class));
        assertThat(controller.recomendaciones(jwt).productoIds()).containsExactly(2L);
        when(views.popularInCategory(eq("Redes"), any())).thenReturn(List.of(1L));
        assertThat(controller.recomendaciones(jwt).productoIds()).containsExactly(1L);
    }
}
