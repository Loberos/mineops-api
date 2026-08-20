package com.mineops.mineopsapi.operations.interfaces.rest.transform;

import com.mineops.mineopsapi.operations.domain.model.commands.CreateShiftCommand;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.CreateShiftResource;

public final class CreateShiftCommandFromResourceAssembler {

    private CreateShiftCommandFromResourceAssembler() {
    }

    public static CreateShiftCommand toCommandFromResource(CreateShiftResource resource) {
        return new CreateShiftCommand(
                resource.date(), resource.journey(), resource.plannedHours(), resource.notes());
    }
}
