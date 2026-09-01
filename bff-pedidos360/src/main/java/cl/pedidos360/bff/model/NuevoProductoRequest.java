package cl.pedidos360.bff.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record NuevoProductoRequest(
        @NotBlank String nombre,
        @NotBlank String categoria,
        @NotNull @Positive BigDecimal precio,
        @PositiveOrZero int stock) {
}
