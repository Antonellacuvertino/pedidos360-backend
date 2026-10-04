package cl.pedidos360.rabbitadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

class RabbitAdminConfigurationTest {
    @Test
    void creaAdministradorConLaConexionConfigurada() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        RabbitAdmin rabbitAdmin = new RabbitAdminConfiguration().rabbitAdmin(connectionFactory);

        assertThat(rabbitAdmin).isNotNull();
    }
}
