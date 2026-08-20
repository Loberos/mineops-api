package com.mineops.mineopsapi.workforce.interfaces.rest.resources;

import java.time.LocalDate;

/**
 * @param validToday      si la certificación rige hoy
 * @param daysUntilExpiry días que faltan para que venza; negativo una vez vencida
 */
public record CertificationResource(
        Long id,
        Long equipmentTypeId,
        String equipmentTypeCode,
        String equipmentTypeName,
        LocalDate issuedOn,
        LocalDate expiresOn,
        boolean validToday,
        long daysUntilExpiry) {
}
