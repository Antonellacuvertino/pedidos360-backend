package cl.pedidos360.pagos;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PaymentVelocity {
    @Id
    private String fingerprint;
    private int attempts;
    private Instant windowStartedAt;
    private Instant blockedUntil;

    protected PaymentVelocity() {
    }

    public PaymentVelocity(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getFingerprint() { return fingerprint; }
    public int getAttempts() { return attempts; }
    public Instant getWindowStartedAt() { return windowStartedAt; }
    public Instant getBlockedUntil() { return blockedUntil; }

    public boolean registerAttempt(Instant now) {
        if (blockedUntil != null && now.isBefore(blockedUntil)) {
            return false;
        }
        if (windowStartedAt == null || !now.isBefore(windowStartedAt.plusSeconds(10))) {
            windowStartedAt = now;
            attempts = 0;
            blockedUntil = null;
        }
        attempts++;
        if (attempts >= 3) {
            blockedUntil = now.plusSeconds(1800);
            return false;
        }
        return true;
    }
}
