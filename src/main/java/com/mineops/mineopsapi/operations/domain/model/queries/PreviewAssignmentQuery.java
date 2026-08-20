package com.mineops.mineopsapi.operations.domain.model.queries;

/**
 * Pregunta qué pasaría si se hiciera una asignación, sin hacerla.
 * <p>
 * Existe para que al planificador se le puedan mostrar todas las reglas que una combinación
 * incumpliría mientras todavía está eligiendo, en vez de después de un envío rechazado.
 * </p>
 */
public record PreviewAssignmentQuery(Long shiftId, Long operatorId, Long equipmentId) {
}
