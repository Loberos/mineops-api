package com.mineops.mineopsapi.iam.domain.services;

import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.domain.model.queries.GetAllRolesQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetRoleByNameQuery;

import java.util.List;
import java.util.Optional;

/**
 * Lado de lectura del catálogo de roles.
 */
public interface RoleQueryService {

    List<Role> handle(GetAllRolesQuery query);

    Optional<Role> handle(GetRoleByNameQuery query);
}
