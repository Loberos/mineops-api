package com.mineops.mineopsapi.assets.interfaces.acl;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;

import java.math.BigDecimal;

/**
 * Vista de solo lectura de una máquina, publicada por el contexto de activos para el resto del
 * sistema.
 * <p>
 * Los demás contextos reciben esta copia inmutable en lugar del agregado {@code Equipment}, de modo
 * que pueden evaluar sus propias reglas sin poder mutar una máquina a espaldas de su dueño.
 * </p>
 *
 * @param id                        identificador de la máquina
 * @param code                      código pintado en la máquina
 * @param equipmentTypeId           familia a la que pertenece
 * @param equipmentTypeCode         código corto de la familia
 * @param equipmentTypeName         nombre visible de la familia
 * @param status                    estado operativo actual
 * @param hourMeter                 horas de uso acumuladas
 * @param maintenanceThresholdHours lectura a la que debe detenerse
 * @param maintenanceIntervalHours  horas que otorga un ciclo de mantenimiento
 */
public record EquipmentSnapshot(
        Long id,
        String code,
        Long equipmentTypeId,
        String equipmentTypeCode,
        String equipmentTypeName,
        EquipmentStatus status,
        BigDecimal hourMeter,
        BigDecimal maintenanceThresholdHours,
        BigDecimal maintenanceIntervalHours) {

    public static EquipmentSnapshot fromAggregate(Equipment equipment) {
        var type = equipment.getEquipmentType();
        return new EquipmentSnapshot(
                equipment.getId(),
                equipment.getCode(),
                type.getId(),
                type.getCode(),
                type.getName(),
                equipment.getStatus(),
                equipment.getHourMeter(),
                equipment.getMaintenanceThresholdHours(),
                type.getMaintenanceIntervalHours());
    }

    public boolean isAvailableForAssignment() {
        return status.allowsAssignment();
    }

    /**
     * Horas de uso que quedan antes de que venza el mantenimiento. Negativo cuando ya se excedió.
     */
    public BigDecimal hoursUntilMaintenance() {
        return maintenanceThresholdHours.subtract(hourMeter);
    }

    /**
     * Indica si trabajar las horas indicadas llevaría a esta máquina más allá de su umbral. Sirve
     * para mirar hacia adelante sin tocar el agregado.
     */
    public boolean wouldReachThresholdAfter(BigDecimal additionalHours) {
        return hourMeter.add(additionalHours).compareTo(maintenanceThresholdHours) >= 0;
    }
}
