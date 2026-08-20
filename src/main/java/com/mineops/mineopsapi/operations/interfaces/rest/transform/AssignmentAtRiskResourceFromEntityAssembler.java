package com.mineops.mineopsapi.operations.interfaces.rest.transform;

import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.AssignmentAtRiskResource;

public final class AssignmentAtRiskResourceFromEntityAssembler {

    private AssignmentAtRiskResourceFromEntityAssembler() {
    }

    public static AssignmentAtRiskResource toResourceFromEntity(Assignment entity) {
        var shift = entity.getShift();
        return new AssignmentAtRiskResource(
                entity.getId(),
                shift.getId(),
                shift.getDate(),
                shift.getJourney().name(),
                entity.getOperatorName(),
                entity.getEquipmentCode(),
                entity.getEquipmentTypeName(),
                entity.getRiskReason());
    }
}
