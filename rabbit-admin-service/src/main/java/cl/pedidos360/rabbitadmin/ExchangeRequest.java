package cl.pedidos360.rabbitadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$", message = "nombre de exchange invalido")
        String name,
        @NotBlank
        @Pattern(regexp = "^(direct|topic)$", message = "el tipo debe ser direct o topic")
        String type) {
}
