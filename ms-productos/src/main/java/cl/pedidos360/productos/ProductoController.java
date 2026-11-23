package cl.pedidos360.productos;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping({"/productos", "/api/v1/productos"})
public class ProductoController {
    private final ProductoRepository repository;
    private final ProductViewPublisher viewPublisher;

    public ProductoController(ProductoRepository repository, ProductViewPublisher viewPublisher) {
        this.repository = repository;
        this.viewPublisher = viewPublisher;
    }

    @GetMapping
    public List<Producto> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public ResponseEntity<Producto> detalle(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return repository.findById(id)
                .map(producto -> {
                    viewPublisher.publicar(jwt.getClaimAsString("oid") != null
                            ? jwt.getClaimAsString("oid") : jwt.getSubject(), producto);
                    return ResponseEntity.ok(producto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasRole('Pedidos.Admin')")
    public Producto crear(@Valid @RequestBody Producto producto) {
        return repository.save(producto);
    }

    @PostMapping("/{id}/interacciones")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.write') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<Void> interaccion(@PathVariable Long id,
            @Valid @RequestBody Interaccion request, @AuthenticationPrincipal Jwt jwt) {
        return repository.findById(id).map(producto -> {
            viewPublisher.publicar(jwt.getClaimAsString("oid") != null
                    ? jwt.getClaimAsString("oid") : jwt.getSubject(), producto, request.accion().name());
            return ResponseEntity.accepted().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public enum Accion { VISTA, CARRITO }
    public record Interaccion(@jakarta.validation.constraints.NotNull Accion accion) { }
}
