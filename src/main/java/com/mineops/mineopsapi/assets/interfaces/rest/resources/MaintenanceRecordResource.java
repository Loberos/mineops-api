package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una entrada del historial de mantenimiento.
 *
 * @param overrunHours horas que la máquina operó más allá de su umbral antes de ser atendida
 * @param overdue      si ese desfase fue mayor que cero
 */
public record MaintenanceRecordResource(
        Long id,
        Long equipmentId,
        String equipmentCode,
        LocalDate performedOn,
        BigDecimal hourMeter,
        BigDecimal thresholdHours,
        BigDecimal overrunHours,
        BigDecimal nextThresholdHours,
        String responsible,
        String observations,
        boolean overdue) {
}
