package cl.pedidos360.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.pedidos360.streaming.KafkaTopics;
import cl.pedidos360.streaming.PaymentAttemptEvent;
import cl.pedidos360.streaming.ProductViewEvent;

@Component
public class AnalyticsConsumer {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private final ProductViewRepository views;
    private final PaymentAttemptRepository payments;

    public AnalyticsConsumer(ProductViewRepository views, PaymentAttemptRepository payments) {
        this.views = views;
        this.payments = payments;
    }

    @Transactional
    @KafkaListener(topics = KafkaTopics.PRODUCT_VIEWS, groupId = "analytics-product-views")
    public void productViewed(ProductViewEvent event) {
        if (!views.existsById(event.eventId())) {
            views.save(new ProductViewRecord(event));
        }
        log.info("Vista procesada. eventId={} productoId={}", event.eventId(), event.productoId());
    }

    @Transactional
    @KafkaListener(topics = KafkaTopics.PAYMENT_ATTEMPTS, groupId = "analytics-payment-attempts")
    public void paymentAttempted(PaymentAttemptEvent event) {
        if (!payments.existsById(event.eventId())) {
            payments.save(new PaymentAttemptRecord(event));
        }
        log.info("Intento de pago procesado. eventId={} estado={}", event.eventId(), event.estado());
    }
}
