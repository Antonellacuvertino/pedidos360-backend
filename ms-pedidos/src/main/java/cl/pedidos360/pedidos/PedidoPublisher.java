package cl.pedidos360.pedidos;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import cl.pedidos360.messaging.OrderEvent;
import cl.pedidos360.messaging.RabbitNames;

@Service
public class PedidoPublisher {
    private final RabbitTemplate rabbitTemplate;

    public PedidoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public PedidoAceptado publicar(NuevoPedidoRequest request) {
        String estado = request.estado() == null ? EstadoPedido.RECIBIDO.name() : request.estado().name();
        OrderEvent event = OrderEvent.orderCreated(
                request.clienteId(),
                request.productoId(),
                request.cantidad(),
                request.total(),
                estado,
                request.email());

        rabbitTemplate.convertAndSend(
                RabbitNames.ORDERS_EXCHANGE,
                RabbitNames.ORDER_CREATED_KEY,
                event);

        return new PedidoAceptado(
                event.eventId(),
                "ACEPTADO",
                "Pedido enviado a procesamiento asincrono");
    }
}
