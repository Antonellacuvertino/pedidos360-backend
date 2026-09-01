package cl.pedidos360.productos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ProductoRepositoryTest {
    @Autowired
    private ProductoRepository repository;

    @Test
    void guardaProducto() {
        Producto producto = repository.save(new Producto("Teclado", "Perifericos", BigDecimal.valueOf(39990), 15));

        assertThat(producto.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(1);
    }
}
