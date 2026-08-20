package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una fila de la proyección de mantenimiento.
 *
 * @param scheduledHoursInHorizon horas que la programación tiene a esta máquina trabajando en el horizonte
 * @param projectedHourMeter      lectura que marcaría al final del horizonte
 * @param crossingDate            día en que se cruzaría el umbral; null si no ocurre
 * @param daysUntilCrossing       días desde hoy hasta ese cruce; null si no ocurre
 */
public record ProjectedMaintenanceResource(
        Long equipmentId,
        String equipmentCode,
        String equipmentTypeName,
        BigDecimal currentHourMeter,
        BigDecimal thresholdHours,
        BigDecimal hoursUntilMaintenance,
        BigDecimal scheduledHoursInHorizon,
        BigDecimal projectedHourMeter,
        boolean alreadyBlocked,
        boolean willReachThreshold,
        LocalDate crossingDate,
        String crossingJourney,
        Long crossingShiftId,
        BigDecimal hourMeterAtCrossing,
        int scheduledShiftsInHorizon,
        Long daysUntilCrossing) {
}
