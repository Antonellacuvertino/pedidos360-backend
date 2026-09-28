package cl.pedidos360.rabbitadmin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import cl.pedidos360.messaging.RabbitNames;

@Service
public class RabbitManagementService {
    private final RabbitAdmin rabbitAdmin;
    private final Set<String> knownQueues = ConcurrentHashMap.newKeySet();

    public RabbitManagementService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
        this.knownQueues.addAll(RabbitNames.PROTECTED_QUEUES);
    }

    public java.util.List<QueueStatus> listarColas() {
        java.util.List<QueueStatus> result = new ArrayList<>();
        for (String name : knownQueues) {
            Properties properties = rabbitAdmin.getQueueProperties(name);
            result.add(new QueueStatus(
                    name,
                    properties != null,
                    value(properties, RabbitAdmin.QUEUE_MESSAGE_COUNT),
                    value(properties, RabbitAdmin.QUEUE_CONSUMER_COUNT)));
        }
        return result.stream().sorted(Comparator.comparing(QueueStatus::name)).toList();
    }

    public ResourceResponse crearCola(QueueRequest request) {
        QueueBuilder builder = Boolean.FALSE.equals(request.durable())
                ? QueueBuilder.nonDurable(request.name())
                : QueueBuilder.durable(request.name());
        if (request.deadLetterRoutingKey() != null && !request.deadLetterRoutingKey().isBlank()) {
            builder.deadLetterExchange(RabbitNames.DEAD_LETTER_EXCHANGE)
                    .deadLetterRoutingKey(request.deadLetterRoutingKey());
        }
        rabbitAdmin.declareQueue(builder.build());
        knownQueues.add(request.name());
        return new ResourceResponse("queue", request.name(), "CREATED");
    }

    public ResourceResponse eliminarCola(String name) {
        if (RabbitNames.PROTECTED_QUEUES.contains(name)) {
            throw new IllegalArgumentException("La cola base esta protegida y no se puede eliminar");
        }
        boolean deleted = rabbitAdmin.deleteQueue(name);
        knownQueues.remove(name);
        return new ResourceResponse("queue", name, deleted ? "DELETED" : "NOT_FOUND");
    }

    public ResourceResponse crearExchange(ExchangeRequest request) {
        rabbitAdmin.declareExchange(exchange(request.name(), request.type()));
        return new ResourceResponse("exchange", request.name(), "CREATED");
    }

    public ResourceResponse eliminarExchange(String name) {
        if (Set.of(RabbitNames.ORDERS_EXCHANGE, RabbitNames.NOTIFICATIONS_EXCHANGE,
                RabbitNames.DEAD_LETTER_EXCHANGE).contains(name)) {
            throw new IllegalArgumentException("El exchange base esta protegido y no se puede eliminar");
        }
        boolean deleted = rabbitAdmin.deleteExchange(name);
        return new ResourceResponse("exchange", name, deleted ? "DELETED" : "NOT_FOUND");
    }

    public ResourceResponse crearBinding(BindingRequest request) {
        Binding binding = binding(request);
        rabbitAdmin.declareBinding(binding);
        return new ResourceResponse("binding", request.routingKey(), "CREATED");
    }

    public ResourceResponse eliminarBinding(BindingRequest request) {
        rabbitAdmin.removeBinding(binding(request));
        return new ResourceResponse("binding", request.routingKey(), "DELETED");
    }

    private Binding binding(BindingRequest request) {
        Queue queue = new Queue(request.queueName());
        if ("topic".equals(request.exchangeType())) {
            return BindingBuilder.bind(queue)
                    .to(new TopicExchange(request.exchangeName()))
                    .with(request.routingKey());
        }
        return BindingBuilder.bind(queue)
                .to(new DirectExchange(request.exchangeName()))
                .with(request.routingKey());
    }

    private Exchange exchange(String name, String type) {
        return "topic".equals(type)
                ? new TopicExchange(name, true, false)
                : new DirectExchange(name, true, false);
    }

    private Integer value(Properties properties, Object key) {
        if (properties == null || !(properties.get(key) instanceof Number number)) {
            return null;
        }
        return number.intValue();
    }
}
