package cl.pedidos360.kafkaadmin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record TopicUpdateRequest(
        @Min(1) Integer partitions,
        @Min(1) Long retentionMs,
        @Pattern(regexp = "delete|compact") String cleanupPolicy,
        @Min(1) Integer minInSyncReplicas) {
}
