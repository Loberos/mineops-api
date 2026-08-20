package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import java.time.LocalDate;

/**
 * Una asignación esperando una decisión humana, con lo suficiente de su turno adjunto como para
 * poder actuar sin hacer una segunda petición.
 */
public record AssignmentAtRiskResource(
        Long assignmentId,
        Long shiftId,
        LocalDate shiftDate,
        String shiftJourney,
        String operatorName,
        String equipmentCode,
        String equipmentTypeName,
        String riskReason) {
}
