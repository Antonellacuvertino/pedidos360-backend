package cl.pedidos360.bff;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class DownstreamExceptionHandler {
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> downstream(RestClientResponseException exception) {
        String message = exception.getStatusCode().value() == 429
                ? "Compra bloqueada temporalmente por intentos de pago reiterados"
                : "El servicio no pudo completar la solicitud";
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("mensaje", message));
    }
}
