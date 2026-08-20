package com.mineops.mineopsapi.operations.domain.model.commands;

/**
 * Suspende un turno que no se trabajó, liberando todas sus asignaciones.
 */
public record CancelShiftCommand(Long shiftId, String reason) {
}
