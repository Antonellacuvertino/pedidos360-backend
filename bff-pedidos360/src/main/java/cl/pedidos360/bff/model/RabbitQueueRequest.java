package cl.pedidos360.bff.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RabbitQueueRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$")
        String name,
        Boolean durable,
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$")
        String deadLetterRoutingKey) {
}
