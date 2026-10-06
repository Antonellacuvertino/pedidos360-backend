package cl.pedidos360.notifications;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/email")
public class NotificationController {
    private final NotificationPublisher publisher;

    public NotificationController(NotificationPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/enviar")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<NotificationAccepted> enviar(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(publisher.publicar(request));
    }
}
