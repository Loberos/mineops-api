package com.mineops.mineopsapi.assets.interfaces.rest.transform;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.EquipmentResource;

public final class EquipmentResourceFromEntityAssembler {

    private EquipmentResourceFromEntityAssembler() {
    }

    public static EquipmentResource toResourceFromEntity(Equipment entity) {
        var type = entity.getEquipmentType();
        return new EquipmentResource(
                entity.getId(),
                entity.getCode(),
                type.getId(),
                type.getCode(),
                type.getName(),
                entity.getStatus().name(),
                entity.getHourMeter(),
                entity.getMaintenanceThresholdHours(),
                type.getMaintenanceIntervalHours(),
                entity.hoursUntilMaintenance(),
                entity.getLastMaintenanceHourMeter(),
                entity.getLastMaintenanceDate(),
                entity.isAvailableForAssignment());
    }
}
