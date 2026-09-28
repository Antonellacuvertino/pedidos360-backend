package cl.pedidos360.auditoria;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditoria")
public class AuditController {
    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuditEntry> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "receivedAt"));
    }
}
