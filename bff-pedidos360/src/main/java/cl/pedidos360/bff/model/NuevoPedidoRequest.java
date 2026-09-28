package cl.pedidos360.bff.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;

public record NuevoPedidoRequest(
        @NotNull Long clienteId,
        @NotNull Long productoId,
        @Positive int cantidad,
        @NotNull @Positive BigDecimal total,
        String estado,
        @Email String email) {
}
