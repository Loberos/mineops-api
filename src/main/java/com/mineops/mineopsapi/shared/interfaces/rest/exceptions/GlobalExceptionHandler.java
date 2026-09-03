package com.mineops.mineopsapi.shared.interfaces.rest.exceptions;

import com.mineops.mineopsapi.shared.domain.exceptions.BusinessRuleViolationException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceConflictException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.ApiErrorResource;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.RuleViolationResource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce toda excepción que levante la aplicación al envoltorio único {@link ApiErrorResource}, de
 * modo que un cliente nunca reciba una página de error generada por el contenedor ni una traza
 * interna.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Un comando que incumplió una o más reglas de negocio. Se informan todas las violaciones
     * detectadas, que es lo que permite al cliente mostrar todas las razones por las que se rechazó
     * una asignación en lugar de solo la primera.
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResource> handleBusinessRuleViolation(
            BusinessRuleViolationException exception, HttpServletRequest request) {
        var violations = exception.getViolations().stream()
                .map(RuleViolationResource::fromViolation)
                .toList();
        LOGGER.info("Comando rechazado en {}: {}", request.getRequestURI(), violations);
        return ResponseEntity.unprocessableEntity()
                .body(ApiErrorResource.withViolations(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        exception.getMessage(),
                        request.getRequestURI(),
                        violations));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResource> handleResourceNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return status(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorResource> handleResourceConflict(
            ResourceConflictException exception, HttpServletRequest request) {
        return status(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    /**
     * Última línea de defensa de las garantías de unicidad declaradas en el esquema. Si dos
     * supervisores compiten por el mismo equipo en el mismo turno, el que pierde aterriza aquí.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResource> handleDataIntegrityViolation(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        LOGGER.warn("Una restricción de la base de datos rechazó la escritura en {}",
                request.getRequestURI(), exception);
        return status(
                HttpStatus.CONFLICT,
                "La operación choca con datos que ya existen. Actualiza la vista e inténtalo de nuevo.",
                request);
    }

    /**
     * Otra transacción modificó el agregado primero. Quien llama está trabajando sobre una copia
     * desactualizada.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResource> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        LOGGER.warn("Se perdió el bloqueo optimista en {}", request.getRequestURI(), exception);
        return status(
                HttpStatus.CONFLICT,
                "Otro usuario modificó este registro mientras trabajabas sobre él. "
                        + "Actualiza la vista e inténtalo de nuevo.",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResource> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiErrorResource.withFieldErrors(
                        HttpStatus.BAD_REQUEST,
                        "El cuerpo de la petición no es válido",
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResource> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        return status(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResource> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        return status(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResource> handleAuthentication(
            AuthenticationException exception, HttpServletRequest request) {
        LOGGER.info("Autenticación rechazada en {}: {}", request.getRequestURI(), exception.getMessage());
        return status(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResource> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return status(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación", request);
    }

    /**
     * Una ruta que no corresponde a ningún controlador. Sin este manejador cae en el de más abajo y
     * la API responde 500 a lo que solo es una dirección mal escrita: quien explora la API ve un
     * fallo del servidor donde debería ver que el recurso no existe.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResource> handleNoResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return status(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", request);
    }

    /**
     * La ruta existe pero no admite ese verbo —un GET sobre lo que solo acepta POST—. Es información
     * útil para quien integra, y no un error del servidor.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResource> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return status(HttpStatus.METHOD_NOT_ALLOWED,
                "El método %s no está permitido en esta ruta".formatted(exception.getMethod()), request);
    }

    /** Cuerpo ausente o que no es JSON válido: el error es de quien llama, no del servidor. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResource> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return status(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", request);
    }

    /** Un parámetro de ruta con el tipo equivocado, como un identificador que no es numérico. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResource> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return status(HttpStatus.BAD_REQUEST,
                "El valor de '%s' no tiene el formato esperado".formatted(exception.getName()), request);
    }

    /**
     * Cualquier cosa imprevista. La causa se registra completa en el log pero nunca se envía al
     * cliente, para no filtrar detalles internos a través de la API.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResource> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Fallo no controlado en {}", request.getRequestURI(), exception);
        return status(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error inesperado. Comunícate con soporte.", request);
    }

    private ResponseEntity<ApiErrorResource> status(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiErrorResource.of(status, message, request.getRequestURI()));
    }
}
