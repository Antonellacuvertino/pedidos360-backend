package cl.pedidos360.analytics;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationService {
    private static final Map<String, List<String>> COMPLEMENTS = Map.of(
            "Computacion", List.of("Perifericos", "Accesorios", "Redes"),
            "Perifericos", List.of("Accesorios", "Computacion"),
            "Accesorios", List.of("Computacion", "Perifericos"),
            "Redes", List.of("Computacion", "Accesorios"));
    private final ProductViewRepository views;
    private final RestClient catalog;

    public RecommendationService(ProductViewRepository views, RestClient.Builder builder,
            @Value("${pedidos360.productos-url}") String catalogUrl) {
        this.views = views;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.catalog = builder.baseUrl(catalogUrl).requestFactory(factory).build();
    }

    public Result forUser(String userId, String token, boolean soloCarrito) {
        var selected = soloCarrito
                ? views.findFirstByUsuarioIdAndAccionOrderByFechaDesc(userId, "CARRITO")
                : views.findFirstByUsuarioIdAndAccionInOrderByFechaDesc(userId, List.of("VISTA", "CARRITO"));
        if (selected == null) return new Result(null, List.of(), null, null);
        try {
            Product[] products = catalog.get().uri("/productos")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve().body(Product[].class);
            return suggest(selected, products == null ? List.of() : Arrays.asList(products));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Sugerencias no disponibles");
        }
    }

    static Result suggest(ProductViewRecord selected, List<Product> products) {
        var categories = new ArrayList<String>();
        if ("VISTA".equals(selected.getAccion())) categories.add(selected.getCategoria());
        categories.addAll(COMPLEMENTS.getOrDefault(selected.getCategoria(), List.of()));
        var ids = products.stream()
                .filter(p -> p.stock() > 0 && !selected.getProductoId().equals(p.id()))
                .filter(p -> categories.contains(p.categoria()))
                .sorted(Comparator.comparingInt((Product p) -> categories.indexOf(p.categoria()))
                        .thenComparing(Product::precio).thenComparing(Product::id))
                .map(Product::id).toList();
        return new Result(selected.getCategoria(), ids, selected.getProductoId(), selected.getAccion());
    }

    record Product(Long id, String categoria, BigDecimal precio, int stock) { }
    public record Result(String categoria, List<Long> productoIds, Long productoBaseId, String accionBase) { }
}
