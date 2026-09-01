package cl.pedidos360.bff.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoDto(
        Long id,
        Long clienteId,
        Long productoId,
        int cantidad,
        BigDecimal total,
        String estado,
        LocalDateTime fechaCreacion) {
}
