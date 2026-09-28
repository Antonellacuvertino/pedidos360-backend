package cl.pedidos360.bff.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @NotBlank @Email String destinatario,
        @NotBlank String asunto,
        @NotBlank String mensaje) {
}
