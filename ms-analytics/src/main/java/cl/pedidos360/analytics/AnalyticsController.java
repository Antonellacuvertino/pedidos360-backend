package cl.pedidos360.analytics;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analitica")
public class AnalyticsController {
    private final ProductViewRepository views;
    private final PaymentAttemptRepository payments;

    public AnalyticsController(ProductViewRepository views, PaymentAttemptRepository payments) {
        this.views = views;
        this.payments = payments;
    }

    @GetMapping("/recomendaciones")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public Recommendations recomendaciones(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("oid") != null ? jwt.getClaimAsString("oid") : jwt.getSubject();
        ProductViewRecord last = views.findFirstByUsuarioIdOrderByFechaDesc(userId);
        if (last == null) {
            return new Recommendations(null, List.of());
        }
        List<Long> seen = views.viewedBy(userId);
        List<Long> suggested = views.popularInCategory(last.getCategoria(), PageRequest.of(0, 20))
                .stream().filter(id -> !seen.contains(id)).limit(5).toList();
        if (suggested.isEmpty()) {
            suggested = List.of(last.getProductoId());
        }
        return new Recommendations(last.getCategoria(), suggested);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public Metrics resumen() {
        return new Metrics(views.count(), payments.count(), payments.countByEstado("BLOQUEADO"));
    }

    public record Recommendations(String categoria, List<Long> productoIds) { }
    public record Metrics(long vistasProductos, long intentosPago, long bloqueosFraude) { }
}
