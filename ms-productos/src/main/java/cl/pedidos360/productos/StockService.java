package cl.pedidos360.productos;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.pedidos360.messaging.OrderEvent;

@Service
public class StockService {
    private final ProductoRepository productoRepository;
    private final StockProcessedEventRepository processedEventRepository;

    public StockService(
            ProductoRepository productoRepository,
            StockProcessedEventRepository processedEventRepository) {
        this.productoRepository = productoRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void descontar(OrderEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        Producto producto = productoRepository.findById(event.productoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (event.cantidad() <= 0 || producto.getStock() < event.cantidad()) {
            throw new IllegalStateException("Stock insuficiente para procesar el pedido");
        }

        producto.setStock(producto.getStock() - event.cantidad());
        processedEventRepository.save(new StockProcessedEvent(event.eventId()));
    }
}
