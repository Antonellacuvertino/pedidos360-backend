package cl.pedidos360.analytics;

import java.time.Instant;
import java.util.UUID;

import cl.pedidos360.streaming.ProductViewEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = @Index(name = "idx_view_user_time", columnList = "usuarioId,fecha"))
public class ProductViewRecord {
    @Id
    private UUID id;
    private String usuarioId;
    private Long productoId;
    private String categoria;
    private Instant fecha;

    protected ProductViewRecord() {
    }

    public ProductViewRecord(ProductViewEvent event) {
        id = event.eventId();
        usuarioId = event.usuarioId();
        productoId = event.productoId();
        categoria = event.categoria();
        fecha = event.fecha();
    }

    public UUID getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public Long getProductoId() { return productoId; }
    public String getCategoria() { return categoria; }
    public Instant getFecha() { return fecha; }
}
