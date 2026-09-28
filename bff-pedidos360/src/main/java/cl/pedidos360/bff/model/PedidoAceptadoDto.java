package cl.pedidos360.bff.model;

import java.util.UUID;

public record PedidoAceptadoDto(UUID eventoId, String estado, String mensaje) {
}
