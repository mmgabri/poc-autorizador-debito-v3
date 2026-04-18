package br.com.nicolebertolo.infrastructure.adapter.inbound.rest.advice;

import br.com.nicolebertolo.domain.exception.PropertyListingNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Tratamento para quando o imóvel não é encontrado.
     */
    @ExceptionHandler(PropertyListingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePropertyNotFound(
            PropertyListingNotFoundException ex, WebRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Tratamento para argumentos inválidos no corpo da requisição.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(
            MethodArgumentNotValidException ex, WebRequest request) {

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, String.join("; ", errors), request);
    }

    /**
     * Tratamento genérico para qualquer outra exceção não capturada.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex, WebRequest request) {

        // Log detalhado para depuração
        ex.printStackTrace();

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado: " + ex.getMessage(),
                request);
    }

    /**
     * Método utilitário para construir respostas JSON padronizadas.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, WebRequest request) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(status).body(body);
    }
}