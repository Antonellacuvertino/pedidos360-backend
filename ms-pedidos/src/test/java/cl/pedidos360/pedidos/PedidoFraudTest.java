package cl.pedidos360.pedidos;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class PedidoFraudTest {
    @Test
    void blockedUserCannotPublishOrder() {
        FraudGuard guard = mock(FraudGuard.class);
        PedidoPublisher publisher = mock(PedidoPublisher.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("test-token");
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(guard)
                .verificar("test-token", java.math.BigDecimal.TEN);
        PedidoController controller = new PedidoController(mock(PedidoRepository.class), publisher, guard);
        NuevoPedidoRequest request = new NuevoPedidoRequest(1L, 1L, 1, java.math.BigDecimal.TEN, null, null);
        assertThrows(ResponseStatusException.class, () -> controller.crear(request, jwt));
        verifyNoInteractions(publisher);
    }

    @Test
    void cartWithThreeProductsCountsAsOneAttempt() {
        FraudGuard guard = mock(FraudGuard.class);
        PedidoPublisher publisher = mock(PedidoPublisher.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("test-token");
        PedidoController controller = new PedidoController(mock(PedidoRepository.class), publisher, guard);
        NuevoPedidoRequest item = new NuevoPedidoRequest(1L, 1L, 1, java.math.BigDecimal.TEN, null, null);
        controller.crearLote(new PedidoController.Compra(java.util.List.of(item, item, item)), jwt);
        verify(guard).verificar("test-token", java.math.BigDecimal.valueOf(30));
        verify(publisher, times(3)).publicar(item);
    }
}
