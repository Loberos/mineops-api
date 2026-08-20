package com.mineops.mineopsapi.operations.domain.model.commands;

/**
 * Suspende una asignación puntual, liberando a su operador y a su máquina para un reemplazo.
 */
public record CancelAssignmentCommand(Long shiftId, Long assignmentId, String reason) {
}
