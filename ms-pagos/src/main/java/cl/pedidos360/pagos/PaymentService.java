package cl.pedidos360.pagos;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
        var tarjeta = TarjetaPrueba.validar(request.tarjetaPrueba());
        Instant now = Instant.now();
        String fingerprint = fingerprint(tarjeta);
        repository.ensureExists(fingerprint);
        PaymentVelocity velocity = repository.findById(fingerprint)
                .orElseGet(() -> new PaymentVelocity(fingerprint));
        boolean accepted = velocity.registerAttempt(now);
        repository.save(velocity);

        String status = !accepted ? "BLOQUEADO" : tarjeta.rechazada() ? "RECHAZADO_SIMULADO" : "APROBADO_SIMULADO";
        UUID eventId = UUID.randomUUID();
        PaymentAttemptEvent event = new PaymentAttemptEvent(
                eventId, usuarioId, fingerprint, request.monto(), status, now);
        if (streamingEnabled) {
            kafka.send(KafkaTopics.PAYMENT_ATTEMPTS, fingerprint, event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("No se publico intento de pago. eventId={}", eventId, error);
                        }
                    });
        }
        if (!accepted) {
            log.warn("Intento de pago bloqueado. eventId={}", eventId);
        }
        String mensaje = switch (status) {
            case "BLOQUEADO" -> "Tarjeta de prueba bloqueada por 30 minutos";
            case "RECHAZADO_SIMULADO" -> "Pago de prueba rechazado";
            default -> "Pago de prueba aprobado; no se cobro dinero";
        };
        return new PagoResponse(eventId, status, mensaje, now);
    }

    @Transactional
    public FraudStatus estado(String numero) {
        Instant until = repository.findById(fingerprint(TarjetaPrueba.validar(numero)))
                .map(PaymentVelocity::getBlockedUntil).orElse(null);
        return new FraudStatus(until != null && Instant.now().isBefore(until), until);
    }

    public record FraudStatus(boolean bloqueado, Instant bloqueadoHasta) { }

    private String fingerprint(TarjetaPrueba tarjeta) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("tarjeta-demo:v1:" + tarjeta.numero()).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 no disponible", exception);
        }
    }
}
