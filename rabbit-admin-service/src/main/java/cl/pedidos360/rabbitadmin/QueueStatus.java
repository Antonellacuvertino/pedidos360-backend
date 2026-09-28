package cl.pedidos360.rabbitadmin;

public record QueueStatus(String name, boolean exists, Integer messageCount, Integer consumerCount) {
}
