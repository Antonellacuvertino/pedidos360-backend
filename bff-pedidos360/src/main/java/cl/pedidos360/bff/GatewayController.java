package cl.pedidos360.bff;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import cl.pedidos360.bff.model.ClienteDto;
import cl.pedidos360.bff.model.EmailRequest;
import cl.pedidos360.bff.model.NuevoClienteRequest;
import cl.pedidos360.bff.model.NuevoPedidoRequest;
import cl.pedidos360.bff.model.NuevoProductoRequest;
import cl.pedidos360.bff.model.PedidoDto;
import cl.pedidos360.bff.model.PedidoAceptadoDto;
import cl.pedidos360.bff.model.ProductoDto;
import cl.pedidos360.bff.model.RabbitQueueRequest;
import cl.pedidos360.bff.model.ResumenDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class GatewayController {
    private final RestClient restClient;
    private final ServiceUrls serviceUrls;

    public GatewayController(RestClient restClient, ServiceUrls serviceUrls) {
        this.restClient = restClient;
        this.serviceUrls = serviceUrls;
    }

    @GetMapping("/productos")
    public List<ProductoDto> productos(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return getList(serviceUrls.productosUrl() + "/productos", authorization, ProductoDto[].class);
    }

    @PostMapping("/productos")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public ProductoDto crearProducto(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody NuevoProductoRequest request) {
        return restClient.post()
                .uri(serviceUrls.productosUrl() + "/productos")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(request)
                .retrieve()
                .body(ProductoDto.class);
    }

    @GetMapping("/clientes")
    public List<ClienteDto> clientes(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return getList(serviceUrls.clientesUrl() + "/clientes", authorization, ClienteDto[].class);
    }

    @PostMapping("/clientes")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ClienteDto crearCliente(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody NuevoClienteRequest request) {
        return restClient.post()
                .uri(serviceUrls.clientesUrl() + "/clientes")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(request)
                .retrieve()
                .body(ClienteDto.class);
    }

    @GetMapping("/pedidos")
    public List<PedidoDto> pedidos(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return getList(serviceUrls.pedidosUrl() + "/pedidos", authorization, PedidoDto[].class);
    }

    @PostMapping("/pedidos")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public ResponseEntity<PedidoAceptadoDto> crearPedido(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody NuevoPedidoRequest request) {
        PedidoAceptadoDto aceptado = restClient.post()
                .uri(serviceUrls.pedidosUrl() + "/pedidos")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(request)
                .retrieve()
                .body(PedidoAceptadoDto.class);
        return ResponseEntity.accepted().body(aceptado);
    }

    @PostMapping("/notificaciones/prueba")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasAnyRole('Pedidos.Admin','Pedidos.Operador')")
    public Map<?, ?> probarNotificacion(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody EmailRequest request) {
        return restClient.post()
                .uri(serviceUrls.notificationsUrl() + "/api/email/enviar")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(request)
                .retrieve()
                .body(Map.class);
    }

    @GetMapping("/rabbitmq/queues")
    public List<Map<String, Object>> listarColas(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Map<String, Object>[] response = restClient.get()
                .uri(serviceUrls.rabbitAdminUrl() + "/api/rabbitmq/queues")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(Map[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    @PostMapping("/rabbitmq/queues")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public Map<?, ?> crearCola(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody RabbitQueueRequest request) {
        return restClient.post()
                .uri(serviceUrls.rabbitAdminUrl() + "/api/rabbitmq/queues")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(request)
                .retrieve()
                .body(Map.class);
    }

    @DeleteMapping("/rabbitmq/queues/{name}")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.escribe') or hasRole('Pedidos.Admin')")
    public Map<?, ?> eliminarCola(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String name) {
        return restClient.delete()
                .uri(serviceUrls.rabbitAdminUrl() + "/api/rabbitmq/queues/{name}", name)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(Map.class);
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> auditoria(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Map<String, Object>[] response = restClient.get()
                .uri(serviceUrls.auditoriaUrl() + "/auditoria")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(Map[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    @GetMapping("/resumen")
    public ResumenDto resumen(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        List<ProductoDto> productos = productos(authorization);
        List<ClienteDto> clientes = clientes(authorization);
        List<PedidoDto> pedidos = pedidos(authorization);
        BigDecimal ventaTotal = pedidos.stream()
                .map(PedidoDto::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int enPreparacion = (int) pedidos.stream()
                .filter(pedido -> "PREPARACION".equals(pedido.estado()))
                .count();

        return new ResumenDto(pedidos.size(), productos.size(), clientes.size(), ventaTotal, enPreparacion);
    }

    private <T> List<T> getList(String url, String authorization, Class<T[]> responseType) {
        T[] response = restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(responseType);
        return response == null ? List.of() : Arrays.asList(response);
    }
}
