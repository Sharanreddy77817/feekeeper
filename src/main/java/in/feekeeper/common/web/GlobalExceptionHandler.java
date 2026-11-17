package in.feekeeper.common.web;

import in.feekeeper.auth.InvalidCredentialsException;
import in.feekeeper.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiProblem> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiProblem.FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiProblem.FieldViolation(e.getField(), e.getDefaultMessage())).toList();
        return problem(400, "VALIDATION_FAILED", "Request validation failed", request, fields);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiProblem> notFound(NotFoundException ex, HttpServletRequest request) {
        return problem(404, "NOT_FOUND", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiProblem> noResource(NoResourceFoundException ex, HttpServletRequest request) {
        return problem(404, "NOT_FOUND", "Resource not found", request, List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiProblem> badRequest(BadRequestException ex, HttpServletRequest request) {
        return problem(400, ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({InvalidCredentialsException.class})
    ResponseEntity<ApiProblem> invalidCredentials(RuntimeException ex, HttpServletRequest request) {
        return problem(401, "INVALID_CREDENTIALS", "Invalid email or password", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiProblem> denied(AccessDeniedException ex, HttpServletRequest request) {
        return problem(403, "ACCESS_DENIED", "Access is denied", request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiProblem> conflict(ConflictException ex, HttpServletRequest request) {
        return problem(409, ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiProblem> integrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return problem(409, "DATA_CONFLICT", "The request conflicts with existing data", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected request failure", ex);
        return problem(500, "INTERNAL_ERROR", "An unexpected error occurred", request, List.of());
    }

    private ResponseEntity<ApiProblem> problem(int status, String code, String message,
                                               HttpServletRequest request, List<ApiProblem.FieldViolation> fields) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ApiProblem(Instant.now(), status, code, message, request.getRequestURI(), fields));
    }
}
