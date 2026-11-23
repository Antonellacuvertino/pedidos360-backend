package cl.pedidos360.pagos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.ElementCollection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PaymentVelocity {
    @Id
    private String fingerprint;
    private int attempts;
    private Instant windowStartedAt;
    private Instant blockedUntil;
    @ElementCollection
    private List<Instant> recentAttempts = new ArrayList<>();

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
        if (blockedUntil != null) {
            recentAttempts.clear();
            blockedUntil = null;
        }
        recentAttempts.removeIf(attempt -> !attempt.isAfter(now.minusSeconds(10)));
        recentAttempts.add(now);
        attempts = recentAttempts.size();
        windowStartedAt = recentAttempts.get(0);
        if (attempts >= 3) {
            blockedUntil = now.plusSeconds(1800);
            return false;
        }
        return true;
    }
}
