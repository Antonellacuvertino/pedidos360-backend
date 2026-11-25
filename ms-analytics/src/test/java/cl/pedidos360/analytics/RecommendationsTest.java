package cl.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import cl.pedidos360.streaming.ProductViewEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RecommendationsTest {
    @Test
    void ranksAvailableComplementsWithoutRepeatingTheSelectedProduct() {
        var selected = new ProductViewRecord(
                new ProductViewEvent(UUID.randomUUID(), "user", 1L, "Computacion", Instant.now(), "CARRITO"));
        var result = RecommendationService.suggest(selected, List.of(
                product(1, "Computacion", 750000, 3),
                product(2, "Perifericos", 170000, 3),
                product(3, "Perifericos", 30000, 5),
                product(4, "Perifericos", 12000, 0),
                product(5, "Accesorios", 10000, 1)));
        assertThat(result.productoBaseId()).isEqualTo(1L);
        assertThat(result.productoIds()).containsExactly(3L, 2L, 5L);
    }

    @Test
    void noCartEventMeansNoSuggestionsAndNoCatalogRequest() {
        var service = new RecommendationService(mock(ProductViewRepository.class),
                RestClient.builder(), "http://localhost:1");
        var result = service.forUser("user", "unused", true);
        assertThat(result.productoBaseId()).isNull();
        assertThat(result.productoIds()).isEmpty();
    }

    @Test
    void viewsPreferSimilarProductsWithoutRepeatingTheViewedProduct() {
        var viewed = new ProductViewRecord(
                new ProductViewEvent(UUID.randomUUID(), "user", 2L, "Perifericos", Instant.now(), "VISTA"));
        var result = RecommendationService.suggest(viewed, List.of(
                product(2, "Perifericos", 170000, 3),
                product(3, "Perifericos", 30000, 5),
                product(5, "Accesorios", 10000, 1)));
        assertThat(result.accionBase()).isEqualTo("VISTA");
        assertThat(result.productoIds()).containsExactly(3L, 5L);
    }

    private RecommendationService.Product product(long id, String category, long price, int stock) {
        return new RecommendationService.Product(id, category, BigDecimal.valueOf(price), stock);
    }
}
