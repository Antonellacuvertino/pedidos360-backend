package cl.pedidos360.analytics;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analitica")
public class AnalyticsController {
    private final ProductViewRepository views;
    private final PaymentAttemptRepository payments;
    private final RecommendationService recommendations;

    public AnalyticsController(ProductViewRepository views, PaymentAttemptRepository payments, RecommendationService recommendations) {
        this.views = views;
        this.payments = payments;
        this.recommendations = recommendations;
    }

    @GetMapping("/recomendaciones")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public RecommendationService.Result recomendaciones(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean soloCarrito) {
        String userId = jwt.getClaimAsString("oid") != null ? jwt.getClaimAsString("oid") : jwt.getSubject();
        return recommendations.forUser(userId, jwt.getTokenValue(), soloCarrito);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('SCOPE_pedidos.read') or hasRole('Pedidos.Admin')")
    public Metrics resumen() {
        return new Metrics(views.count(), payments.count(), payments.countByEstado("BLOQUEADO"));
    }

    public record Metrics(long vistasProductos, long intentosPago, long bloqueosFraude) { }
}
