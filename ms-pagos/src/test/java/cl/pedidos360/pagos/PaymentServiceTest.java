package cl.pedidos360.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import cl.pedidos360.streaming.KafkaTopics;
import cl.pedidos360.streaming.PaymentAttemptEvent;

class PaymentServiceTest {
    @Test
    void changingClientOrCardDoesNotBypassAuthenticatedUserBlock() {
        PaymentVelocityRepository repository = mock(PaymentVelocityRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, PaymentAttemptEvent> kafka = mock(KafkaTemplate.class);
        PaymentVelocity velocity = new PaymentVelocity("test");
        when(repository.findById(anyString())).thenReturn(Optional.of(velocity));
        PaymentService service = new PaymentService(repository, kafka, "test-pepper", false);
        PagoRequest first = new PagoRequest("client-a", "order-1", BigDecimal.TEN, "demo_card1");
        PagoRequest changed = new PagoRequest("client-b", "order-2", BigDecimal.TEN, "demo_card2");
        assertThat(service.simular(first, "same-user").estado()).isEqualTo("APROBADO_SIMULADO");
        assertThat(service.simular(changed, "same-user").estado()).isEqualTo("APROBADO_SIMULADO");
        assertThat(service.simular(changed, "same-user").estado()).isEqualTo("BLOQUEADO");
        assertThat(service.estado("same-user").bloqueado()).isTrue();
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(3)).ensureExists(keys.capture());
        assertThat(keys.getAllValues()).containsOnly(keys.getValue());
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
                BigDecimal.valueOf(2000), "demo_tarjeta01"), "usuario-autenticado");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaymentAttemptEvent> event = ArgumentCaptor.forClass(PaymentAttemptEvent.class);
        verify(kafka).send(eq(KafkaTopics.PAYMENT_ATTEMPTS), key.capture(), event.capture());
        assertThat(response.estado()).isEqualTo("APROBADO_SIMULADO");
        assertThat(key.getValue()).hasSize(64).doesNotContain("tarjeta01");
        assertThat(event.getValue().tarjetaHuella()).hasSize(64).isNotEqualTo(key.getValue());
        assertThat(event.getValue().clienteId()).isEqualTo("usuario-autenticado");
    }
}
