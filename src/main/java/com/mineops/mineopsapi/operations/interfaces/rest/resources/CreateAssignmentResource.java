package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param force               se activa cuando quien llama es un supervisor autorizando una asignación
 *                            que incumple reglas; sin él, una regla incumplida rechaza la petición
 * @param authorizationReason obligatorio cuando {@code force} está activo
 */
public record CreateAssignmentResource(
        @NotNull(message = "El operador es obligatorio")
        Long operatorId,

        @NotNull(message = "El equipo es obligatorio")
        Long equipmentId,

        boolean force,

        @Size(max = 500)
        String authorizationReason) {
}
