package tools.vitruv.framework.remote.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * GlobalExceptionHandler is a centralized exception handling component for a Spring Boot application.
 * It provides handling for specific and generic exceptions that arise in the application.
 * By using this class, uniform responses can be returned for different types of exceptions.
 * <p>
 * The class is annotated with {@code @RestControllerAdvice}, which allows it to globally intercept
 * exceptions thrown by methods annotated with {@code @RequestMapping}.
 * <p>
 * Exception-Handling Methods:
 * - {@code handleResponseStatusException}: Handles {@code ResponseStatusException} occurrences,
 * returning a response built using the exception's status code and reason.
 * - {@code handleGenericException}: Handles all other generic exceptions, returning a response
 * with a status of {@code BAD_REQUEST} and the exception's message as the body.
 * <p>
 * This class ensures that the application returns consistent and informative error responses
 * to the client, improving the overall API design.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }
}
