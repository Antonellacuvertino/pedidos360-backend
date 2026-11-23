package cl.pedidos360.kafkaadmin;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class KafkaAdminExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(KafkaAdminExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, ExecutionException.class, TimeoutException.class})
    public ResponseEntity<Map<String, String>> handle(Exception exception) {
        Throwable cause = exception instanceof ExecutionException ? exception.getCause() : exception;
        if (cause instanceof IllegalArgumentException) {
            return response(HttpStatus.BAD_REQUEST, cause.getMessage());
        }
        if (cause instanceof TopicExistsException) {
            return response(HttpStatus.CONFLICT, "El topico ya existe");
        }
        if (cause instanceof UnknownTopicOrPartitionException) {
            return response(HttpStatus.NOT_FOUND, "Topico no encontrado");
        }
        log.error("Error al administrar Kafka", exception);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no disponible o solicitud agotada");
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
