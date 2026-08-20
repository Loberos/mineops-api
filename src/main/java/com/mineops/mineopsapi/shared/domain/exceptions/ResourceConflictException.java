package com.mineops.mineopsapi.shared.domain.exceptions;

/**
 * Se lanza cuando un comando choca con el estado actual del sistema: una clave natural duplicada, o
 * una escritura concurrente que ya se apropió del recurso.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
