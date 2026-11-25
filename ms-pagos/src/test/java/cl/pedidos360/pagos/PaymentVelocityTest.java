package cl.pedidos360.pagos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PaymentVelocityTest {
    @Test
    void countsSlidingWindowAcrossOriginalBoundary() {
        PaymentVelocity velocity = new PaymentVelocity("fingerprint");
        Instant start = Instant.parse("2026-11-16T10:00:00Z");
        assertTrue(velocity.registerAttempt(start));
        assertTrue(velocity.registerAttempt(start.plusSeconds(9)));
        assertTrue(velocity.registerAttempt(start.plusSeconds(10)));
        assertFalse(velocity.registerAttempt(start.plusSeconds(11)));
    }
    @Test
    void blocksThirdAttemptAndResetsAfterOneMinute() {
        PaymentVelocity velocity = new PaymentVelocity("fingerprint");
        Instant start = Instant.parse("2026-11-16T10:00:00Z");

        assertTrue(velocity.registerAttempt(start));
        assertTrue(velocity.registerAttempt(start.plusSeconds(1)));
        assertFalse(velocity.registerAttempt(start.plusSeconds(2)));
        assertFalse(velocity.registerAttempt(start.plusSeconds(3)));
        assertFalse(velocity.registerAttempt(start.plusSeconds(61)));
        assertTrue(velocity.registerAttempt(start.plusSeconds(62)));
    }

    @Test
    void startsNewWindowAfterTenSeconds() {
        PaymentVelocity velocity = new PaymentVelocity("fingerprint");
        Instant start = Instant.parse("2026-11-16T10:00:00Z");
        assertTrue(velocity.registerAttempt(start));
        assertTrue(velocity.registerAttempt(start.plusSeconds(10)));
        assertTrue(velocity.registerAttempt(start.plusSeconds(11)));
        assertFalse(velocity.registerAttempt(start.plusSeconds(12)));
    }
}
