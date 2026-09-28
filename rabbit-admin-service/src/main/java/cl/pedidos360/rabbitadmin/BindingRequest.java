package cl.pedidos360.rabbitadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BindingRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$")
        String queueName,
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$")
        String exchangeName,
        @NotBlank String routingKey,
        @NotBlank
        @Pattern(regexp = "^(direct|topic)$")
        String exchangeType) {
}
