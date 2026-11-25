package cl.pedidos360.pagos;

import java.time.Instant;
import java.util.UUID;

public record PagoResponse(UUID intentoId, String estado, String mensaje, Instant fecha, Instant bloqueadoHasta) {
}
