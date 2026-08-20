package com.mineops.mineopsapi.assets.interfaces.rest.transform;

import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentCommand;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.CreateEquipmentResource;

import java.math.BigDecimal;

public final class CreateEquipmentCommandFromResourceAssembler {

    private CreateEquipmentCommandFromResourceAssembler() {
    }

    public static CreateEquipmentCommand toCommandFromResource(CreateEquipmentResource resource) {
        var initialHourMeter = resource.initialHourMeter() == null ? BigDecimal.ZERO : resource.initialHourMeter();
        return new CreateEquipmentCommand(resource.code(), resource.equipmentTypeId(), initialHourMeter);
    }
}
