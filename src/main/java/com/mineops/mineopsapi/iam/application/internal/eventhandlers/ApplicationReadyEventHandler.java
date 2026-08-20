package com.mineops.mineopsapi.iam.application.internal.eventhandlers;

import com.mineops.mineopsapi.iam.domain.model.commands.SeedRolesCommand;
import com.mineops.mineopsapi.iam.domain.services.RoleCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Garantiza que el catálogo de roles esté completo antes que cualquier otra cosa. Se ordena por
 * delante del cargador de datos de ejemplo, que otorga esos roles a sus usuarios.
 */
@Service
public class ApplicationReadyEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReadyEventHandler.class);

    private final RoleCommandService roleCommandService;

    public ApplicationReadyEventHandler(RoleCommandService roleCommandService) {
        this.roleCommandService = roleCommandService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void onApplicationReady(ApplicationReadyEvent event) {
        LOGGER.info("Verificando el catálogo de roles");
        roleCommandService.handle(new SeedRolesCommand());
    }
}
