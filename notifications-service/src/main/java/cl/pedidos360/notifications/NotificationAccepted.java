package cl.pedidos360.notifications;

import java.util.UUID;

public record NotificationAccepted(UUID eventoId, String estado) {
}
