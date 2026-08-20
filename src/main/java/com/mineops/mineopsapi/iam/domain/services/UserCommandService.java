package com.mineops.mineopsapi.iam.domain.services;

import com.mineops.mineopsapi.iam.domain.model.aggregates.User;
import com.mineops.mineopsapi.iam.domain.model.commands.SignInCommand;
import com.mineops.mineopsapi.iam.domain.model.commands.SignUpCommand;
import com.mineops.mineopsapi.iam.domain.model.valueobjects.AuthenticatedUser;

import java.util.Optional;

/**
 * Lado de escritura del agregado de usuario.
 */
public interface UserCommandService {

    Optional<User> handle(SignUpCommand command);

    Optional<AuthenticatedUser> handle(SignInCommand command);
}
