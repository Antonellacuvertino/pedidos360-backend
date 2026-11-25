package cl.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import cl.pedidos360.streaming.ProductViewEvent;

@DataJpaTest
class ProductViewRepositoryTest {
    @Autowired
    private ProductViewRepository repository;

    @Test
    void ignoresLaterViewsAndOtherUsersWhenSelectingCartContext() {
        repository.save(new ProductViewRecord(event("user-1", 1L, 1, "CARRITO")));
        repository.save(new ProductViewRecord(event("user-1", 2L, 2, "CARRITO")));
        repository.save(new ProductViewRecord(event("user-1", 3L, 3, "VISTA")));
        repository.save(new ProductViewRecord(event("user-2", 4L, 4, "CARRITO")));

        assertThat(repository.findFirstByUsuarioIdAndAccionOrderByFechaDesc("user-1", "CARRITO")
                .getProductoId()).isEqualTo(2L);
    }

    private ProductViewEvent event(String user, Long product, int second, String action) {
        return new ProductViewEvent(UUID.randomUUID(), user, product, "Redes",
                Instant.parse("2026-11-16T10:00:00Z").plusSeconds(second), action);
    }
}
