package com.mineops.mineopsapi.workforce.interfaces.rest.transform;

import com.mineops.mineopsapi.workforce.domain.model.entities.Certification;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.ExpiringCertificationResource;

import java.time.LocalDate;

public final class ExpiringCertificationResourceFromEntityAssembler {

    private ExpiringCertificationResourceFromEntityAssembler() {
    }

    public static ExpiringCertificationResource toResourceFromEntity(Certification entity) {
        var today = LocalDate.now();
        var validity = entity.getValidity();
        var operator = entity.getOperator();
        return new ExpiringCertificationResource(
                operator.getId(),
                operator.getDocumentNumber(),
                operator.getName().getFullName(),
                entity.getEquipmentTypeId(),
                entity.getEquipmentTypeCode(),
                entity.getEquipmentTypeName(),
                validity.getExpiresOn(),
                validity.daysUntilExpiryFrom(today),
                validity.isExpiredOn(today));
    }
}
