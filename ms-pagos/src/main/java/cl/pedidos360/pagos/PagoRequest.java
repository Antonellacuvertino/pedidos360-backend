package cl.pedidos360.pagos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

public record PagoRequest(
        @NotBlank String clienteId,
        @NotBlank String pedidoId,
        @NotNull @Positive BigDecimal monto,
        @NotBlank @Pattern(regexp = "demo_[A-Za-z0-9_-]{4,64}", message = "Use un token de prueba demo_, nunca una tarjeta real")
        String metodoPagoToken) {
}
