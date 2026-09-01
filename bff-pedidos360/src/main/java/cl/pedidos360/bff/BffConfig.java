package cl.pedidos360.bff;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ServiceUrls.class)
public class BffConfig {
    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
