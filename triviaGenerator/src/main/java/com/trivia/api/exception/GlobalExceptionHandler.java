package com.trivia.api.exception;

import com.trivia.api.ai.exception.AiClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Manejador global de excepciones (RFC 7807 Problem Details).
 *
 * Captura y estandariza las respuestas de error en formato JSON para toda la API REST.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNoSuchElement(NoSuchElementException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso no encontrado");
        problem.setType(URI.create("https://api.trivia.com/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Solicitud inválida");
        problem.setType(URI.create("https://api.trivia.com/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Errores de validación de campos: {}", errores);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Uno o más campos son inválidos.");
        problem.setTitle("Error de validación");
        problem.setType(URI.create("https://api.trivia.com/errors/validation-error"));
        problem.setProperty("errores", errores);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InsufficientUniqueTriviasException.class)
    public ProblemDetail handleInsufficientUnique(InsufficientUniqueTriviasException ex) {
        log.error("Fallo por trivias únicas insuficientes: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Trivias únicas insuficientes");
        problem.setType(URI.create("https://api.trivia.com/errors/insufficient-unique"));
        problem.setProperty("solicitadas", ex.getSolicitadas());
        problem.setProperty("obtenidas", ex.getObtenidas());
        problem.setProperty("intentos", ex.getIntentos());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiClientException.class)
    public ProblemDetail handleAiClientError(AiClientException ex) {
        log.error("Error del proveedor de IA: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setTitle("Error del servicio de IA");
        problem.setType(URI.create("https://api.trivia.com/errors/ai-gateway-error"));
        if (ex.getStatusCode() != null) {
            problem.setProperty("upstreamStatusCode", ex.getStatusCode());
        }
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Error no controlado en el servidor: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno en el servidor.");
        problem.setTitle("Error interno");
        problem.setType(URI.create("https://api.trivia.com/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
