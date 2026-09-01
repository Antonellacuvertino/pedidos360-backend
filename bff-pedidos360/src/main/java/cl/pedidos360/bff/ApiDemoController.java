package cl.pedidos360.bff;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApiDemoController {

    @GetMapping("/public")
    public Map<String, Object> publico() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 200);
        response.put("tipo", "PUBLICO");
        response.put("mensaje", "Endpoint publico del BFF. No requiere token JWT.");
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    @GetMapping
    public Map<String, Object> protegido(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 200);
        response.put("tipo", "PROTEGIDO");
        response.put("mensaje", "Token JWT validado correctamente por Spring Security.");
        response.put("issuer", jwt.getIssuer().toString());
        response.put("audience", jwt.getAudience());
        response.put("subject", jwt.getSubject());
        response.put("scopes", jwt.getClaimAsString("scp"));
        response.put("roles", jwt.getClaimAsStringList("roles"));
        response.put("expiresAt", jwt.getExpiresAt());
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> postProtegido(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 201);
        response.put("tipo", "CREADO");
        response.put("mensaje", "POST autenticado procesado correctamente.");
        response.put("usuario", jwt.getClaimAsString("preferred_username"));
        response.put("bodyRecibido", body);
        response.put("timestamp", Instant.now().toString());
        return response;
    }
}
