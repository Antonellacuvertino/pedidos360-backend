package cl.pedidos360.kafkaadmin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.pedidos360.streaming.KafkaTopics;

import jakarta.annotation.PreDestroy;

@Service
public class KafkaAdminService {
    private final AdminClient client;

    public KafkaAdminService(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        client = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @PreDestroy
    void close() {
        client.close(Duration.ofSeconds(3));
    }

    public List<String> topics() throws Exception {
        return client.listTopics().names().get(10, TimeUnit.SECONDS).stream()
                .filter(name -> !name.startsWith("__")).sorted().toList();
    }

    public TopicDetails detail(String name) throws Exception {
        validateName(name);
        var topic = client.describeTopics(List.of(name)).allTopicNames().get(10, TimeUnit.SECONDS).get(name);
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        var config = client.describeConfigs(List.of(resource)).all().get(10, TimeUnit.SECONDS).get(resource);
        return new TopicDetails(name, topic.partitions().size(),
                topic.partitions().get(0).replicas().size(),
                config.get("retention.ms").value(), config.get("cleanup.policy").value(),
                config.get("min.insync.replicas").value());
    }

    public TopicDetails create(TopicCreateRequest request) throws Exception {
        validateName(request.name());
        if (request.replicationFactor() > 3) {
            throw new IllegalArgumentException("El laboratorio tiene tres brokers como maximo");
        }
        String policy = request.cleanupPolicy() == null ? "delete" : request.cleanupPolicy();
        NewTopic topic = new NewTopic(request.name(), request.partitions(), request.replicationFactor())
                .configs(Map.of("retention.ms", Long.toString(request.retentionMs()),
                        "cleanup.policy", policy,
                        "min.insync.replicas", request.replicationFactor() >= 2 ? "2" : "1"));
        client.createTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
        return detail(request.name());
    }

    public TopicDetails update(String name, TopicUpdateRequest request) throws Exception {
        validateMutableName(name);
        TopicDetails current = detail(name);
        if (request.partitions() != null) {
            if (request.partitions() < current.partitions()) {
                throw new IllegalArgumentException("Kafka no permite reducir particiones");
            }
            if (request.partitions() > current.partitions()) {
                client.createPartitions(Map.of(name, NewPartitions.increaseTo(request.partitions())))
                        .all().get(10, TimeUnit.SECONDS);
            }
        }
        List<AlterConfigOp> changes = new ArrayList<>();
        if (request.retentionMs() != null) {
            changes.add(set("retention.ms", request.retentionMs().toString()));
        }
        if (request.cleanupPolicy() != null) {
            changes.add(set("cleanup.policy", request.cleanupPolicy()));
        }
        if (request.minInSyncReplicas() != null) {
            if (request.minInSyncReplicas() > current.replicationFactor()) {
                throw new IllegalArgumentException("min.insync.replicas supera el factor de replica");
            }
            changes.add(set("min.insync.replicas", request.minInSyncReplicas().toString()));
        }
        if (!changes.isEmpty()) {
            client.incrementalAlterConfigs(Map.of(new ConfigResource(ConfigResource.Type.TOPIC, name), changes))
                    .all().get(10, TimeUnit.SECONDS);
        }
        return detail(name);
    }

    public void delete(String name) throws Exception {
        validateMutableName(name);
        client.deleteTopics(List.of(name)).all().get(10, TimeUnit.SECONDS);
    }

    public List<String> groups() throws Exception {
        return client.listConsumerGroups().all().get(10, TimeUnit.SECONDS).stream()
                .map(group -> group.groupId()).sorted().toList();
    }

    public List<GroupOffset> groupOffsets(String groupId) throws Exception {
        if (groupId == null || !groupId.matches("[A-Za-z0-9._-]{1,100}")) {
            throw new IllegalArgumentException("Identificador de grupo invalido");
        }
        return client.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
                .get(10, TimeUnit.SECONDS).entrySet().stream()
                .map(entry -> new GroupOffset(entry.getKey().topic(), entry.getKey().partition(),
                        entry.getValue().offset()))
                .sorted(Comparator.comparing(GroupOffset::topic).thenComparingInt(GroupOffset::partition))
                .toList();
    }

    private AlterConfigOp set(String key, String value) {
        return new AlterConfigOp(new ConfigEntry(key, value), AlterConfigOp.OpType.SET);
    }

    private void validateName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,99}") || name.startsWith("__")) {
            throw new IllegalArgumentException("Nombre de topico invalido o reservado");
        }
    }

    private void validateMutableName(String name) {
        validateName(name);
        if (List.of(KafkaTopics.PAYMENT_ATTEMPTS, KafkaTopics.PRODUCT_VIEWS,
                KafkaTopics.PAYMENT_ATTEMPTS_DLT, KafkaTopics.PRODUCT_VIEWS_DLT).contains(name)) {
            throw new IllegalArgumentException("El topico del sistema no se modifica desde esta API");
        }
    }

    public record TopicDetails(String name, int partitions, int replicationFactor,
            String retentionMs, String cleanupPolicy, String minInSyncReplicas) { }
    public record GroupOffset(String topic, int partition, long offset) { }
}
