package cl.pedidos360.pagos;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pagos")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/simular")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<PagoResponse> simular(@Valid @RequestBody PagoRequest request) {
        PagoResponse response = service.simular(request);
        return ResponseEntity.status("BLOQUEADO".equals(response.estado())
                ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.OK).body(response);
    }
}
