package cl.pedidos360.rabbitadmin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/rabbitmq")
public class RabbitAdminController {
    private final RabbitManagementService service;

    public RabbitAdminController(RabbitManagementService service) {
        this.service = service;
    }

    @GetMapping("/queues")
    public List<QueueStatus> listarColas() {
        return service.listarColas();
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public ResourceResponse crearCola(@Valid @RequestBody QueueRequest request) {
        return service.crearCola(request);
    }

    @DeleteMapping("/queues/{name}")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public ResourceResponse eliminarCola(
            @PathVariable
            @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{2,79}$") String name) {
        return service.eliminarCola(name);
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public ResourceResponse crearExchange(@Valid @RequestBody ExchangeRequest request) {
        return service.crearExchange(request);
    }

    @DeleteMapping("/exchanges/{name}")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public ResourceResponse eliminarExchange(@PathVariable String name) {
        return service.eliminarExchange(name);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public ResourceResponse crearBinding(@Valid @RequestBody BindingRequest request) {
        return service.crearBinding(request);
    }

    @DeleteMapping("/bindings")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public ResourceResponse eliminarBinding(@Valid @RequestBody BindingRequest request) {
        return service.eliminarBinding(request);
    }
}
