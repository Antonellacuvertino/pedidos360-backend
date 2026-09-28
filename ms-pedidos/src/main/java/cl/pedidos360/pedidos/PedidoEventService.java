package cl.pedidos360.pedidos;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.pedidos360.messaging.OrderEvent;

@Service
public class PedidoEventService {
    private final PedidoRepository repository;

    public PedidoEventService(PedidoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void guardarSiEsNuevo(OrderEvent event) {
        if (repository.existsByEventoId(event.eventId())) {
            return;
        }

        EstadoPedido estado = event.estado() == null
                ? EstadoPedido.RECIBIDO
                : EstadoPedido.valueOf(event.estado());
        repository.save(new Pedido(
                event.eventId(),
                event.clienteId(),
                event.productoId(),
                event.cantidad(),
                event.total(),
                estado));
    }
}
