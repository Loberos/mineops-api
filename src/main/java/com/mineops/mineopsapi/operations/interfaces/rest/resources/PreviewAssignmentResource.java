package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record PreviewAssignmentResource(
        @NotNull(message = "El operador es obligatorio") Long operatorId,
        @NotNull(message = "El equipo es obligatorio") Long equipmentId) {
}
