package com.mineops.mineopsapi.iam.domain.model.commands;

import com.mineops.mineopsapi.iam.domain.model.entities.Role;

import java.util.List;

/**
 * Registra un nuevo usuario de la plataforma.
 *
 * @param email    identificador único de acceso
 * @param password contraseña en claro, que se hashea antes de almacenarse
 * @param fullName nombre visible que aparece en las trazas de auditoría
 * @param roles    roles a otorgar; se otorga el rol por defecto cuando viene vacío
 */
public record SignUpCommand(String email, String password, String fullName, List<Role> roles) {
}
