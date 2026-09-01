package cl.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class BffApplicationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BffConfig.class)
            .withPropertyValues(
                    "pedidos360.services.productos-url=http://localhost:8081",
                    "pedidos360.services.clientes-url=http://localhost:8082",
                    "pedidos360.services.pedidos-url=http://localhost:8083")
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void cargaConfiguracionDeServicios() {
        contextRunner.run(context -> {
            ServiceUrls urls = context.getBean(ServiceUrls.class);

            assertThat(urls.productosUrl()).isEqualTo("http://localhost:8081");
            assertThat(urls.clientesUrl()).isEqualTo("http://localhost:8082");
            assertThat(urls.pedidosUrl()).isEqualTo("http://localhost:8083");
        });
    }
}
