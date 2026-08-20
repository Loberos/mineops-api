package com.mineops.mineopsapi.iam.interfaces.rest.transform;

import com.mineops.mineopsapi.iam.domain.model.aggregates.User;
import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.UserResource;

public final class UserResourceFromEntityAssembler {

    private UserResourceFromEntityAssembler() {
    }

    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isActive(),
                user.getRoles().stream().map(Role::getStringName).sorted().toList());
    }
}
