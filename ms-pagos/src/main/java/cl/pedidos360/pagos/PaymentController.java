package cl.pedidos360.pagos;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/pagos")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/simular")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<PagoResponse> simular(@Valid @RequestBody PagoRequest request, @AuthenticationPrincipal Jwt jwt) {
        PagoResponse response = service.simular(request, usuario(jwt));
        return ResponseEntity.status("BLOQUEADO".equals(response.estado())
                ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.OK).body(response);
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyAuthority('SCOPE_pedidos.read','SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public PaymentService.FraudStatus estado(@AuthenticationPrincipal Jwt jwt) {
        return service.estado(usuario(jwt));
    }

    @PostMapping("/autorizar-compra")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public PaymentService.FraudStatus autorizar(@Valid @RequestBody Compra request, @AuthenticationPrincipal Jwt jwt) {
        PagoResponse response = service.simular(new PagoRequest(usuario(jwt), java.util.UUID.randomUUID().toString(),
                request.monto(), "demo_checkout"), usuario(jwt), request.rechazoSimulado());
        return new PaymentService.FraudStatus("BLOQUEADO".equals(response.estado()), null);
    }

    public record Compra(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive
            java.math.BigDecimal monto, boolean rechazoSimulado) { }

    private String usuario(Jwt jwt) {
        return jwt.getIssuer() + ":" + (jwt.getClaimAsString("oid") != null
                ? jwt.getClaimAsString("oid") : jwt.getSubject());
    }
}
