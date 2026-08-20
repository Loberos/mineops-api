package com.mineops.mineopsapi.workforce.interfaces.rest.transform;

import com.mineops.mineopsapi.workforce.domain.model.entities.Certification;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.CertificationResource;

import java.time.LocalDate;

public final class CertificationResourceFromEntityAssembler {

    private CertificationResourceFromEntityAssembler() {
    }

    public static CertificationResource toResourceFromEntity(Certification entity) {
        var today = LocalDate.now();
        var validity = entity.getValidity();
        return new CertificationResource(
                entity.getId(),
                entity.getEquipmentTypeId(),
                entity.getEquipmentTypeCode(),
                entity.getEquipmentTypeName(),
                validity.getIssuedOn(),
                validity.getExpiresOn(),
                validity.isValidOn(today),
                validity.daysUntilExpiryFrom(today));
    }
}
