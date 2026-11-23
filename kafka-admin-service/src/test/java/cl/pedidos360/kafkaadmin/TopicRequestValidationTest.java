package cl.pedidos360.kafkaadmin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;

class TopicRequestValidationTest {
    @Test
    void acceptsThreePartitionsAndThreeReplicas() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new TopicCreateRequest("payment-attempts", 3, (short) 3, 604800000L, "delete");
            assertThat(validator.validate(request)).isEmpty();
        }
    }

    @Test
    void rejectsMissingReplicationAndInvalidName() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new TopicCreateRequest("bad name", 3, (short) 0, 0L, "delete");
            assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(3);
        }
    }
}
