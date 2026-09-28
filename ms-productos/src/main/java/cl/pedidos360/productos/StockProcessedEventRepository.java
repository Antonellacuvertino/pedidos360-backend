package cl.pedidos360.productos;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockProcessedEventRepository extends JpaRepository<StockProcessedEvent, UUID> {
}
