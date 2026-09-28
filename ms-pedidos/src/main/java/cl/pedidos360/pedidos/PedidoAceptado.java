package cl.pedidos360.pedidos;

import java.util.UUID;

public record PedidoAceptado(UUID eventoId, String estado, String mensaje) {
}
