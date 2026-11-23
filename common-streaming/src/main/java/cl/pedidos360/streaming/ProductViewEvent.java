package cl.pedidos360.streaming;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProductViewEvent(
        UUID eventId,
        String usuarioId,
        Long productoId,
        String categoria,
        Instant fecha,
        String accion) {

    public ProductViewEvent(UUID eventId, String usuarioId, Long productoId, String categoria, Instant fecha) {
        this(eventId, usuarioId, productoId, categoria, fecha, "VISTA");
    }

    public ProductViewEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(productoId, "productoId");
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(fecha, "fecha");
        if (usuarioId.isBlank() || categoria.isBlank() || productoId <= 0) {
            throw new IllegalArgumentException("Vista de producto incompleta");
        }
    }
}
