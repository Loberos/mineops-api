package com.mineops.mineopsapi.shared.interfaces.rest.resources;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Envoltorio único de error que devuelve cada endpoint de la API.
 * <p>
 * Los miembros nulos se omiten del payload, de modo que un fallo simple lleva solo los campos
 * comunes mientras que un comando rechazado lleva además la lista de reglas que incumplió.
 * </p>
 *
 * @param timestamp   momento en que se manejó el fallo
 * @param status      código de estado HTTP
 * @param error       frase descriptiva del estado HTTP
 * @param message     explicación del fallo
 * @param path        ruta de la petición que produjo el fallo
 * @param violations  reglas de negocio incumplidas, cuando el fallo vino del dominio
 * @param fieldErrors mensajes por campo, cuando el fallo vino de la validación de la petición
 */
public record ApiErrorResource(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<RuleViolationResource> violations,
        Map<String, String> fieldErrors) {

    public static ApiErrorResource of(HttpStatus status, String message, String path) {
        return new ApiErrorResource(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, null, null);
    }

    public static ApiErrorResource withViolations(
            HttpStatus status, String message, String path, List<RuleViolationResource> violations) {
        return new ApiErrorResource(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, violations, null);
    }

    public static ApiErrorResource withFieldErrors(
            HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiErrorResource(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, null, fieldErrors);
    }
}
