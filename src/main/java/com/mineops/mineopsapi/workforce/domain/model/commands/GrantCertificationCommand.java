package com.mineops.mineopsapi.workforce.domain.model.commands;

import java.time.LocalDate;

/**
 * Certifica a un operador para conducir una familia de máquinas, o renueva la certificación que ya
 * tiene.
 *
 * @param operatorId      el operador que se certifica
 * @param equipmentTypeId la familia de máquinas
 * @param issuedOn        primer día en que rige la certificación
 * @param expiresOn       último día en que rige la certificación
 */
public record GrantCertificationCommand(
        Long operatorId, Long equipmentTypeId, LocalDate issuedOn, LocalDate expiresOn) {
}
