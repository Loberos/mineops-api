package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Suspender algo siempre exige decir por qué, para que la constancia se explique sola más adelante.
 */
public record CancelReasonResource(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500)
        String reason) {
}
