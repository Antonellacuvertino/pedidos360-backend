package cl.pedidos360.analytics;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductViewRepository extends JpaRepository<ProductViewRecord, UUID> {
    ProductViewRecord findFirstByUsuarioIdAndAccionOrderByFechaDesc(String usuarioId, String accion);
}
