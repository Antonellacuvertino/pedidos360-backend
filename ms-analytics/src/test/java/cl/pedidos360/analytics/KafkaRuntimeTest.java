package cl.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import cl.pedidos360.streaming.*;

@EnabledIfEnvironmentVariable(named = "KAFKA_RUNTIME_TEST", matches = "true")
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092,localhost:9093,localhost:9094",
    "spring.kafka.listener.auto-startup=true",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.enable-auto-commit=false",
    "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
    "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
    "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=cl.pedidos360.streaming",
    "spring.kafka.listener.ack-mode=record"
})
class KafkaRuntimeTest {
    @Autowired ProductViewRepository views;
    @Autowired PaymentAttemptRepository payments;
    @MockBean JwtDecoder jwtDecoder;

    @Test
    void invalidEventReachesDltAndConsumerContinues() throws Exception {
        String key = "invalid-validation-" + UUID.randomUUID();
        byte[] invalid = ("not-json-" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (var producer = new KafkaProducer<String, byte[]>(Map.of(
                "bootstrap.servers", "localhost:9092", "acks", "all"),
                new StringSerializer(), new org.apache.kafka.common.serialization.ByteArraySerializer());
             var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]>(Map.of(
                "bootstrap.servers", "localhost:9092", "group.id", key,
                "auto.offset.reset", "earliest", "enable.auto.commit", false),
                new org.apache.kafka.common.serialization.StringDeserializer(),
                new org.apache.kafka.common.serialization.ByteArrayDeserializer())) {
            consumer.subscribe(java.util.List.of(KafkaTopics.PRODUCT_VIEWS_DLT));
            producer.send(new ProducerRecord<>(KafkaTopics.PRODUCT_VIEWS, 0, key, invalid))
                    .get(35, TimeUnit.SECONDS);
            boolean received = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
            while (!received && System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofSeconds(1))) {
                    if (key.equals(record.key())) {
                        assertThat(record.value()).isEqualTo(invalid);
                        received = true;
                    }
                }
            }
            assertThat(received).as("Evento invalido recibido en DLT").isTrue();
        }
        ProductViewEvent valid = new ProductViewEvent(UUID.randomUUID(), key, 124L, key, Instant.now());
        try (var producer = new KafkaProducer<String, ProductViewEvent>(Map.of(
                "bootstrap.servers", "localhost:9092", "acks", "all"), new StringSerializer(), new JsonSerializer<>())) {
            producer.send(new ProducerRecord<>(KafkaTopics.PRODUCT_VIEWS, 0, key, valid)).get(35, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> assertThat(views.existsById(valid.eventId())).isTrue());
    }

    @Test
    void realKafkaEventsReachRepositoriesAndRecommendations() throws Exception {
        String user = "validation-" + UUID.randomUUID();
        ProductViewEvent view = new ProductViewEvent(UUID.randomUUID(), user, 123L, user, Instant.now(), "CARRITO");
        PaymentAttemptEvent payment = new PaymentAttemptEvent(UUID.randomUUID(), user, "test-fingerprint",
                BigDecimal.TEN, "BLOQUEADO", Instant.now());
        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(Map.of(
                "bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094",
                "acks", "all", "delivery.timeout.ms", 30000, "request.timeout.ms", 10000),
                new StringSerializer(), new JsonSerializer<>())) {
            producer.send(new ProducerRecord<>(KafkaTopics.PRODUCT_VIEWS, user, view)).get(35, TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>(KafkaTopics.PAYMENT_ATTEMPTS, user, payment)).get(35, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
            assertThat(views.existsById(view.eventId())).isTrue();
            assertThat(payments.existsById(payment.eventId())).isTrue();
        });
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test")
                .header("alg", "none").claim("oid", user).build();
        assertThat(new AnalyticsController(views, payments).recomendaciones(jwt).productoIds()).contains(123L);
    }
}
