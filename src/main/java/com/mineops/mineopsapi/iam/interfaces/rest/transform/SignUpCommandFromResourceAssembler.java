package com.mineops.mineopsapi.iam.interfaces.rest.transform;

import com.mineops.mineopsapi.iam.domain.model.commands.SignUpCommand;
import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.SignUpResource;

import java.util.List;

public final class SignUpCommandFromResourceAssembler {

    private SignUpCommandFromResourceAssembler() {
    }

    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        var roles = resource.roles() == null
                ? List.<Role>of()
                : resource.roles().stream().map(Role::toRoleFromName).toList();
        return new SignUpCommand(resource.email(), resource.password(), resource.fullName(), roles);
    }
}
