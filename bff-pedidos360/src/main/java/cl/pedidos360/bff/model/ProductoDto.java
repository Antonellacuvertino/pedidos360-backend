package cl.pedidos360.bff.model;

import java.math.BigDecimal;

public record ProductoDto(Long id, String nombre, String categoria, BigDecimal precio, int stock) {
}
