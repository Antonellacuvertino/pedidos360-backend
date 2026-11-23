package cl.pedidos360.analytics;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductViewRepository extends JpaRepository<ProductViewRecord, UUID> {
    ProductViewRecord findFirstByUsuarioIdOrderByFechaDesc(String usuarioId);

    @Query("select distinct v.productoId from ProductViewRecord v where v.usuarioId = ?1")
    List<Long> viewedBy(String usuarioId);

    @Query("select v.productoId from ProductViewRecord v where v.categoria = ?1 "
            + "group by v.productoId order by count(v.id) desc")
    List<Long> popularInCategory(String categoria, Pageable pageable);
}
