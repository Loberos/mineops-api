package com.mineops.mineopsapi.iam.domain.model.commands;

/**
 * Garantiza que el catálogo de roles tenga una fila por cada rol declarado. Es idempotente por
 * diseño, de modo que puede ejecutarse en cada arranque.
 */
public record SeedRolesCommand() {
}
