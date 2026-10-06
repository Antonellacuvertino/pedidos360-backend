package cl.pedidos360.pedidos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoRepository repository;
    private final PedidoPublisher publisher;

    public PedidoController(PedidoRepository repository, PedidoPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @GetMapping
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<PedidoAceptado> crear(@Valid @RequestBody NuevoPedidoRequest request) {
        PedidoAceptado respuesta = publisher.publicar(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }
}
