package cl.pedidos360.productos;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/productos")
public class ProductoController {
    private final ProductoRepository repository;

    public ProductoController(ProductoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Producto> listar() {
        return repository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public Producto crear(@Valid @RequestBody Producto producto) {
        return repository.save(producto);
    }
}
