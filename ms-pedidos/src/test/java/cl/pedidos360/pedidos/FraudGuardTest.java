package cl.pedidos360.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class FraudGuardTest {
    private static final String DEMO_CARD = "0000000000000002";
    @ParameterizedTest
    @CsvSource({"true,429", "null,503"})
    void rejectsBlockedOrMissingStatus(String blocked, int expected) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://pagos");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FraudGuard guard = new FraudGuard(builder.build());
        server.expect(requestTo("http://pagos/pagos/autorizar-compra"))
            .andExpect(header("Authorization", "Bearer prueba"))
            .andRespond(withSuccess("{\"bloqueado\":" + blocked + "}", MediaType.APPLICATION_JSON));
        assertThat(assertThrows(ResponseStatusException.class, () -> guard.verificar("prueba", java.math.BigDecimal.TEN, DEMO_CARD))
            .getStatusCode().value()).isEqualTo(expected);
        server.verify();
    }

    @Test
    void rejectsUnavailableFraudService() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://pagos");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FraudGuard guard = new FraudGuard(builder.build());
        server.expect(requestTo("http://pagos/pagos/autorizar-compra")).andRespond(withServerError());
        assertThat(assertThrows(ResponseStatusException.class, () -> guard.verificar("prueba", java.math.BigDecimal.TEN, DEMO_CARD))
            .getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void allowsOnlyApprovedPayment() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://pagos");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FraudGuard guard = new FraudGuard(builder.build());
        server.expect(requestTo("http://pagos/pagos/autorizar-compra"))
            .andExpect(content().json("{\"monto\":10,\"tarjetaPrueba\":\"0000000000000002\"}"))
            .andRespond(withSuccess("{\"bloqueado\":false,\"estado\":\"APROBADO_SIMULADO\"}", MediaType.APPLICATION_JSON));
        guard.verificar("prueba", java.math.BigDecimal.TEN, DEMO_CARD);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"RECHAZADO_SIMULADO,402", "DESCONOCIDO,503"})
    void rejectsNonApprovedResponse(String state, int expected) {
        var builder = RestClient.builder().baseUrl("http://pagos");
        var server = MockRestServiceServer.bindTo(builder).build();
        var guard = new FraudGuard(builder.build());
        server.expect(requestTo("http://pagos/pagos/autorizar-compra"))
                .andRespond(withSuccess("{\"bloqueado\":false,\"estado\":\"" + state + "\"}", MediaType.APPLICATION_JSON));
        assertThat(assertThrows(ResponseStatusException.class,
                () -> guard.verificar("prueba", java.math.BigDecimal.TEN, DEMO_CARD))
                .getStatusCode().value()).isEqualTo(expected);
        server.verify();
    }
}
