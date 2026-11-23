package cl.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import cl.pedidos360.streaming.ProductViewEvent;

@DataJpaTest
class ProductViewRepositoryTest {
    @Autowired
    private ProductViewRepository repository;

    @Test
    void findsLastCategoryAndPopularProducts() {
        repository.save(new ProductViewRecord(event("user-1", 1L, "Redes", 1)));
        repository.save(new ProductViewRecord(event("user-2", 2L, "Redes", 2)));
        repository.save(new ProductViewRecord(event("user-3", 2L, "Redes", 3)));

        assertThat(repository.findFirstByUsuarioIdOrderByFechaDesc("user-1").getCategoria())
                .isEqualTo("Redes");
        assertThat(repository.popularInCategory("Redes", PageRequest.of(0, 5)))
                .startsWith(2L);
        assertThat(repository.viewedBy("user-1")).containsExactly(1L);
    }

    private ProductViewEvent event(String user, Long product, String category, int second) {
        return new ProductViewEvent(UUID.randomUUID(), user, product, category,
                Instant.parse("2026-11-16T10:00:00Z").plusSeconds(second));
    }
}
