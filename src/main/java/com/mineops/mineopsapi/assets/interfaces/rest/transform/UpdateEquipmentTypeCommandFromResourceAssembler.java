package com.mineops.mineopsapi.assets.interfaces.rest.transform;

import com.mineops.mineopsapi.assets.domain.model.commands.UpdateEquipmentTypeCommand;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.UpdateEquipmentTypeResource;

public final class UpdateEquipmentTypeCommandFromResourceAssembler {

    private UpdateEquipmentTypeCommandFromResourceAssembler() {
    }

    public static UpdateEquipmentTypeCommand toCommandFromResource(
            Long equipmentTypeId, UpdateEquipmentTypeResource resource) {
        return new UpdateEquipmentTypeCommand(
                equipmentTypeId, resource.name(), resource.maintenanceIntervalHours(), resource.description());
    }
}
