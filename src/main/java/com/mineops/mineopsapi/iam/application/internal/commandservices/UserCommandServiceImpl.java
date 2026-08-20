package com.mineops.mineopsapi.iam.application.internal.commandservices;

import com.mineops.mineopsapi.iam.application.internal.outboundservices.hashing.HashingService;
import com.mineops.mineopsapi.iam.application.internal.outboundservices.tokens.TokenService;
import com.mineops.mineopsapi.iam.domain.model.aggregates.User;
import com.mineops.mineopsapi.iam.domain.model.commands.SignInCommand;
import com.mineops.mineopsapi.iam.domain.model.commands.SignUpCommand;
import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.domain.model.valueobjects.AuthenticatedUser;
import com.mineops.mineopsapi.iam.domain.services.UserCommandService;
import com.mineops.mineopsapi.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.mineops.mineopsapi.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCommandServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            HashingService hashingService,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public Optional<User> handle(SignUpCommand command) {
        var email = normalizeEmail(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResourceConflictException("Ya existe un usuario con el correo %s".formatted(email));
        }
        var roles = resolvePersistedRoles(command.roles());
        var user = new User(email, hashingService.encode(command.password()), command.fullName().trim(), roles);
        LOGGER.info("Registrando al usuario {}", email);
        return Optional.of(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> handle(SignInCommand command) {
        var email = normalizeEmail(command.email());
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!hashingService.matches(command.password(), user.getPassword())) {
            LOGGER.info("Intento de acceso fallido para {}", email);
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!user.isActive()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return Optional.of(new AuthenticatedUser(user, tokenService.generateToken(user.getEmail())));
    }

    /**
     * Reemplaza los roles desligados que trae el comando por las filas gestionadas del catálogo, de
     * modo que otorgar un rol nunca cree una entrada duplicada.
     */
    private List<Role> resolvePersistedRoles(List<Role> requestedRoles) {
        return Role.validateRoleSet(requestedRoles).stream()
                .map(role -> roleRepository.findByName(role.getName())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El rol %s no forma parte del catálogo".formatted(role.getStringName()))))
                .toList();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
