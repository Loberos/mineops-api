package com.mineops.mineopsapi.iam.interfaces.rest.transform;

import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.RoleResource;

public final class RoleResourceFromEntityAssembler {

    private RoleResourceFromEntityAssembler() {
    }

    public static RoleResource toResourceFromEntity(Role role) {
        return new RoleResource(role.getId(), role.getStringName());
    }
}
