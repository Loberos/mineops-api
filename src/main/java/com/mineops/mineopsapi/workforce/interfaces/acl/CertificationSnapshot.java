package com.mineops.mineopsapi.workforce.interfaces.acl;

import com.mineops.mineopsapi.workforce.domain.model.entities.Certification;

import java.time.LocalDate;

/**
 * Vista de solo lectura de una certificación, publicada para los contextos que necesitan
 * verificarla.
 *
 * @param equipmentTypeId   familia de máquinas que cubre la certificación
 * @param equipmentTypeCode código corto de esa familia
 * @param equipmentTypeName nombre visible de esa familia
 * @param issuedOn          primer día en que rige la certificación
 * @param expiresOn         último día en que rige la certificación
 */
public record CertificationSnapshot(
        Long equipmentTypeId,
        String equipmentTypeCode,
        String equipmentTypeName,
        LocalDate issuedOn,
        LocalDate expiresOn) {

    public static CertificationSnapshot fromEntity(Certification certification) {
        return new CertificationSnapshot(
                certification.getEquipmentTypeId(),
                certification.getEquipmentTypeCode(),
                certification.getEquipmentTypeName(),
                certification.getValidity().getIssuedOn(),
                certification.getValidity().getExpiresOn());
    }

    public boolean isValidOn(LocalDate date) {
        return !date.isBefore(issuedOn) && !date.isAfter(expiresOn);
    }

    public boolean coversRange(LocalDate from, LocalDate to) {
        return isValidOn(from) && isValidOn(to);
    }
}
