package cl.pedidos360.kafkaadmin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TopicCreateRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,99}") String name,
        @Min(1) int partitions,
        @Min(1) short replicationFactor,
        @Min(1) long retentionMs,
        @Pattern(regexp = "delete|compact") String cleanupPolicy) {
}
