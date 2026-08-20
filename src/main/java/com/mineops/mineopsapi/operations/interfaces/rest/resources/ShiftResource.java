package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param startsAt          cuándo empieza el turno, derivado de su jornada
 * @param endsAt            cuándo termina, que puede caer al día siguiente
 * @param assignmentsAtRisk cuántas de sus asignaciones necesitan una decisión
 */
public record ShiftResource(
        Long id,
        LocalDate date,
        String journey,
        BigDecimal plannedHours,
        String status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDate endDate,
        LocalDateTime closedAt,
        String notes,
        int assignmentCount,
        int assignmentsAtRisk,
        List<AssignmentResource> assignments) {
}
