package cl.pedidos360.analytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.pedidos360.streaming.PaymentAttemptEvent;
import cl.pedidos360.streaming.ProductViewEvent;

@ExtendWith(MockitoExtension.class)
class AnalyticsConsumerTest {
    @Mock ProductViewRepository views;
    @Mock PaymentAttemptRepository payments;
    @InjectMocks AnalyticsConsumer consumer;

    @Test
    void storesProductViewOnce() {
        ProductViewEvent event = new ProductViewEvent(UUID.randomUUID(), "user", 1L,
                "Computacion", Instant.now());
        when(views.existsById(event.eventId())).thenReturn(false);
        consumer.productViewed(event);
        verify(views).save(any(ProductViewRecord.class));
    }

    @Test
    void storesPaymentAttemptOnce() {
        PaymentAttemptEvent event = new PaymentAttemptEvent(UUID.randomUUID(), "client", "hash",
                BigDecimal.TEN, "BLOQUEADO", Instant.now());
        when(payments.existsById(event.eventId())).thenReturn(false);
        consumer.paymentAttempted(event);
        verify(payments).save(any(PaymentAttemptRecord.class));
    }
}
