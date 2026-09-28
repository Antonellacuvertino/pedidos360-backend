package cl.pedidos360.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RabbitTopologyConfiguration {

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(RabbitNames.ORDERS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange notificationsExchange() {
        return new DirectExchange(RabbitNames.NOTIFICATIONS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitNames.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue ordersQueue() {
        return mainQueue(RabbitNames.ORDERS_QUEUE, RabbitNames.ORDERS_DLQ);
    }

    @Bean
    Queue notificationsQueue() {
        return mainQueue(RabbitNames.NOTIFICATIONS_QUEUE, RabbitNames.NOTIFICATIONS_DLQ);
    }

    @Bean
    Queue stockQueue() {
        return mainQueue(RabbitNames.STOCK_QUEUE, RabbitNames.STOCK_DLQ);
    }

    @Bean
    Queue auditQueue() {
        return mainQueue(RabbitNames.AUDIT_QUEUE, RabbitNames.AUDIT_DLQ);
    }

    @Bean
    Queue ordersDeadLetterQueue() {
        return QueueBuilder.durable(RabbitNames.ORDERS_DLQ).build();
    }

    @Bean
    Queue notificationsDeadLetterQueue() {
        return QueueBuilder.durable(RabbitNames.NOTIFICATIONS_DLQ).build();
    }

    @Bean
    Queue stockDeadLetterQueue() {
        return QueueBuilder.durable(RabbitNames.STOCK_DLQ).build();
    }

    @Bean
    Queue auditDeadLetterQueue() {
        return QueueBuilder.durable(RabbitNames.AUDIT_DLQ).build();
    }

    @Bean
    Binding ordersBinding(Queue ordersQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue).to(ordersExchange).with(RabbitNames.ORDER_CREATED_KEY);
    }

    @Bean
    Binding notificationsOrderBinding(Queue notificationsQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(notificationsQueue).to(ordersExchange).with("order.*");
    }

    @Bean
    Binding stockBinding(Queue stockQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(stockQueue).to(ordersExchange).with(RabbitNames.ORDER_CREATED_KEY);
    }

    @Bean
    Binding auditBinding(Queue auditQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(auditQueue).to(ordersExchange).with("order.#");
    }

    @Bean
    Binding notificationsDirectBinding(Queue notificationsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(notificationsExchange)
                .with(RabbitNames.NOTIFICATION_EMAIL_KEY);
    }

    @Bean
    Binding ordersDeadLetterBinding(Queue ordersDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(ordersDeadLetterQueue).to(deadLetterExchange).with(RabbitNames.ORDERS_DLQ);
    }

    @Bean
    Binding notificationsDeadLetterBinding(
            Queue notificationsDeadLetterQueue,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationsDeadLetterQueue)
                .to(deadLetterExchange)
                .with(RabbitNames.NOTIFICATIONS_DLQ);
    }

    @Bean
    Binding stockDeadLetterBinding(Queue stockDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(stockDeadLetterQueue).to(deadLetterExchange).with(RabbitNames.STOCK_DLQ);
    }

    @Bean
    Binding auditDeadLetterBinding(Queue auditDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(auditDeadLetterQueue).to(deadLetterExchange).with(RabbitNames.AUDIT_DLQ);
    }

    @Bean
    Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    private Queue mainQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(RabbitNames.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }
}
