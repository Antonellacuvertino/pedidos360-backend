package cl.pedidos360.pedidos;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoRepository repository;
    private final PedidoPublisher publisher;
    private final FraudGuard fraudGuard;

    public PedidoController(PedidoRepository repository, PedidoPublisher publisher, FraudGuard fraudGuard) {
        this.repository = repository;
        this.publisher = publisher;
        this.fraudGuard = fraudGuard;
    }

    @GetMapping
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<PedidoAceptado> crear(@Valid @RequestBody NuevoPedidoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        fraudGuard.verificar(jwt.getTokenValue(), request.total(), request.tarjetaPrueba());
        PedidoAceptado respuesta = publisher.publicar(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }

    @PostMapping("/lote")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<List<PedidoAceptado>> crearLote(@Valid @RequestBody Compra request,
            @AuthenticationPrincipal Jwt jwt) {
        var total = request.items().stream().map(NuevoPedidoRequest::total)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        fraudGuard.verificar(jwt.getTokenValue(), total, request.tarjetaPrueba());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(request.items().stream().map(publisher::publicar).toList());
    }

    public record Compra(@jakarta.validation.constraints.NotEmpty @jakarta.validation.constraints.Size(max = 50)
            List<@jakarta.validation.constraints.NotNull @Valid NuevoPedidoRequest> items, String tarjetaPrueba) { }

    @ExceptionHandler(FraudGuard.PaymentBlockedException.class)
    ResponseEntity<ProblemDetail> pagoBloqueado(FraudGuard.PaymentBlockedException exception) {
        var body = exception.getBody();
        body.setTitle("Alerta de posible fraude");
        var response = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (exception.retryAt() != null) {
            var remaining = Duration.between(Instant.now(), exception.retryAt());
            body.setProperty("bloqueadoHasta", exception.retryAt());
            response.header(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(0, remaining.plusMillis(999).toSeconds())));
        }
        return response.body(body);
    }
}
