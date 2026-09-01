package cl.pedidos360.bff.model;

import java.math.BigDecimal;

public record ResumenDto(
        int totalPedidos,
        int totalProductos,
        int totalClientes,
        BigDecimal ventaTotal,
        int pedidosEnPreparacion) {
}
