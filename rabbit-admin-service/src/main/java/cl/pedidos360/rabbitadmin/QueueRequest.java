package cl.pedidos360.rabbitadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QueueRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$", message = "use letras minusculas, numeros, punto, guion o guion bajo")
        String name,
        Boolean durable,
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$", message = "routing key invalida")
        String deadLetterRoutingKey) {
}
