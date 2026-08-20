package com.mineops.mineopsapi.assets.interfaces.rest.transform;

import com.mineops.mineopsapi.assets.domain.model.commands.RegisterMaintenanceCommand;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.RegisterMaintenanceResource;

public final class RegisterMaintenanceCommandFromResourceAssembler {

    private RegisterMaintenanceCommandFromResourceAssembler() {
    }

    public static RegisterMaintenanceCommand toCommandFromResource(
            Long equipmentId, RegisterMaintenanceResource resource) {
        return new RegisterMaintenanceCommand(
                equipmentId,
                resource.performedOn(),
                resource.hourMeter(),
                resource.responsible(),
                resource.observations());
    }
}
