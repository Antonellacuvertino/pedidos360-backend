package cl.pedidos360.bff.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NuevoClienteRequest(
        @NotBlank String nombre,
        @Email @NotBlank String email,
        @NotBlank String segmento) {
}
