package com.mineops.mineopsapi.iam.domain.services;

import com.mineops.mineopsapi.iam.domain.model.commands.SeedRolesCommand;

/**
 * Lado de escritura del catálogo de roles.
 */
public interface RoleCommandService {

    void handle(SeedRolesCommand command);
}
