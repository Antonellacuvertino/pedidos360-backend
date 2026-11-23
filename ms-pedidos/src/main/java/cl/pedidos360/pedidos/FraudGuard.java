package cl.pedidos360.pedidos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FraudGuard {
    private final RestClient client;

    @Autowired
    public FraudGuard(RestClient.Builder builder, @Value("${pedidos360.pagos-url}") String pagosUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        client = builder.baseUrl(pagosUrl).requestFactory(factory).build();
    }

    FraudGuard(RestClient client) { this.client = client; }

    public void verificar(String accessToken, java.math.BigDecimal monto) {
        Status status;
        try {
            status = client.post().uri("/pagos/autorizar-compra")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(java.util.Map.of("monto", monto))
                    .retrieve().body(Status.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar el bloqueo de compra");
        }
        if (status == null || status.bloqueado() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Estado de fraude no disponible");
        }
        if (status.bloqueado()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Compra bloqueada temporalmente por intentos de pago reiterados");
        }
    }

    public record Status(Boolean bloqueado) { }
}
