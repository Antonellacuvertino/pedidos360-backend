package cl.pedidos360.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PedidoRepositoryTest {
    @Autowired
    private PedidoRepository repository;

    @Test
    void guardaPedido() {
        Pedido pedido = repository.save(new Pedido(1L, 2L, 3, BigDecimal.valueOf(15000), EstadoPedido.RECIBIDO));

        assertThat(pedido.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(1);
    }
}
