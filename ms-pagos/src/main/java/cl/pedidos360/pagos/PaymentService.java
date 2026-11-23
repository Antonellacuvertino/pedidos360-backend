package cl.pedidos360.pagos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.pedidos360.streaming.KafkaTopics;
import cl.pedidos360.streaming.PaymentAttemptEvent;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentVelocityRepository repository;
    private final KafkaTemplate<String, PaymentAttemptEvent> kafka;
    private final String pepper;
    private final boolean streamingEnabled;

    public PaymentService(
            PaymentVelocityRepository repository,
            KafkaTemplate<String, PaymentAttemptEvent> kafka,
            @Value("${pedidos360.payments.fingerprint-pepper}") String pepper,
            @Value("${pedidos360.streaming.enabled:false}") boolean streamingEnabled) {
        this.repository = repository;
        this.kafka = kafka;
        this.pepper = pepper;
        this.streamingEnabled = streamingEnabled;
    }

    @Transactional
    public PagoResponse simular(PagoRequest request, String usuarioId) {
        Instant now = Instant.now();
        String fingerprint = fingerprint("usuario:" + usuarioId);
        repository.ensureExists(fingerprint);
        PaymentVelocity velocity = repository.findById(fingerprint)
                .orElseGet(() -> new PaymentVelocity(fingerprint));
        boolean accepted = velocity.registerAttempt(now);
        repository.save(velocity);

        String status = accepted ? "APROBADO_SIMULADO" : "BLOQUEADO";
        UUID eventId = UUID.randomUUID();
        PaymentAttemptEvent event = new PaymentAttemptEvent(
                eventId, usuarioId, fingerprint(request.metodoPagoToken()), request.monto(), status, now);
        if (streamingEnabled) {
            kafka.send(KafkaTopics.PAYMENT_ATTEMPTS, fingerprint, event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("No se publico intento de pago. eventId={}", eventId, error);
                        }
                    });
        }
        if (!accepted) {
            log.warn("Intento de pago bloqueado. eventId={} clienteId={}", eventId, request.clienteId());
        }
        return new PagoResponse(eventId, status,
                accepted ? "Pago de prueba aprobado; no se cobro dinero" : "Usuario bloqueado por 30 minutos", now);
    }

    @Transactional
    public FraudStatus estado(String usuarioId) {
        Instant until = repository.findById(fingerprint("usuario:" + usuarioId))
                .map(PaymentVelocity::getBlockedUntil).orElse(null);
        return new FraudStatus(until != null && Instant.now().isBefore(until), until);
    }

    public record FraudStatus(boolean bloqueado, Instant bloqueadoHasta) { }

    private String fingerprint(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((pepper + ":" + token).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }
}
