package cl.pedidos360.pedidos;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    boolean existsByEventoId(UUID eventoId);
}
