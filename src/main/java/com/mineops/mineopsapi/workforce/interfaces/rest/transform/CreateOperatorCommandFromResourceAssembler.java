package com.mineops.mineopsapi.workforce.interfaces.rest.transform;

import com.mineops.mineopsapi.workforce.domain.model.commands.CreateOperatorCommand;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.CreateOperatorResource;

public final class CreateOperatorCommandFromResourceAssembler {

    private CreateOperatorCommandFromResourceAssembler() {
    }

    public static CreateOperatorCommand toCommandFromResource(CreateOperatorResource resource) {
        return new CreateOperatorCommand(resource.documentNumber(), resource.firstName(), resource.lastName());
    }
}
