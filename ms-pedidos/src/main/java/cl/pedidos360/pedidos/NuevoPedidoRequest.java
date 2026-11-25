package cl.pedidos360.pedidos;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NuevoPedidoRequest(
        @NotNull Long clienteId,
        @NotNull Long productoId,
        @Positive int cantidad,
        @NotNull @Positive BigDecimal total,
        EstadoPedido estado,
        @Email String email,
        String tarjetaPrueba) {
    public NuevoPedidoRequest(Long clienteId, Long productoId, int cantidad, BigDecimal total,
            EstadoPedido estado, String email) {
        this(clienteId, productoId, cantidad, total, estado, email, null);
    }
}
