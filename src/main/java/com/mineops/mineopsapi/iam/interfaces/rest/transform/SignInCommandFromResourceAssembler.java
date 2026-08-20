package com.mineops.mineopsapi.iam.interfaces.rest.transform;

import com.mineops.mineopsapi.iam.domain.model.commands.SignInCommand;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.SignInResource;

public final class SignInCommandFromResourceAssembler {

    private SignInCommandFromResourceAssembler() {
    }

    public static SignInCommand toCommandFromResource(SignInResource resource) {
        return new SignInCommand(resource.email(), resource.password());
    }
}
