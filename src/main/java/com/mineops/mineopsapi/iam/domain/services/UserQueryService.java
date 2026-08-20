package com.mineops.mineopsapi.iam.domain.services;

import com.mineops.mineopsapi.iam.domain.model.aggregates.User;
import com.mineops.mineopsapi.iam.domain.model.queries.GetAllUsersQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByEmailQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByIdQuery;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;

import java.util.List;
import java.util.Optional;

/**
 * Lado de lectura del agregado de usuario.
 */
public interface UserQueryService {

    /** Resuelve la consulta acotada al tramo pedido. Es la que atiende al listado de la API. */
    PagedResult<User> handle(GetAllUsersQuery query, PageCriteria criteria);

    /** Resuelve la consulta completa, sin trocear, para los usos que no la exponen por HTTP. */
    List<User> handle(GetAllUsersQuery query);

    Optional<User> handle(GetUserByIdQuery query);

    Optional<User> handle(GetUserByEmailQuery query);
}
