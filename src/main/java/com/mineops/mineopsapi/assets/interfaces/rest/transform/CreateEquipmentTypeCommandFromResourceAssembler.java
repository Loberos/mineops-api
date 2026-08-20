package com.mineops.mineopsapi.assets.interfaces.rest.transform;

import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentTypeCommand;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.CreateEquipmentTypeResource;

public final class CreateEquipmentTypeCommandFromResourceAssembler {

    private CreateEquipmentTypeCommandFromResourceAssembler() {
    }

    public static CreateEquipmentTypeCommand toCommandFromResource(CreateEquipmentTypeResource resource) {
        return new CreateEquipmentTypeCommand(
                resource.code(), resource.name(), resource.maintenanceIntervalHours(), resource.description());
    }
}
