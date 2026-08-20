package com.mineops.mineopsapi.operations.interfaces.rest.transform;

import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.SupervisorAuthorization;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.AssignmentResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.SupervisorAuthorizationResource;

public final class AssignmentResourceFromEntityAssembler {

    private AssignmentResourceFromEntityAssembler() {
    }

    public static AssignmentResource toResourceFromEntity(Assignment entity) {
        return new AssignmentResource(
                entity.getId(),
                entity.getShift().getId(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getOperatorDocument(),
                entity.getEquipmentId(),
                entity.getEquipmentCode(),
                entity.getEquipmentTypeId(),
                entity.getEquipmentTypeName(),
                entity.getStatus().name(),
                entity.getWorkedHours(),
                entity.getClosureNote(),
                entity.getRiskReason(),
                entity.isForced(),
                toAuthorizationResource(entity.getAuthorization()));
    }

    private static SupervisorAuthorizationResource toAuthorizationResource(SupervisorAuthorization authorization) {
        if (authorization == null || authorization.getAuthorizedByUserId() == null) {
            return null;
        }
        return new SupervisorAuthorizationResource(
                authorization.getAuthorizedByUserId(),
                authorization.getAuthorizedByName(),
                authorization.getReason(),
                authorization.getAuthorizedAt(),
                authorization.getOverriddenRuleCodeList());
    }
}
