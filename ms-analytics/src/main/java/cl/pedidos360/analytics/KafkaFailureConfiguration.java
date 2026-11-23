package cl.pedidos360.analytics;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaFailureConfiguration {
    private static final Logger log = LoggerFactory.getLogger(KafkaFailureConfiguration.class);

    @Bean
    DefaultErrorHandler kafkaErrorHandler(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> settings = new HashMap<>();
        settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        settings.put(ProducerConfig.ACKS_CONFIG, "all");
        settings.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        Map<Class<?>, Serializer<?>> serializers = new HashMap<>();
        serializers.put(byte[].class, new ByteArraySerializer());
        serializers.put(Object.class, new JsonSerializer<>());
        var factory = new DefaultKafkaProducerFactory<>(settings, new StringSerializer(),
                new DelegatingByTypeSerializer(serializers, true));
        var template = new KafkaTemplate<>(factory);
        var recoverer = new DeadLetterPublishingRecoverer(template, (record, exception) -> {
            log.error("Evento enviado a DLT. topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset(), exception);
            return new TopicPartition(record.topic() + "-dlt", record.partition());
        });
        recoverer.setFailIfSendResultIsError(true);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        handler.setRetryListeners((record, exception, attempt) -> log.warn(
                "Reintento Kafka {}. topic={} offset={}", attempt, record.topic(), record.offset()));
        return handler;
    }
}
