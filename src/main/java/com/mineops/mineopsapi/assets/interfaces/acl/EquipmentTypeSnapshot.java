package com.mineops.mineopsapi.assets.interfaces.acl;

import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;

import java.math.BigDecimal;

/**
 * Vista de solo lectura de una familia de máquinas, publicada para los contextos que necesitan
 * nombrar una sin ser sus dueños; el contexto de personal, por ejemplo, certifica operadores por
 * familia.
 *
 * @param id                       identificador de la familia
 * @param code                     código corto
 * @param name                     nombre visible
 * @param maintenanceIntervalHours horas que otorga un ciclo de mantenimiento
 */
public record EquipmentTypeSnapshot(Long id, String code, String name, BigDecimal maintenanceIntervalHours) {

    public static EquipmentTypeSnapshot fromAggregate(EquipmentType equipmentType) {
        return new EquipmentTypeSnapshot(
                equipmentType.getId(),
                equipmentType.getCode(),
                equipmentType.getName(),
                equipmentType.getMaintenanceIntervalHours());
    }
}
