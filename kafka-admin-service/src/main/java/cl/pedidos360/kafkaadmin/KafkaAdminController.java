package cl.pedidos360.kafkaadmin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/kafka")
public class KafkaAdminController {
    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/topics")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public List<String> topics() throws Exception { return service.topics(); }

    @GetMapping("/topics/{name}")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public KafkaAdminService.TopicDetails topic(@PathVariable String name) throws Exception {
        return service.detail(name);
    }

    @PostMapping("/topics")
    @PreAuthorize("hasRole('Pedidos.Admin')")
    public ResponseEntity<KafkaAdminService.TopicDetails> create(@Valid @RequestBody TopicCreateRequest request)
            throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/topics/{name}")
    @PreAuthorize("hasRole('Pedidos.Admin')")
    public KafkaAdminService.TopicDetails update(@PathVariable String name,
            @Valid @RequestBody TopicUpdateRequest request) throws Exception {
        return service.update(name, request);
    }

    @DeleteMapping("/topics/{name}")
    @PreAuthorize("hasRole('Pedidos.Admin')")
    public ResponseEntity<Void> delete(@PathVariable String name) throws Exception {
        service.delete(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public List<String> groups() throws Exception { return service.groups(); }

    @GetMapping("/groups/{id}/offsets")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public List<KafkaAdminService.GroupOffset> offsets(@PathVariable String id) throws Exception {
        return service.groupOffsets(id);
    }
}
