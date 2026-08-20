package com.mineops.mineopsapi.iam.interfaces.rest.transform;

import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.domain.model.valueobjects.AuthenticatedUser;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.AuthenticatedUserResource;

public final class AuthenticatedUserResourceFromEntityAssembler {

    private AuthenticatedUserResourceFromEntityAssembler() {
    }

    public static AuthenticatedUserResource toResourceFromEntity(AuthenticatedUser authenticatedUser) {
        var user = authenticatedUser.user();
        return new AuthenticatedUserResource(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(Role::getStringName).sorted().toList(),
                authenticatedUser.token());
    }
}
