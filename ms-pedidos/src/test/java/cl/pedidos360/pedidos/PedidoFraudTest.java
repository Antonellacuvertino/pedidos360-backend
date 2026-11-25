package cl.pedidos360.pedidos;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class PedidoFraudTest {
    private static final String DEMO_CARD = "0000000000000001";
    @Test
    void blockedUserCannotPublishOrder() {
        FraudGuard guard = mock(FraudGuard.class);
        PedidoPublisher publisher = mock(PedidoPublisher.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("test-token");
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(guard)
                .verificar("test-token", java.math.BigDecimal.TEN, DEMO_CARD);
        PedidoController controller = new PedidoController(mock(PedidoRepository.class), publisher, guard);
        NuevoPedidoRequest request = new NuevoPedidoRequest(1L, 1L, 1, java.math.BigDecimal.TEN, null, null, DEMO_CARD);
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
        controller.crearLote(new PedidoController.Compra(java.util.List.of(item, item, item), DEMO_CARD), jwt);
        verify(guard).verificar("test-token", java.math.BigDecimal.valueOf(30), DEMO_CARD);
        verify(publisher, times(3)).publicar(item);
    }

    @Test
    void declinedPaymentCountsWithoutPublishingAnOrder() {
        FraudGuard guard = mock(FraudGuard.class);
        PedidoPublisher publisher = mock(PedidoPublisher.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("test-token");
        var controller = new PedidoController(mock(PedidoRepository.class), publisher, guard);
        var item = new NuevoPedidoRequest(1L, 1L, 1, java.math.BigDecimal.TEN, null, null);
        var request = new PedidoController.Compra(java.util.List.of(item), DEMO_CARD);
        doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED)).when(guard)
                .verificar("test-token", java.math.BigDecimal.TEN, DEMO_CARD);
        assertEquals(HttpStatus.PAYMENT_REQUIRED, assertThrows(ResponseStatusException.class,
                () -> controller.crearLote(request, jwt)).getStatusCode());
        verify(guard).verificar("test-token", java.math.BigDecimal.TEN, DEMO_CARD);
        verifyNoInteractions(publisher);
    }
}
