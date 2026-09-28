package cl.pedidos360.productos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.pedidos360.messaging.OrderEvent;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private StockProcessedEventRepository processedEventRepository;

    @Test
    void descuentaStockYRegistraEvento() {
        UUID eventId = UUID.randomUUID();
        Producto producto = new Producto("Monitor", "Perifericos", BigDecimal.valueOf(100000), 10);
        OrderEvent event = new OrderEvent(
                eventId, "ORDER_CREATED", 1L, 2L, 3, BigDecimal.valueOf(300000),
                "RECIBIDO", "cliente@ejemplo.cl", "Pedido", "Mensaje", Instant.now());
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));

        new StockService(productoRepository, processedEventRepository).descontar(event);

        assertThat(producto.getStock()).isEqualTo(7);
        verify(processedEventRepository).save(org.mockito.ArgumentMatchers.any(StockProcessedEvent.class));
    }
}
