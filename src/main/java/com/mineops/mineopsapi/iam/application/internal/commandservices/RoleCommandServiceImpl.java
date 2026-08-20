package com.mineops.mineopsapi.iam.application.internal.commandservices;

import com.mineops.mineopsapi.iam.domain.model.commands.SeedRolesCommand;
import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.domain.model.valueobjects.Roles;
import com.mineops.mineopsapi.iam.domain.services.RoleCommandService;
import com.mineops.mineopsapi.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
public class RoleCommandServiceImpl implements RoleCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCommandServiceImpl.class);

    private final RoleRepository roleRepository;

    public RoleCommandServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void handle(SeedRolesCommand command) {
        Arrays.stream(Roles.values())
                .filter(role -> !roleRepository.existsByName(role))
                .forEach(role -> {
                    LOGGER.info("Agregando el rol {} que faltaba en el catálogo", role);
                    roleRepository.save(new Role(role));
                });
    }
}
