package cl.pedidos360.pedidos;

import java.util.List;

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

    public PedidoController(PedidoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_write-read') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public Pedido crear(@Valid @RequestBody Pedido pedido) {
        return repository.save(pedido);
    }
}
