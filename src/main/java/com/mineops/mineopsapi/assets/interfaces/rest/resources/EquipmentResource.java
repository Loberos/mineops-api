package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representación de transporte de una máquina.
 *
 * @param hoursUntilMaintenance  horas que quedan antes del umbral; negativo cuando ya se excedió
 * @param availableForAssignment bandera de conveniencia para que el cliente no reimplemente la regla
 */
public record EquipmentResource(
        Long id,
        String code,
        Long equipmentTypeId,
        String equipmentTypeCode,
        String equipmentTypeName,
        String status,
        BigDecimal hourMeter,
        BigDecimal maintenanceThresholdHours,
        BigDecimal maintenanceIntervalHours,
        BigDecimal hoursUntilMaintenance,
        BigDecimal lastMaintenanceHourMeter,
        LocalDate lastMaintenanceDate,
        boolean availableForAssignment) {
}
