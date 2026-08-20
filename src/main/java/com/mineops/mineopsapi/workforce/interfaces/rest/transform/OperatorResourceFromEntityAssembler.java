package com.mineops.mineopsapi.workforce.interfaces.rest.transform;

import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.OperatorResource;

import java.util.Comparator;

public final class OperatorResourceFromEntityAssembler {

    private OperatorResourceFromEntityAssembler() {
    }

    public static OperatorResource toResourceFromEntity(Operator entity) {
        var certifications = entity.getCertifications().stream()
                .sorted(Comparator.comparing(certification -> certification.getEquipmentTypeCode()))
                .map(CertificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return new OperatorResource(
                entity.getId(),
                entity.getDocumentNumber(),
                entity.getName().getFirstName(),
                entity.getName().getLastName(),
                entity.getName().getFullName(),
                entity.getStatus().name(),
                entity.isAvailableForAssignment(),
                certifications);
    }
}
