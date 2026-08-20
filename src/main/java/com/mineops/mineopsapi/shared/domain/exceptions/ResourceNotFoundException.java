package com.mineops.mineopsapi.shared.domain.exceptions;

/**
 * Se lanza cuando un comando o una consulta referencian una entidad que no existe.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object identifier) {
        super("%s %s no fue encontrado".formatted(resource, identifier));
    }
}
