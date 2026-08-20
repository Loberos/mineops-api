package com.mineops.mineopsapi.assets.interfaces.rest.transform;

import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.EquipmentTypeResource;

public final class EquipmentTypeResourceFromEntityAssembler {

    private EquipmentTypeResourceFromEntityAssembler() {
    }

    public static EquipmentTypeResource toResourceFromEntity(EquipmentType entity) {
        return new EquipmentTypeResource(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getMaintenanceIntervalHours(),
                entity.getDescription(),
                entity.isActive());
    }
}
