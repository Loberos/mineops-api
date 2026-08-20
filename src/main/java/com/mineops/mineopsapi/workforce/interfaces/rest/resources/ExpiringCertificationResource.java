package com.mineops.mineopsapi.workforce.interfaces.rest.resources;

import java.time.LocalDate;

/**
 * Certificación próxima a vencer, o ya vencida, junto con quién la posee.
 *
 * @param expired si la ventana ya se cerró
 */
public record ExpiringCertificationResource(
        Long operatorId,
        String documentNumber,
        String operatorName,
        Long equipmentTypeId,
        String equipmentTypeCode,
        String equipmentTypeName,
        LocalDate expiresOn,
        long daysUntilExpiry,
        boolean expired) {
}
