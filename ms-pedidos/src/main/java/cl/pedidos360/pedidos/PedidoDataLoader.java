package cl.pedidos360.pedidos;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PedidoDataLoader {
    @Bean
    CommandLineRunner cargarPedidos(PedidoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Pedido(1L, 1L, 2, BigDecimal.valueOf(1499980), EstadoPedido.PREPARACION));
                repository.save(new Pedido(2L, 3L, 1, BigDecimal.valueOf(69990), EstadoPedido.DESPACHADO));
                repository.save(new Pedido(3L, 2L, 4, BigDecimal.valueOf(759960), EstadoPedido.RECIBIDO));
            }
        };
    }
}
