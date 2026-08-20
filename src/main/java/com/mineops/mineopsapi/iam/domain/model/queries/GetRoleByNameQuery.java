package com.mineops.mineopsapi.iam.domain.model.queries;

import com.mineops.mineopsapi.iam.domain.model.valueobjects.Roles;

public record GetRoleByNameQuery(Roles name) {
}
