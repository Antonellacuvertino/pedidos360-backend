package cl.pedidos360.pagos;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return ResponseEntity.status(switch (response.estado()) {
            case "BLOQUEADO" -> HttpStatus.TOO_MANY_REQUESTS;
            case "RECHAZADO_SIMULADO" -> HttpStatus.PAYMENT_REQUIRED;
            default -> HttpStatus.OK;
        }).body(response);
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyAuthority('SCOPE_pedidos.read','SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public PaymentService.FraudStatus estado(@RequestParam(defaultValue = "2") String tarjeta) {
        return service.estado("0".repeat(15) + tarjeta);
    }

    @PostMapping("/autorizar-compra")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public Autorizacion autorizar(@Valid @RequestBody Compra request, @AuthenticationPrincipal Jwt jwt) {
        PagoResponse response = service.simular(new PagoRequest(usuario(jwt), java.util.UUID.randomUUID().toString(),
                request.monto(), request.tarjetaPrueba()), usuario(jwt));
        return new Autorizacion("BLOQUEADO".equals(response.estado()), response.estado(), response.bloqueadoHasta());
    }

    public record Compra(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive
            java.math.BigDecimal monto, @jakarta.validation.constraints.NotBlank String tarjetaPrueba) { }
    public record Autorizacion(boolean bloqueado, String estado, Instant bloqueadoHasta) { }

    private String usuario(Jwt jwt) {
        return jwt.getIssuer() + ":" + (jwt.getClaimAsString("oid") != null
                ? jwt.getClaimAsString("oid") : jwt.getSubject());
    }
}
