package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Traza de auditoría de una asignación que un supervisor autorizó pese a las reglas incumplidas.
 */
public record SupervisorAuthorizationResource(
        Long authorizedByUserId,
        String authorizedByName,
        String reason,
        LocalDateTime authorizedAt,
        List<String> overriddenRuleCodes) {
}
