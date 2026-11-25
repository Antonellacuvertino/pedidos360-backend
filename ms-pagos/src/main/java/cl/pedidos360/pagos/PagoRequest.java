package cl.pedidos360.pagos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PagoRequest(
        @NotBlank String clienteId,
        @NotBlank String pedidoId,
        @NotNull @Positive BigDecimal monto,
        @NotBlank String tarjetaPrueba) {
}
