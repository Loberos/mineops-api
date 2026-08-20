package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import java.math.BigDecimal;

/**
 * @param riskReason    por qué la asignación está marcada; null mientras nada la amenace
 * @param authorization presente solo cuando un supervisor la autorizó pese a las reglas incumplidas
 */
public record AssignmentResource(
        Long id,
        Long shiftId,
        Long operatorId,
        String operatorName,
        String operatorDocument,
        Long equipmentId,
        String equipmentCode,
        Long equipmentTypeId,
        String equipmentTypeName,
        String status,
        BigDecimal workedHours,
        String closureNote,
        String riskReason,
        boolean forced,
        SupervisorAuthorizationResource authorization) {
}
