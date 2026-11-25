package cl.pedidos360.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import cl.pedidos360.streaming.KafkaTopics;
import cl.pedidos360.streaming.PaymentAttemptEvent;

class PaymentServiceTest {
    @Test
    void fourthAttemptBlocksTheSameCardAcrossUsersButNotOtherCards() {
        PaymentVelocityRepository repository = mock(PaymentVelocityRepository.class);
        var cards = new java.util.HashMap<String, PaymentVelocity>();
        when(repository.findById(anyString())).thenAnswer(call -> Optional.ofNullable(cards.get(call.getArgument(0))));
        when(repository.save(any(PaymentVelocity.class))).thenAnswer(call -> {
            PaymentVelocity velocity = call.getArgument(0);
            cards.put(velocity.getFingerprint(), velocity);
            return velocity;
        });
        PaymentService service = new PaymentService(repository, null, "test-pepper", false);
        PagoRequest first = new PagoRequest("client-a", "order-1", BigDecimal.TEN, "0000000000000002");
        PagoRequest changed = new PagoRequest("client-b", "order-2", BigDecimal.TEN, "0000000000000002");
        for (int i = 0; i < 3; i++) {
            assertThat(service.simular(first, "user-a").estado()).isEqualTo("RECHAZADO_SIMULADO");
        }
        assertThat(service.simular(changed, "user-b").estado()).isEqualTo("BLOQUEADO");
        assertThat(service.estado(first.tarjetaPrueba()).bloqueado()).isTrue();
        var other = new PagoRequest("client-a", "order-3", BigDecimal.TEN, "0000000000000001");
        assertThat(service.simular(other, "user-a").estado()).isEqualTo("APROBADO_SIMULADO");
        assertThat(service.estado(other.tarjetaPrueba()).bloqueado()).isFalse();
        assertThat(cards).hasSize(2);
    }

    @Test
    void rejectsNonDemoNumbersBeforeRecordingAttempts() {
        var repository = mock(PaymentVelocityRepository.class);
        var service = new PaymentService(repository, null, "test-pepper", false);
        assertThatThrownBy(() -> service.simular(new PagoRequest("a", "b", BigDecimal.ONE, "1234"), "user"))
                .isInstanceOf(ResponseStatusException.class);
        org.mockito.Mockito.verifyNoInteractions(repository);
    }
    @Test
    void publishesHashKeyWithoutExposingTestToken() {
        PaymentVelocityRepository repository = mock(PaymentVelocityRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, PaymentAttemptEvent> kafka = mock(KafkaTemplate.class);
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(kafka.send(eq(KafkaTopics.PAYMENT_ATTEMPTS), anyString(), any(PaymentAttemptEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        PaymentService service = new PaymentService(repository, kafka, "test-pepper-value", true);

        PagoResponse response = service.simular(new PagoRequest("client-1", "order-1",
                BigDecimal.valueOf(2000), "0000000000000001"), "usuario-autenticado");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaymentAttemptEvent> event = ArgumentCaptor.forClass(PaymentAttemptEvent.class);
        verify(kafka).send(eq(KafkaTopics.PAYMENT_ATTEMPTS), key.capture(), event.capture());
        assertThat(response.estado()).isEqualTo("APROBADO_SIMULADO");
        assertThat(key.getValue()).hasSize(64).doesNotContain("0000000000000001");
        assertThat(event.getValue().tarjetaHuella()).isEqualTo(key.getValue());
        assertThat(event.getValue().clienteId()).isEqualTo("usuario-autenticado");
    }
}
