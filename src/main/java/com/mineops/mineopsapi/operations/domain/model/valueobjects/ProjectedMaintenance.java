package com.mineops.mineopsapi.operations.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que la proyección concluyó sobre una máquina.
 *
 * @param equipmentId              identificador de la máquina
 * @param equipmentCode            código de la máquina
 * @param equipmentTypeName        familia a la que pertenece
 * @param currentHourMeter         horómetro tal como marca hoy
 * @param thresholdHours           lectura a la que vence el mantenimiento
 * @param hoursUntilMaintenance    horas de uso que quedan hoy; negativo si ya se excedió
 * @param scheduledHoursInHorizon  horas que la programación tiene a esta máquina trabajando en el horizonte
 * @param projectedHourMeter       lectura que marcaría al final del horizonte
 * @param alreadyBlocked           si está detenida en este momento
 * @param willReachThreshold       si la programación la lleva al umbral o más allá
 * @param crossingDate             día en que se cruzaría el umbral; null si no ocurre
 * @param crossingJourney          jornada del turno que lo cruza; null si no ocurre
 * @param crossingShiftId          identificador de ese turno; null si no ocurre
 * @param hourMeterAtCrossing      lectura justo después de ese turno; null si no ocurre
 * @param scheduledShiftsInHorizon cuántos turnos tiene programados la máquina dentro del horizonte
 */
public record ProjectedMaintenance(
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
        Journey crossingJourney,
        Long crossingShiftId,
        BigDecimal hourMeterAtCrossing,
        int scheduledShiftsInHorizon) {
}
