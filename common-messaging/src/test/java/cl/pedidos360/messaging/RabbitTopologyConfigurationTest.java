package cl.pedidos360.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RabbitTopologyConfigurationTest {
    private final RabbitTopologyConfiguration configuration = new RabbitTopologyConfiguration();

    @Test
    void configuraCadaColaPrincipalConSuDlq() {
        assertDeadLetter(configuration.ordersQueue(), RabbitNames.ORDERS_DLQ);
        assertDeadLetter(configuration.notificationsQueue(), RabbitNames.NOTIFICATIONS_DLQ);
        assertDeadLetter(configuration.stockQueue(), RabbitNames.STOCK_DLQ);
        assertDeadLetter(configuration.auditQueue(), RabbitNames.AUDIT_DLQ);
    }

    @Test
    void declaraExchangesDirectYTopic() {
        assertThat(configuration.ordersExchange().getType()).isEqualTo("topic");
        assertThat(configuration.notificationsExchange().getType()).isEqualTo("direct");
        assertThat(configuration.deadLetterExchange().getType()).isEqualTo("direct");
    }

    @Test
    void iniciaElContextoConTodosLosBindings() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RabbitTopologyConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("ordersBinding");
                    assertThat(context).hasBean("notificationsDirectBinding");
                    assertThat(context).hasBean("stockDeadLetterBinding");
                });
    }

    private void assertDeadLetter(Queue queue, String routingKey) {
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitNames.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", routingKey);
    }
}
