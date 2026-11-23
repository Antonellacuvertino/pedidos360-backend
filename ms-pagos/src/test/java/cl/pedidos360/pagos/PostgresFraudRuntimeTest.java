package cl.pedidos360.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@EnabledIfEnvironmentVariable(named = "POSTGRES_FRAUD_TEST", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:55432/validation",
    "spring.datasource.username=postgres",
    "spring.datasource.password=validation-only",
    "spring.jpa.hibernate.ddl-auto=update",
    "pedidos360.streaming.enabled=false",
    "pedidos360.payments.fingerprint-pepper=local-validation-only",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://example.test",
    "spring.security.oauth2.resourceserver.jwt.audiences=test"
})
class PostgresFraudRuntimeTest {
    @Autowired PaymentService service;
    @MockBean JwtDecoder jwtDecoder;

    @Test
    void concurrentFirstAttemptsCommitExactlyOneBlock() throws Exception {
        String user = "validation-" + UUID.randomUUID();
        PagoRequest request = new PagoRequest("ignored", "test", BigDecimal.TEN, "demo_test");
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<String> attempt = () -> { start.await(); return service.simular(request, user).estado(); };
            List<Future<String>> results = List.of(executor.submit(attempt), executor.submit(attempt), executor.submit(attempt));
            start.countDown();
            List<String> states = List.of(results.get(0).get(20, TimeUnit.SECONDS),
                    results.get(1).get(20, TimeUnit.SECONDS), results.get(2).get(20, TimeUnit.SECONDS));
            assertThat(states).containsExactlyInAnyOrder("APROBADO_SIMULADO", "APROBADO_SIMULADO", "BLOQUEADO");
            assertThat(service.estado(user).bloqueado()).isTrue();
            assertThat(service.simular(new PagoRequest("changed", "test2", BigDecimal.ONE, "demo_changed"), user).estado())
                    .isEqualTo("BLOQUEADO");
        } finally {
            executor.shutdownNow();
        }
    }
}
